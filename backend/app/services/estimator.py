"""How many minutes each person takes: the number every wait in the app is built from
(MODELS.md "The estimator"):

    wait = minutes per person × people ahead

Minutes per person comes from one of two places (MODELS.md "QueueStats.estimate_source"):
- the rolling average of the last 5 service times today (BACKEND.md step 6.1), while a queue has
  fewer than 10 of them today, or when there's no trained model;
- the learning model (ml/, BACKEND.md phase 9) after that: a linear model that predicts minutes
  per person from how the queue looks right now, and learns from every person served. Its
  prediction is kept between half and double the rolling average (ml/wait_model.within_band).

A service time is the gap between one call and the next: calling Maria at 10:00 and José at
10:04 means Maria took 4 minutes. Before anyone has been served the default 5 minutes is used.
"""

import logging
import threading
from collections import defaultdict, deque
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from pathlib import Path

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.enums import EstimateSource, TicketStatus
from app.models import Queue, Ticket
from app import timeutil
from app.timeutil import MANILA
from ml.features import Snapshot, to_row
from ml.wait_model import WaitModel, within_band

log = logging.getLogger(__name__)

# Before anyone has been served (MODELS.md: "the default 5 min per person is used")
DEFAULT_MINUTES_PER_PERSON = 5.0
# The last this-many service times make the average (FakeData.SERVICE_TIME_WINDOW)
WINDOW = 5
# A gap this long between calls is a break, not someone being served (DECISIONS.md
# "Wait-time estimation learns online", guards)
BREAK_MINUTES = 30
# The model takes over from the rolling average once a queue has this many service times today
MODEL_AFTER_SERVED = 10
# The live model, which the server saves after every lesson (not committed), and the
# warm-started one it begins from when there's no live one yet (ml/train.py, committed)
MODEL_PATH = Path(__file__).resolve().parents[2] / "ml" / "wait_model.joblib"
WARM_PATH = MODEL_PATH.with_name("wait_model_warm.joblib")


class RollingAverage:
    """Average of the last `window_size` samples: older ones fall out as new ones arrive, so
    it follows how fast the queue moves now, not this morning. A port of RollingAverage.java."""

    def __init__(self, window_size: int):
        if window_size < 1:
            raise ValueError("window_size must be at least 1")
        self.samples: deque[float] = deque(maxlen=window_size)

    def add(self, sample: float) -> None:
        self.samples.append(sample)  # a full deque drops its oldest by itself

    def __len__(self) -> int:
        return len(self.samples)

    def average(self) -> float:
        """0 when there are no samples yet."""
        return sum(self.samples) / len(self.samples) if self.samples else 0.0


@dataclass
class Estimate:
    minutes_per_person: float
    # How many service times the average came from (Insights: "average of the last 5 served")
    samples: int
    source: EstimateSource = EstimateSource.ROLLING_AVERAGE
    # How many served people the model has learned from, when it's the source
    model_samples: int = 0


# ---- The learning model ---------------------------------------------------------------
# One model for the whole server, loaded on first use. Requests run on several threads, so
# learning (which changes and saves it) holds a lock; predicting only reads.

_model: WaitModel | None = None
_model_path: Path = MODEL_PATH
_lock = threading.Lock()


def model() -> WaitModel:
    global _model
    if _model is None:
        with _lock:
            if _model is None:
                # What it learned last time if there is any, else the warm start
                path = _model_path if _model_path.exists() else WARM_PATH
                _model = WaitModel.load(path)
    return _model


def use_model(new_model: WaitModel | None, path: Path) -> None:
    """Swaps the model and where it's saved (the tests use a fresh one in a temporary folder)."""
    global _model, _model_path
    _model, _model_path = new_model, path


def snapshot(db: Session, queue: Queue, day: date, now: datetime,
             rolling_average: float) -> Snapshot:
    """How the queue looks right now, in the model's terms (ml/features.py), counted in one
    query."""
    calls_15, in_line, no_shows_hour, moved_back = db.execute(
        select(func.count().filter(Ticket.called_at >= now - timedelta(minutes=15)),
               func.count().filter(Ticket.status == TicketStatus.WAITING),
               func.count().filter(Ticket.status == TicketStatus.NO_SHOW,
                                   Ticket.finished_at >= now - timedelta(hours=1)),
               func.count().filter(Ticket.status == TicketStatus.WAITING, Ticket.moved_back))
        .where(Ticket.queue_id == queue.id, Ticket.service_date == day)).one()
    local = now.astimezone(MANILA)
    opened = datetime.combine(day, queue.opens_at, MANILA)
    return Snapshot(
        rolling_average=rolling_average,
        served_last_15=calls_15,
        in_line=in_line,
        no_shows_last_hour=no_shows_hour,
        moved_back_waiting=moved_back,
        hour=local.hour,
        weekday=local.weekday(),
        category=queue.category.value,
        minutes_open=max((now - opened).total_seconds() / 60, 0.0),
        grace_period=queue.grace_period_enabled,
        proximity_check=queue.proximity_check_enabled,
    )


def learn_from_call(db: Session, queue: Queue, now: datetime) -> None:
    """Call next or No-show: the person at the counter has just finished, so their real service
    time (call to call) is one lesson for the model. Called before the counter changes. A break
    (over 30 minutes) isn't a lesson. Anything going wrong here is logged and skipped: learning
    must never stop the counter from calling the next person."""
    try:
        serving = db.scalar(select(Ticket).where(Ticket.queue_id == queue.id,
                                                 Ticket.status == TicketStatus.CALLED))
        if serving is None or serving.called_at is None:
            return
        minutes = (now - serving.called_at).total_seconds() / 60
        if not 0 < minutes <= BREAK_MINUTES:
            return
        day = serving.service_date
        average = RollingAverage(WINDOW)
        for sample in service_minutes(db, [queue.id], day).get(queue.id, []):
            average.add(sample)
        rolling = average.average() if len(average) else DEFAULT_MINUTES_PER_PERSON
        row = to_row(snapshot(db, queue, day, now, rolling))
        current = model()
        with _lock:
            current.learn(row, minutes)
            current.save(_model_path)
    except Exception:  # noqa: BLE001  the counter matters more than one lesson
        log.exception("The wait-time model couldn't learn from this call")


def service_minutes(db: Session, queue_ids: list[str], day: date) -> dict[str, list[float]]:
    """Each queue's service times on `day`, oldest first, in minutes. One query for all the
    queues, so Browse doesn't run one per card."""
    calls = db.execute(
        select(Ticket.queue_id, Ticket.called_at)
        .where(Ticket.queue_id.in_(queue_ids), Ticket.service_date == day,
               Ticket.called_at.is_not(None))
        .order_by(Ticket.queue_id, Ticket.called_at))
    times: dict[str, list[float]] = defaultdict(list)
    previous: dict[str, object] = {}
    for queue_id, called_at in calls:
        if queue_id in previous:
            minutes = (called_at - previous[queue_id]).total_seconds() / 60
            if minutes <= BREAK_MINUTES:
                times[queue_id].append(minutes)
        previous[queue_id] = called_at
    return times


def estimates(db: Session, queue_ids: list[str], day: date,
              now: datetime | None = None) -> dict[str, Estimate]:
    """Minutes per person for each queue on `day`: the rolling average, or the model's
    prediction for queues with MODEL_AFTER_SERVED service times today."""
    times = service_minutes(db, queue_ids, day)
    result = {}
    ready = []
    for queue_id in queue_ids:
        average = RollingAverage(WINDOW)
        for minutes in times.get(queue_id, []):
            average.add(minutes)
        result[queue_id] = (Estimate(average.average(), len(average)) if len(average)
                            else Estimate(DEFAULT_MINUTES_PER_PERSON, 0))
        if len(times.get(queue_id, [])) >= MODEL_AFTER_SERVED:
            ready.append(queue_id)

    current = model()
    if ready and current.ready:
        now = now or timeutil.now()
        for queue in db.scalars(select(Queue).where(Queue.id.in_(ready))):
            rolling = result[queue.id]
            try:
                minutes = current.predict(to_row(snapshot(db, queue, day, now,
                                                          rolling.minutes_per_person)))
            except Exception:  # noqa: BLE001  a broken model falls back, never breaks a page
                log.exception("The wait-time model couldn't predict; using the rolling average")
                continue
            result[queue.id] = Estimate(within_band(minutes, rolling.minutes_per_person),
                                        rolling.samples, EstimateSource.MODEL, current.samples)
    return result


def estimate(db: Session, queue_id: str, day: date) -> Estimate:
    return estimates(db, [queue_id], day)[queue_id]


def wait_minutes(minutes_per_person: float, people_ahead: int) -> int:
    return round(minutes_per_person * max(people_ahead, 0))

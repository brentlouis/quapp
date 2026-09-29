"""How many minutes each person takes: the number every wait in the app is built from
(MODELS.md "The estimator"):

    wait = minutes per person × people ahead

Minutes per person is the rolling average of the last 5 service times today (BACKEND.md step
6.1). A service time is the gap between one call and the next: calling Maria at 10:00 and José
at 10:04 means Maria took 4 minutes. Before anyone has been served the default 5 minutes is
used. Phase 9 adds the learning model; callers won't have to change.
"""

from collections import defaultdict, deque
from dataclasses import dataclass
from datetime import date

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.enums import EstimateSource
from app.models import Ticket

# Before anyone has been served (MODELS.md: "the default 5 min per person is used")
DEFAULT_MINUTES_PER_PERSON = 5.0
# The last this-many service times make the average (FakeData.SERVICE_TIME_WINDOW)
WINDOW = 5
# A gap this long between calls is a break, not someone being served (DECISIONS.md
# "Wait-time estimation learns online", guards)
BREAK_MINUTES = 30


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


def estimates(db: Session, queue_ids: list[str], day: date) -> dict[str, Estimate]:
    """Minutes per person for each queue on `day`."""
    times = service_minutes(db, queue_ids, day)
    result = {}
    for queue_id in queue_ids:
        average = RollingAverage(WINDOW)
        for minutes in times.get(queue_id, []):
            average.add(minutes)
        result[queue_id] = (Estimate(average.average(), len(average)) if len(average)
                            else Estimate(DEFAULT_MINUTES_PER_PERSON, 0))
    return result


def estimate(db: Session, queue_id: str, day: date) -> Estimate:
    return estimates(db, [queue_id], day)[queue_id]


def wait_minutes(minutes_per_person: float, people_ahead: int) -> int:
    return round(minutes_per_person * max(people_ahead, 0))

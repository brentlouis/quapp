"""The wait-time model (BACKEND.md phase 9): the features, the model itself, and the server using
it. The clock is fixed at 10:00 AM on 2026-09-29 (a Tuesday); conftest gives every test a fresh,
untrained model saved in a temporary folder."""

from datetime import datetime, time, timedelta

from app.enums import EstimateSource, TicketStatus
from app.services import estimator
from app.timeutil import MANILA
from ml.features import FEATURE_NAMES, Snapshot, to_row
from ml.simulate import simulate
from ml.wait_model import MAX_MINUTES, MIN_MINUTES, WaitModel, within_band
from tests.conftest import Clock
from tests.factories import make_queue, make_ticket, make_user, sign_in


def snap(**fields) -> Snapshot:
    values = dict(rolling_average=4.0, served_last_15=3, in_line=10, no_shows_last_hour=0,
                  moved_back_waiting=0, hour=10, weekday=1, category="MEDICAL",
                  minutes_open=120.0, grace_period=True, proximity_check=False)
    values.update(fields)
    return Snapshot(**values)


def at(hour: int, minute: int = 0) -> datetime:
    return datetime.combine(Clock.DAY, time(hour, minute), MANILA)


def called(db, queue, *minutes, status=TicketStatus.SERVED):
    """Tickets called at these minutes past 9 AM today, oldest first."""
    for minute in minutes:
        make_ticket(db, queue, make_user(db), status=status,
                    called_at=at(9) + timedelta(minutes=minute))


def trained(minutes: float = 7.0) -> WaitModel:
    """A model that learned "every queue takes about this long" from a few hundred examples."""
    model = WaitModel()
    rows = [to_row(snap(in_line=n % 20)) for n in range(400)]
    model.warm_start(rows, [minutes] * 400, epochs=20)
    return model


# ---- Features ---------------------------------------------------------------------

def test_a_row_has_one_number_per_feature_name():
    assert len(to_row(snap())) == len(FEATURE_NAMES)


def test_day_part_weekday_and_category_are_one_hot():
    row = dict(zip(FEATURE_NAMES, to_row(snap(hour=12, weekday=0, category="BILLS"))))
    assert row["part_lunch"] == 1 and row["part_morning"] == 0
    assert row["day_mon"] == 1 and sum(v for k, v in row.items() if k.startswith("day_")) == 1
    assert row["category_bills"] == 1
    assert sum(v for k, v in row.items() if k.startswith("category_")) == 1


# ---- The model --------------------------------------------------------------------

def test_an_untrained_model_isnt_ready():
    assert not WaitModel().ready


def test_it_learns_how_the_line_length_changes_service_time():
    """minutes = 2 + 0.2 × people in line: after training it should predict close to that."""
    model = WaitModel()
    rows, minutes = [], []
    for n in range(2000):
        in_line = n % 30
        rows.append(to_row(snap(in_line=in_line)))
        minutes.append(2 + 0.2 * in_line)
    model.warm_start(rows, minutes, epochs=30)
    assert abs(model.predict(to_row(snap(in_line=5))) - 3.0) < 0.5
    assert abs(model.predict(to_row(snap(in_line=25))) - 7.0) < 0.5


def test_learning_one_person_counts_and_nudges_the_prediction():
    model = trained(5.0)
    before = model.predict(to_row(snap()))
    for _ in range(50):
        model.learn(to_row(snap()), 9.0)
    assert model.samples == 400 + 50
    assert model.predict(to_row(snap())) > before


def test_predictions_stay_in_a_sensible_range():
    model = trained(5.0)
    assert MIN_MINUTES <= model.predict(to_row(snap(in_line=10_000))) <= MAX_MINUTES


def test_saving_and_loading_keeps_what_it_learned(tmp_path):
    model = trained(6.0)
    model.save(tmp_path / "m.joblib")
    loaded = WaitModel.load(tmp_path / "m.joblib")
    assert loaded.samples == model.samples
    assert loaded.predict(to_row(snap())) == model.predict(to_row(snap()))


def test_loading_with_no_file_gives_a_fresh_model(tmp_path):
    assert not WaitModel.load(tmp_path / "missing.joblib").ready


def test_the_simulator_is_repeatable():
    first = simulate(2, seed=3)
    again = simulate(2, seed=3)
    assert [e.minutes for e in first] == [e.minutes for e in again]
    assert all(0 < e.minutes <= 30 for e in first)


# ---- The server -------------------------------------------------------------------

def test_the_rolling_average_is_used_until_ten_service_times(db):
    estimator.use_model(trained(9.0), estimator._model_path)
    queue = make_queue(db, make_user(db))
    called(db, queue, *range(0, 40, 4))  # 10 calls = 9 service times of 4 minutes
    estimate = estimator.estimate(db, queue.id, Clock.DAY)
    assert estimate.source == EstimateSource.ROLLING_AVERAGE
    assert estimate.minutes_per_person == 4


def test_the_model_takes_over_at_ten_service_times(db):
    model = trained(9.0)
    estimator.use_model(model, estimator._model_path)
    queue = make_queue(db, make_user(db))
    called(db, queue, *range(0, 44, 4))  # 11 calls = 10 service times
    estimate = estimator.estimate(db, queue.id, Clock.DAY)
    assert estimate.source == EstimateSource.MODEL
    assert estimate.model_samples == model.samples
    # It learned 9 minutes, but the counter's own average is 4: kept to double that
    assert estimate.minutes_per_person == 8.0


def test_the_model_follows_its_prediction_inside_the_band(db):
    estimator.use_model(trained(5.0), estimator._model_path)
    queue = make_queue(db, make_user(db))
    called(db, queue, *range(0, 44, 4))  # rolling average 4: the band is 2 to 8
    estimate = estimator.estimate(db, queue.id, Clock.DAY)
    assert estimate.source == EstimateSource.MODEL
    assert 4.0 < estimate.minutes_per_person < 6.0


def test_the_band_keeps_the_model_near_the_counter():
    assert within_band(0.5, 3.0) == 1.5
    assert within_band(20.0, 3.0) == 6.0
    assert within_band(4.0, 3.0) == 4.0


def test_an_untrained_model_leaves_the_rolling_average(db):
    queue = make_queue(db, make_user(db))
    called(db, queue, *range(0, 60, 4))
    assert estimator.estimate(db, queue.id, Clock.DAY).source == EstimateSource.ROLLING_AVERAGE


def test_call_next_teaches_the_model_and_saves_it(client, db, wait_model):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    make_ticket(db, queue, make_user(db), status=TicketStatus.CALLED, called_at=at(9, 56))
    make_ticket(db, queue, make_user(db))
    client.post(f"/queues/{queue.id}/call-next", headers=sign_in(db, organizer))
    assert wait_model.samples == 1
    assert estimator._model_path.exists()


def test_a_no_show_is_a_lesson_too(client, db, wait_model):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    make_ticket(db, queue, make_user(db), status=TicketStatus.CALLED, called_at=at(9, 57))
    client.post(f"/queues/{queue.id}/no-show", headers=sign_in(db, organizer))
    assert wait_model.samples == 1


def test_a_break_isnt_a_lesson(client, db, wait_model):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    make_ticket(db, queue, make_user(db), status=TicketStatus.CALLED, called_at=at(9, 15))
    client.post(f"/queues/{queue.id}/call-next", headers=sign_in(db, organizer))
    assert wait_model.samples == 0


def test_insights_says_the_model_made_the_estimate(client, db):
    model = trained(9.0)
    estimator.use_model(model, estimator._model_path)
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    called(db, queue, *range(0, 44, 4))
    stats = client.get(f"/queues/{queue.id}/stats", headers=sign_in(db, organizer)).json()
    assert stats["estimate_source"] == "MODEL"
    assert stats["model_samples"] == model.samples

"""The rolling average and the organizer's stats (BACKEND.md steps 6.1 and 6.2). The clock is
fixed at 10:00 AM on 2026-09-29."""

from datetime import datetime, time, timedelta

import pytest

from app.enums import RemovalReason, TicketStatus
from app.services import estimator
from app.services.estimator import RollingAverage
from app.timeutil import MANILA
from tests.conftest import Clock
from tests.factories import make_queue, make_ticket, make_user, sign_in


def at(hour: int, minute: int = 0, day=Clock.DAY) -> datetime:
    return datetime.combine(day, time(hour, minute), MANILA)


def called(db, queue, *minutes, day=Clock.DAY, status=TicketStatus.SERVED):
    """Tickets called at these minutes past 9 AM, oldest first."""
    for minute in minutes:
        make_ticket(db, queue, make_user(db), status=status, service_date=day,
                    called_at=at(9, 0, day) + timedelta(minutes=minute))


# ---- RollingAverage (RollingAverageTest.java, ported) ---------------------------

def test_an_empty_average_is_zero():
    average = RollingAverage(3)
    assert len(average) == 0
    assert average.average() == 0


def test_it_averages_the_samples_while_under_the_window():
    average = RollingAverage(3)
    average.add(2)
    average.add(4)
    assert len(average) == 2
    assert average.average() == 3


def test_the_oldest_sample_drops_out_when_the_window_is_full():
    average = RollingAverage(3)
    for sample in (100, 1, 2, 3):  # 100 falls out
        average.add(sample)
    assert len(average) == 3
    assert average.average() == 2


def test_a_window_smaller_than_one_is_refused():
    with pytest.raises(ValueError):
        RollingAverage(0)


# ---- Minutes per person -----------------------------------------------------------

def test_before_anyone_is_served_the_default_is_used(db):
    queue = make_queue(db, make_user(db))
    estimate = estimator.estimate(db, queue.id, Clock.DAY)
    assert estimate.minutes_per_person == 5
    assert estimate.samples == 0


def test_a_service_time_is_the_gap_between_calls(db):
    queue = make_queue(db, make_user(db))
    called(db, queue, 0, 4, 10)  # gaps of 4 and 6 minutes
    estimate = estimator.estimate(db, queue.id, Clock.DAY)
    assert estimate.minutes_per_person == 5
    assert estimate.samples == 2


def test_only_the_last_five_count(db):
    queue = make_queue(db, make_user(db))
    called(db, queue, 0, 20, 21, 22, 23, 24, 25)  # a slow 20 at first, then 1s
    assert estimator.estimate(db, queue.id, Clock.DAY).minutes_per_person == 1


def test_a_long_gap_is_a_break_not_a_service(db):
    queue = make_queue(db, make_user(db))
    called(db, queue, 0, 2, 45, 47)  # 2, a 43-minute lunch, 2
    estimate = estimator.estimate(db, queue.id, Clock.DAY)
    assert estimate.minutes_per_person == 2
    assert estimate.samples == 2


def test_only_todays_calls_count(db):
    queue = make_queue(db, make_user(db), end_date=Clock.DAY + timedelta(days=1))
    yesterday = Clock.DAY - timedelta(days=1)
    called(db, queue, 0, 10, 20, day=yesterday)
    assert estimator.estimate(db, queue.id, Clock.DAY).samples == 0


def test_the_average_drives_every_wait(client, db):
    """2 minutes per person: Browse, a ticket and moving back all use it."""
    queue = make_queue(db, make_user(db))
    called(db, queue, 0, 2, 4)
    maria = make_user(db)
    for _ in range(3):
        make_ticket(db, queue, make_user(db))  # ahead of Maria
    mine = make_ticket(db, queue, maria)
    for _ in range(8):
        make_ticket(db, queue, make_user(db))  # behind her
    headers = sign_in(db, maria)

    detail = client.get(f"/queues/{queue.id}").json()
    assert detail["estimated_wait_minutes"] == 12 * 2
    assert detail["minutes_per_person"] == 2
    assert detail["service_sample_count"] == 2
    assert detail["next_ticket_number"] == 16  # 3 called + 12 waiting so far
    assert client.get(f"/tickets/{mine.id}", headers=headers).json()[
        "estimated_wait_minutes"] == 3 * 2
    # 10 minutes at 2 per person = 5 places back
    moved = client.post(f"/tickets/{mine.id}/move-back", json={"minutes_needed": 10},
                        headers=headers).json()
    assert moved["position"] == 4 + 5


# ---- Stats ------------------------------------------------------------------------

def test_the_organizers_numbers_for_today(client, db):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    called(db, queue, 0, 3, 6)                               # 3 served
    called(db, queue, 9, status=TicketStatus.NO_SHOW)        # 1 no-show
    make_ticket(db, queue, make_user(db), status=TicketStatus.REMOVED,
                removal_reason=RemovalReason.PRANK)          # a prank counts as one
    make_ticket(db, queue, make_user(db), status=TicketStatus.REMOVED,
                removal_reason=RemovalReason.DUPLICATE)      # a duplicate doesn't
    for _ in range(4):
        make_ticket(db, queue, make_user(db))                # 4 waiting

    body = client.get(f"/queues/{queue.id}/stats", headers=sign_in(db, organizer)).json()
    assert body == {
        "served_today": 3,
        "no_shows_today": 2,
        "waiting_now": 4,
        "average_service_minutes": 3.0,
        "service_sample_count": 3,
        "projected_wait_minutes": 12,
        "estimate_source": "ROLLING_AVERAGE",
        "model_samples": 0,
    }


def test_stats_are_for_the_organizer_only(client, db):
    queue = make_queue(db, make_user(db))
    response = client.get(f"/queues/{queue.id}/stats", headers=sign_in(db, make_user(db)))
    assert response.status_code == 403

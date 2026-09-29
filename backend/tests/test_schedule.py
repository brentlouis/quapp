"""The schedule on its own (BACKEND.md step 3.2): the time is passed in, so each test says
exactly when "now" is."""

from datetime import datetime, time, timedelta

from app.enums import QueueStatus, TicketStatus
from app.services import schedule
from app.timeutil import MANILA
from tests.factories import make_queue, make_ticket, make_user

DAY = datetime(2026, 9, 29).date()


def at(hour: int, minute: int = 0, day=DAY) -> datetime:
    return datetime.combine(day, time(hour, minute), MANILA)


def test_an_upcoming_queue_opens_at_its_opening_time(db):
    queue = make_queue(db, make_user(db), status=QueueStatus.UPCOMING, start_date=DAY,
                       end_date=DAY, opens_at=time(8), closes_at=time(17))
    schedule.refresh(db, queue, at(7, 59))
    assert queue.status == QueueStatus.UPCOMING
    schedule.refresh(db, queue, at(8, 0))
    assert queue.status == QueueStatus.OPEN


def test_a_queue_closes_at_its_last_closing_time_and_releases_the_line(db):
    queue = make_queue(db, make_user(db), start_date=DAY, end_date=DAY,
                       opens_at=time(8), closes_at=time(17))
    waiting = make_ticket(db, queue, make_user(db), service_date=DAY)
    called = make_ticket(db, queue, make_user(db), service_date=DAY,
                         status=TicketStatus.CALLED, called_at=at(16, 50))

    schedule.refresh(db, queue, at(17, 0))

    assert queue.status == QueueStatus.CLOSED
    assert queue.closed_at == at(17, 0)
    for ticket in (waiting, called):
        assert ticket.status == TicketStatus.QUEUE_CLOSED  # never a no-show
        assert ticket.finished_at == at(17, 0)


def test_a_paused_queue_still_closes_on_time(db):
    queue = make_queue(db, make_user(db), status=QueueStatus.PAUSED, paused_at=at(12),
                       start_date=DAY, end_date=DAY, opens_at=time(8), closes_at=time(17))
    schedule.refresh(db, queue, at(18))
    assert queue.status == QueueStatus.CLOSED
    assert queue.paused_at is None


def test_a_multi_day_queue_releases_each_day_and_stays_open(db):
    tomorrow = DAY + timedelta(days=1)
    queue = make_queue(db, make_user(db), start_date=DAY, end_date=tomorrow,
                       opens_at=time(8), closes_at=time(17))
    today_ticket = make_ticket(db, queue, make_user(db), service_date=DAY)
    tomorrow_ticket = make_ticket(db, queue, make_user(db), service_date=tomorrow)

    schedule.refresh(db, queue, at(18))  # evening of day one

    assert queue.status == QueueStatus.OPEN
    assert today_ticket.status == TicketStatus.QUEUE_CLOSED
    assert tomorrow_ticket.status == TicketStatus.WAITING


def test_overnight_a_multi_day_queue_is_open_but_not_within_hours(db):
    queue = make_queue(db, make_user(db), start_date=DAY, end_date=DAY + timedelta(days=1),
                       opens_at=time(8), closes_at=time(17))
    assert schedule.is_within_hours(queue, at(10))
    assert not schedule.is_within_hours(queue, at(22))
    assert not schedule.is_within_hours(queue, at(7, day=DAY + timedelta(days=2)))


def test_closing_by_hand_releases_everyone_including_early_joiners(db):
    queue = make_queue(db, make_user(db), start_date=DAY, end_date=DAY + timedelta(days=1))
    early = make_ticket(db, queue, make_user(db), service_date=DAY + timedelta(days=1))
    schedule.close_now(db, queue, at(12))
    assert queue.status == QueueStatus.CLOSED
    assert early.status == TicketStatus.QUEUE_CLOSED

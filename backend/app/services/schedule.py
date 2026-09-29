"""When a queue runs, and moving it along on time (MODELS.md "Queue", status).

UPCOMING becomes OPEN at its first opening time; OPEN or PAUSED becomes CLOSED at its last
closing time. On a multi-day queue each day's tickets end at that day's closing time.

This runs lazily: every endpoint that reads or changes a queue calls refresh() first. There's
no background timer to keep alive, and a queue is never more than one request out of date.
"""

from datetime import date, datetime

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.enums import QueueStatus, TicketStatus
from app.models import Queue, Ticket
from app.timeutil import MANILA

LIVE_TICKETS = (TicketStatus.WAITING, TicketStatus.CALLED)


def opening(queue: Queue, day: date) -> datetime:
    """The moment the queue opens on a day, in Manila."""
    return datetime.combine(day, queue.opens_at, MANILA)


def closing(queue: Queue, day: date) -> datetime:
    """The moment the queue closes on a day, in Manila."""
    return datetime.combine(day, queue.closes_at, MANILA)


def is_within_hours(queue: Queue, now: datetime) -> bool:
    """Open for business right now: within the run's dates and today's hours. A multi-day
    queue stays OPEN overnight, but nobody should be able to join it then (BACKEND.md 4.1)."""
    day = now.astimezone(MANILA).date()
    return (queue.start_date <= day <= queue.end_date
            and opening(queue, day) <= now < closing(queue, day))


def refresh(db: Session, queue: Queue, now: datetime) -> None:
    """Brings one queue's status, and its tickets, up to `now`. The caller commits."""
    refresh_many(db, [queue], now)


def refresh_all(db: Session, now: datetime) -> None:
    """Every queue that isn't closed yet. Fine for Quapp's size (dozens of queues); a big
    deployment would do this in SQL instead."""
    refresh_many(db, list(db.scalars(select(Queue).where(Queue.status != QueueStatus.CLOSED))), now)


def refresh_many(db: Session, queues: list[Queue], now: datetime) -> None:
    """The statuses are worked out in Python; the tickets that may have ended come from one
    query for all the queues, not one per queue (the N+1 problem, BACKEND.md 3.1)."""
    for queue in queues:
        if queue.status == QueueStatus.UPCOMING and now >= opening(queue, queue.start_date):
            queue.status = QueueStatus.OPEN
        if queue.status != QueueStatus.CLOSED and now >= closing(queue, queue.end_date):
            queue.status = QueueStatus.CLOSED
            queue.closed_at = closing(queue, queue.end_date)
            queue.paused_at = None
    release_ended_tickets(db, queues, now)


def release_ended_tickets(db: Session, queues: list[Queue], now: datetime) -> None:
    """Live tickets whose day has closed become QUEUE_CLOSED. Never a no-show: the queue
    ended, the person didn't miss their turn (MODELS.md "Rules the server enforces")."""
    if not queues:
        return
    by_id = {queue.id: queue for queue in queues}
    today = now.astimezone(MANILA).date()
    tickets = db.scalars(select(Ticket).where(Ticket.queue_id.in_(by_id),
                                              Ticket.status.in_(LIVE_TICKETS),
                                              Ticket.service_date <= today))
    for ticket in tickets:
        ended = closing(by_id[ticket.queue_id], ticket.service_date)
        if now >= ended:
            ticket.status = TicketStatus.QUEUE_CLOSED
            ticket.finished_at = ended


def close_now(db: Session, queue: Queue, now: datetime) -> None:
    """The organizer closes the queue: every live ticket, whatever its day, is released."""
    queue.status = QueueStatus.CLOSED
    queue.closed_at = now
    queue.paused_at = None
    for ticket in db.scalars(select(Ticket).where(Ticket.queue_id == queue.id,
                                                  Ticket.status.in_(LIVE_TICKETS))):
        ticket.status = TicketStatus.QUEUE_CLOSED
        ticket.finished_at = now

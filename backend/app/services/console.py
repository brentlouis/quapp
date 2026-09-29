"""The organizer's Live console: the line, calling, no-shows, walk-ins, removing someone
(BACKEND.md phase 5; MODELS.md "Line").

Everything that changes who's at the counter locks the queue's row first, like joining does, so
two taps of "Call next" from two phones can't call two people at once.
"""

from dataclasses import dataclass
from datetime import date, datetime

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.enums import QueueStatus, RemovalReason, TicketStatus
from app.errors import ApiError
from app.models import Queue, Ticket, TicketRemoval, User
from app.services import schedule
from app.services.tickets import GRACE, locked_queue
from app.timeutil import MANILA


@dataclass
class Line:
    now_serving: Ticket | None
    now_serving_timed_out: bool
    waiting: list[Ticket]


def line_day(queue: Queue, now: datetime) -> date:
    """The day the console is running: today once the queue has started, else its first day
    (so an upcoming queue's console shows the people who joined early)."""
    today = now.astimezone(MANILA).date()
    return queue.start_date if queue.start_date > today else today


def at_counter(db: Session, queue: Queue) -> Ticket | None:
    """The CALLED ticket. Calling finishes the previous one first, so there's at most one."""
    return db.scalar(select(Ticket).where(Ticket.queue_id == queue.id,
                                          Ticket.status == TicketStatus.CALLED))


def timed_out(queue: Queue, ticket: Ticket | None, now: datetime) -> bool:
    """3 minutes since the call and no "I'm here", on a queue with the grace period. The
    organizer sees it and decides; the server never marks the no-show by itself."""
    return (ticket is not None and queue.grace_period_enabled and ticket.here_at is None
            and now >= ticket.called_at + GRACE)


def line(db: Session, queue: Queue, now: datetime) -> Line:
    waiting = list(db.scalars(select(Ticket).where(
        Ticket.queue_id == queue.id, Ticket.status == TicketStatus.WAITING,
        Ticket.service_date == line_day(queue, now)).order_by(Ticket.line_order)))
    serving = at_counter(db, queue)
    return Line(serving, timed_out(queue, serving, now), waiting)


def check_running(queue: Queue) -> None:
    """Calling needs an open queue; a paused one still serves the people already in line."""
    if queue.status not in (QueueStatus.OPEN, QueueStatus.PAUSED):
        raise ApiError(409, "WRONG_STATUS", "The line isn't running: the queue isn't open.")


def call_next(db: Session, queue_id: str, now: datetime) -> Queue:
    """The person at the counter is served; the first in line is called. Commits."""
    queue = locked_queue(db, queue_id)
    schedule.refresh(db, queue, now)
    check_running(queue)
    finish_counter(db, queue, TicketStatus.SERVED, now)
    call_first(db, queue, now)
    db.commit()
    return queue


def no_show(db: Session, queue_id: str, now: datetime) -> Queue:
    """The person at the counter didn't come: a no-show (a strike on penalty queues, services/
    tickets.check_cooldown), and the next one is called. Commits."""
    queue = locked_queue(db, queue_id)
    schedule.refresh(db, queue, now)
    check_running(queue)
    if at_counter(db, queue) is None:
        raise ApiError(409, "NOBODY_CALLED", "Nobody is at the counter.")
    finish_counter(db, queue, TicketStatus.NO_SHOW, now)
    call_first(db, queue, now)
    db.commit()
    return queue


def finish_counter(db: Session, queue: Queue, status: TicketStatus, now: datetime) -> None:
    serving = at_counter(db, queue)
    if serving is not None:
        serving.status = status
        serving.finished_at = now


def call_first(db: Session, queue: Queue, now: datetime) -> None:
    first = db.scalar(select(Ticket).where(
        Ticket.queue_id == queue.id, Ticket.status == TicketStatus.WAITING,
        Ticket.service_date == line_day(queue, now)).order_by(Ticket.line_order).limit(1))
    if first is not None:
        first.status = TicketStatus.CALLED
        first.called_at = now
    # Written now, so the next query in this transaction sees who's at the counter
    db.flush()


def walk_in(db: Session, queue_id: str, name: str, now: datetime) -> Ticket:
    """Someone without a phone joins at the counter: a ticket with no account, numbered like
    a join (the same lock), at the back of today's line. Commits."""
    queue = locked_queue(db, queue_id)
    schedule.refresh(db, queue, now)
    check_running(queue)
    number = queue.next_ticket_number
    queue.next_ticket_number += 1
    ticket = Ticket(queue_id=queue.id, user_id=None, holder_name=name, holder_phone=None,
                    walk_in=True, ticket_number=number, line_order=float(number),
                    service_date=line_day(queue, now), status=TicketStatus.WAITING,
                    joined_at=now)
    db.add(ticket)
    db.commit()
    return ticket


def remove(db: Session, ticket: Ticket, reason: RemovalReason, by: User,
           now: datetime) -> TicketRemoval:
    """The organizer takes someone out of the line, with a reason. PRANK counts as a strike
    (services/tickets.check_cooldown) and goes to the admin; the others don't. Commits."""
    locked_queue(db, ticket.queue_id)
    db.refresh(ticket)
    if ticket.status not in (TicketStatus.WAITING, TicketStatus.CALLED):
        raise ApiError(409, "WRONG_STATUS", "This ticket has already finished.")
    ticket.status = TicketStatus.REMOVED
    ticket.removal_reason = reason
    ticket.finished_at = now
    removal = TicketRemoval(ticket_id=ticket.id, queue_id=ticket.queue_id, reason=reason,
                            removed_by=by.id, created_at=now)
    db.add(removal)
    db.commit()
    return removal

"""Joining a queue and where a ticket stands (BACKEND.md phase 4; MODELS.md "Ticket" and
"Rules the server enforces").

The rules live here rather than in the router so they can be tested, and reused, without
HTTP. Each refusal is an ApiError with its own code from MODELS.md "Errors", so the app can
explain exactly why.
"""

import math
from datetime import date, datetime, timedelta

from sqlalchemy import func, or_, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.enums import QueueStatus, RemovalReason, TicketStatus
from app.errors import ApiError
from app.models import Queue, Ticket, User
from app.services import estimator, schedule
from app.timeutil import MANILA

LIVE = (TicketStatus.WAITING, TicketStatus.CALLED)

# No-show cooldown (DECISIONS.md "No-show cooldown: 2 strikes, 30 minutes, penalty queues only")
STRIKES_FOR_COOLDOWN = 2
COOLDOWN = timedelta(minutes=30)

# The app measures distance with Android's Location.distanceBetween, which can differ from the
# haversine formula by up to about 0.5%; allow 1% so nobody the app let through is refused.
RADIUS_ALLOWANCE = 1.01
EARTH_RADIUS_METERS = 6_371_000


# ---- Joining --------------------------------------------------------------------

def join(db: Session, queue_id: str, user: User, latitude: float | None,
         longitude: float | None, now: datetime) -> Ticket:
    """Puts `user` at the back of the line, or raises the ApiError saying why not.
    Commits on success."""
    # Lock the queue's row until this transaction ends. A second join on the same queue
    # waits here, so two people can never read the same next_ticket_number, and the checks
    # below can't race either. populate_existing re-reads the row even if it's cached.
    queue = db.scalar(select(Queue).where(Queue.id == queue_id).with_for_update()
                      .execution_options(populate_existing=True))
    if queue is None:
        raise ApiError(404, "QUEUE_NOT_FOUND", "That queue doesn't exist.")
    schedule.refresh(db, queue, now)

    day = joining_day(queue, now)
    already = db.scalar(select(Ticket).where(Ticket.queue_id == queue.id,
                                             Ticket.user_id == user.id,
                                             Ticket.status.in_(LIVE)))
    if already is not None:
        raise ApiError(409, "ALREADY_IN_LINE", "You're already in this line.",
                       ticket_id=already.id)
    if queue.no_show_cooldown_enabled:
        check_cooldown(db, user, now)
    check_overlap(db, user, queue, day)
    if queue.proximity_check_enabled:
        check_distance(queue, latitude, longitude)

    number = queue.next_ticket_number
    queue.next_ticket_number += 1
    ticket = Ticket(queue_id=queue.id, user_id=user.id, holder_name=user.name,
                    holder_phone=user.phone, walk_in=False, ticket_number=number,
                    line_order=float(number), service_date=day, status=TicketStatus.WAITING,
                    joined_at=now)
    db.add(ticket)
    try:
        db.commit()
    except IntegrityError:
        # The partial unique index: the same person joined twice at once from two phones
        db.rollback()
        raise ApiError(409, "ALREADY_IN_LINE", "You're already in this line.")
    return ticket


def joining_day(queue: Queue, now: datetime) -> date:
    """The day a new ticket is for, or why the queue isn't taking joins. Upcoming queues take
    early joins for their first day; open ones take joins for today until today's closing."""
    today = now.astimezone(MANILA).date()
    if queue.status == QueueStatus.PAUSED:
        raise ApiError(409, "QUEUE_PAUSED", "This queue isn't taking new joins right now.")
    if queue.status == QueueStatus.CLOSED:
        raise ApiError(409, "QUEUE_NOT_OPEN", "This queue has closed.")
    if queue.status == QueueStatus.UPCOMING:
        return queue.start_date
    # OPEN: a multi-day queue stays OPEN overnight, but today's line has ended
    if now >= schedule.closing(queue, today):
        raise ApiError(409, "QUEUE_NOT_OPEN", "This queue has closed for today.")
    return today


def check_cooldown(db: Session, user: User, now: datetime) -> None:
    """Strikes, oldest first: every second one starts a 30-minute cooldown, and the count
    starts again. Blocked while the latest cooldown hasn't run out."""
    strikes = db.scalars(
        select(Ticket.finished_at)
        .join(Queue, Queue.id == Ticket.queue_id)
        .where(Ticket.user_id == user.id, Queue.no_show_cooldown_enabled,
               Ticket.finished_at.is_not(None),
               or_(Ticket.status == TicketStatus.NO_SHOW,
                   Ticket.removal_reason == RemovalReason.PRANK))
        .order_by(Ticket.finished_at))
    count = 0
    blocked_until = None
    for struck_at in strikes:
        count += 1
        if count == STRIKES_FOR_COOLDOWN:
            blocked_until = struck_at + COOLDOWN
            count = 0
    if blocked_until is not None and now < blocked_until:
        raise ApiError(409, "COOLDOWN",
                       "You missed two calls, so penalty queues are paused for you for 30 minutes.",
                       until=blocked_until.astimezone(MANILA).isoformat(timespec="seconds"))


def check_overlap(db: Session, user: User, queue: Queue, day: date) -> None:
    """Several tickets are fine, but not two you'd have to stand in at once: same day, and the
    hours overlap. Touching ends (one closes at 12:00, the other opens at 12:00) don't count
    (TicketRules.hoursOverlap)."""
    others = db.execute(
        select(Ticket.service_date, Queue)
        .join(Queue, Queue.id == Ticket.queue_id)
        .where(Ticket.user_id == user.id, Ticket.status.in_(LIVE), Queue.id != queue.id))
    for other_day, other in others:
        same_day = other_day == day
        share_hours = queue.opens_at < other.closes_at and other.opens_at < queue.closes_at
        if same_day and share_hours:
            raise ApiError(409, "HOURS_OVERLAP",
                           f"You're already in line at {other.name} at the same time.",
                           other_queue_id=other.id, other_queue_name=other.name)


def check_distance(queue: Queue, latitude: float | None, longitude: float | None) -> None:
    if latitude is None or longitude is None:
        raise ApiError(422, "LOCATION_NEEDED", "This queue needs your location to join.")
    meters = distance_meters(latitude, longitude, queue.latitude, queue.longitude)
    if meters > queue.join_radius_meters * RADIUS_ALLOWANCE:
        raise ApiError(409, "TOO_FAR", "You're too far from the venue to join.",
                       distance_meters=round(meters), join_radius_meters=queue.join_radius_meters)


def distance_meters(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    """Haversine: the distance along the Earth's surface, treating it as a sphere."""
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lambda = math.radians(lng2 - lng1)
    a = math.sin(d_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    return 2 * EARTH_RADIUS_METERS * math.asin(math.sqrt(a))


# ---- Where a ticket stands ------------------------------------------------------------

def positions(db: Session, tickets: list[Ticket]) -> dict[str, int]:
    """Position of each WAITING ticket: how many WAITING tickets in its queue have a smaller
    line_order, plus one. Others are 0 (MODELS.md "Ticket"). One query per ticket asked
    about; My tickets holds a handful, so that's fine."""
    result = {}
    for ticket in tickets:
        if ticket.status != TicketStatus.WAITING:
            result[ticket.id] = 0
            continue
        ahead = db.scalar(select(func.count()).select_from(Ticket).where(
            Ticket.queue_id == ticket.queue_id, Ticket.status == TicketStatus.WAITING,
            Ticket.line_order < ticket.line_order))
        result[ticket.id] = ahead + 1
    return result


def wait_for(ticket: Ticket, position: int) -> int:
    """The people ahead are position − 1; the wait is them × minutes per person."""
    if position <= 0:
        return 0
    return estimator.wait_minutes(ticket.queue, position - 1)

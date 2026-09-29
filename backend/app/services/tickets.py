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


# The minutes "I need more time" offers (TicketRules.MORE_TIME_CHOICES)
MORE_TIME_CHOICES = (5, 10, 15, 20, 30, 45)


def locked_queue(db: Session, queue_id: str) -> Queue:
    """The queue, with its row locked until this transaction ends.

    Everything that changes a line (joining, calling, walk-ins, moving back) takes this lock
    first. A second request on the same queue waits here, so two people can never read the
    same next_ticket_number, and two "Call next" taps can't call two people.
    populate_existing re-reads the row even if this session already has it cached."""
    queue = db.scalar(select(Queue).where(Queue.id == queue_id).with_for_update()
                      .execution_options(populate_existing=True))
    if queue is None:
        raise ApiError(404, "QUEUE_NOT_FOUND", "That queue doesn't exist.")
    return queue


# ---- Joining --------------------------------------------------------------------

def join(db: Session, queue_id: str, user: User, latitude: float | None,
         longitude: float | None, now: datetime, holder_name: str | None = None,
         holder_phone: str | None = None) -> Ticket:
    """Puts `user` at the back of the line, or raises the ApiError saying why not. The ticket
    is the account's; the name and phone on it default to the account's too. Commits on success."""
    queue = locked_queue(db, queue_id)
    schedule.refresh(db, queue, now)

    day = joining_day(queue, now)
    already = db.scalar(select(Ticket).where(Ticket.queue_id == queue.id,
                                             Ticket.user_id == user.id,
                                             Ticket.status.in_(LIVE)))
    if already is not None and missed_call(queue, already, now):
        # Called, 3 minutes passed and the app told them the slot went: joining again settles
        # that ticket as the no-show it was, before the organizer gets to it
        already.status = TicketStatus.NO_SHOW
        already.finished_at = now
        db.flush()
        already = None
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
    ticket = Ticket(queue_id=queue.id, user_id=user.id, holder_name=holder_name or user.name,
                    holder_phone=holder_phone or user.phone, walk_in=False, ticket_number=number,
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


def missed_call(queue: Queue, ticket: Ticket, now: datetime) -> bool:
    """Called on a queue with the grace period, 3 minutes gone and no "I'm here"."""
    return (ticket.status == TicketStatus.CALLED and queue.grace_period_enabled
            and ticket.here_at is None and now >= ticket.called_at + GRACE)


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
    """Blocked while the latest cooldown hasn't run out (cooldown_state)."""
    blocked_until, _ = cooldown_state(db, user, now)
    if blocked_until is not None:
        raise ApiError(409, "COOLDOWN",
                       "You missed two calls, so penalty queues are paused for you for 30 minutes.",
                       until=blocked_until.astimezone(MANILA).isoformat(timespec="seconds"))


def cooldown_state(db: Session, user: User, now: datetime) -> tuple[datetime | None, int]:
    """Strikes, oldest first: every second one starts a 30-minute cooldown, and the count
    starts again. Returns when the current cooldown ends (None if there isn't one running now)
    and how many strikes count toward the next one."""
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
    running = blocked_until is not None and now < blocked_until
    return (blocked_until if running else None), count


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


# ---- When you're called ----------------------------------------------------------

# The grace period for "I'm here" (MODELS.md "Queue": presence confirmation, 3 min)
GRACE = timedelta(minutes=3)


def here(db: Session, ticket: Ticket, now: datetime) -> Ticket:
    """"I'm here": the console shows the person as confirmed. With the grace period on, only
    within 3 minutes of the call. Commits."""
    if ticket.status != TicketStatus.CALLED:
        raise ApiError(409, "NOT_CALLED", "You haven't been called yet.")
    if ticket.queue.grace_period_enabled and now >= ticket.called_at + GRACE:
        raise ApiError(409, "GRACE_OVER", "The 3 minutes to confirm have passed.")
    if ticket.here_at is None:  # tapping twice keeps the first time
        ticket.here_at = now
    db.commit()
    return ticket


def places_to_move_back(minutes_needed: int, minutes_per_person: float, people_behind: int) -> int:
    """Time ÷ minutes per person, rounded up so you get at least the time asked, at least one
    place, never past the end of the line (TicketRules.placesToMoveBack; MODELS.md "Moving
    back")."""
    if minutes_needed <= 0 or people_behind <= 0:
        return 0
    per_person = minutes_per_person if minutes_per_person > 0 else estimator.DEFAULT_MINUTES_PER_PERSON
    places = math.ceil(minutes_needed / per_person)
    return min(max(places, 1), people_behind)


def move_back(db: Session, ticket: Ticket, minutes_needed: int, now: datetime) -> None:
    """"I need more time": the ticket lets `places` people go ahead of it and keeps its
    number (only line_order changes). Once per ticket, never a no-show. A called ticket hands
    the counter back and rejoins the line. Doesn't commit: the router commits, or rolls back
    for a preview."""
    queue = locked_queue(db, ticket.queue_id)
    db.refresh(ticket)
    if ticket.status not in (TicketStatus.WAITING, TicketStatus.CALLED):
        raise ApiError(409, "WRONG_STATUS", "This ticket has already finished.")
    if ticket.moved_back:
        raise ApiError(409, "ALREADY_MOVED_BACK", "You've already asked for more time once.")

    # Everyone still waiting behind this ticket today. A called ticket is at the front, so
    # everyone waiting is behind it.
    behind = select(Ticket).where(Ticket.queue_id == queue.id, Ticket.id != ticket.id,
                                  Ticket.status == TicketStatus.WAITING,
                                  Ticket.service_date == ticket.service_date)
    if ticket.status == TicketStatus.WAITING:
        behind = behind.where(Ticket.line_order > ticket.line_order)
    behind = list(db.scalars(behind.order_by(Ticket.line_order)))
    if not behind:
        raise ApiError(409, "LAST_IN_LINE", "There's nobody behind you to let ahead.")

    per_person = estimator.estimate(db, queue.id, ticket.service_date).minutes_per_person
    places = places_to_move_back(minutes_needed, per_person, len(behind))
    # Land between the ticket `places` back and the one after it (45.5 between #45 and #46),
    # or just after the last one
    target = behind[places - 1]
    after = behind[places] if places < len(behind) else None
    ticket.line_order = ((target.line_order + after.line_order) / 2 if after
                         else target.line_order + 1)
    ticket.moved_back = True
    ticket.moved_back_at = now
    if ticket.status == TicketStatus.CALLED:
        # Back in line: the call didn't happen as far as the counter is concerned
        ticket.status = TicketStatus.WAITING
        ticket.called_at = None
        ticket.here_at = None
    db.flush()


# ---- Where a ticket stands ------------------------------------------------------------

def positions(db: Session, tickets: list[Ticket]) -> dict[str, int]:
    """Position of each WAITING ticket: how many WAITING tickets for the same queue and day
    have a smaller line_order, plus one. Others are 0 (MODELS.md "Ticket"). One query per
    ticket asked about: My tickets holds a handful. The console's line already has them in
    order, so it numbers them itself instead."""
    result = {}
    for ticket in tickets:
        if ticket.status != TicketStatus.WAITING:
            result[ticket.id] = 0
            continue
        ahead = db.scalar(select(func.count()).select_from(Ticket).where(
            Ticket.queue_id == ticket.queue_id, Ticket.status == TicketStatus.WAITING,
            Ticket.service_date == ticket.service_date,
            Ticket.line_order < ticket.line_order))
        result[ticket.id] = ahead + 1
    return result

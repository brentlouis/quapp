"""Queues: browsing, one queue, the organizer's own, creating, editing, and running one
(pause, resume, close, extend). BACKEND.md phase 3; MODELS.md "Queue" and "Endpoints".

Every endpoint brings queues up to date with the schedule first (services/schedule.py), so
a queue whose opening time has passed is OPEN before anyone sees it.
"""

from datetime import datetime

from fastapi import APIRouter, Depends
from sqlalchemy import case, func, or_, select
from sqlalchemy.orm import Session, selectinload

from app import timeutil
from app.database import get_db
from app.deps import current_user, owned_queue, queue_or_404
from app.enums import Category, QueueStatus, RemovalReason, TicketStatus, VerificationStatus
from app.errors import ApiError, documented
from app.models import Queue, Ticket, User
from app.schemas import ExtendIn, QueueIn, QueueOut, QueuePatch, QueueStatsOut
from app.services import estimator, schedule
from app.services.line import live_numbers

router = APIRouter()

# The radius choices in Create Queue (MODELS.md: 500, 1000, 2000 or 5000)
RADII = (500, 1000, 2000, 5000)

# Browse and Your queues list live queues first, then upcoming, then closed
STATUS_ORDER = case({QueueStatus.OPEN: 0, QueueStatus.PAUSED: 1,
                     QueueStatus.UPCOMING: 2, QueueStatus.CLOSED: 3}, value=Queue.status)


# ---- Turning rows into JSON -----------------------------------------------------

def to_out(db: Session, queues: list[Queue]) -> list[QueueOut]:
    """Queue rows → QueueOut, with the organizer and the live numbers filled in. The numbers
    for all of them come from three queries: two in services/line.py, one for the service
    times in services/estimator.py."""
    ids = [q.id for q in queues]
    numbers = live_numbers(db, ids, timeutil.today())
    per_person = estimator.estimates(db, ids, timeutil.today())
    out = []
    for queue in queues:
        organizer = queue.organizer
        verified = organizer.verification_status == VerificationStatus.VERIFIED
        live = numbers[queue.id]
        columns = {column.key: getattr(queue, column.key) for column in Queue.__table__.columns}
        out.append(QueueOut(
            **columns,
            # A verified organizer posts as their organization ("Brgy. Poblacion Council")
            organizer_name=organizer.organization_name if verified else organizer.name,
            organizer_verified=verified,
            people_waiting=live.people_waiting,
            now_serving=live.now_serving,
            # For someone joining now: everyone waiting is ahead of them
            estimated_wait_minutes=estimator.wait_minutes(
                per_person[queue.id].minutes_per_person, live.people_waiting),
            minutes_per_person=round(per_person[queue.id].minutes_per_person, 2),
            service_sample_count=per_person[queue.id].samples,
        ))
    return out


def one_out(db: Session, queue: Queue) -> QueueOut:
    return to_out(db, [queue])[0]


# ---- Rules for a queue's fields ---------------------------------------------------

def invalid(field: str, message: str) -> ApiError:
    """INVALID_INPUT naming the field to fix, like the validation errors do."""
    return ApiError(422, "INVALID_INPUT", message, fields={field: message})


def check_fit(values: dict) -> None:
    """How a queue's fields fit together. Each field alone was already checked by QueueIn;
    the database's CHECKs back these up, but this gives a message the app can show."""
    if values["end_date"] < values["start_date"]:
        raise invalid("end_date", "The last day can't be before the first.")
    if values["closes_at"] <= values["opens_at"]:
        raise invalid("closes_at", "Closing time must be after opening time.")
    if values["proximity_check_enabled"]:
        if values["join_radius_meters"] not in RADII:
            raise invalid("join_radius_meters", "Pick 500 m, 1 km, 2 km or 5 km.")
    else:
        values["join_radius_meters"] = 0  # no check, no radius (MODELS.md)


def check_not_past(values: dict, now: datetime) -> None:
    if values["start_date"] < now.astimezone(timeutil.MANILA).date():
        raise invalid("start_date", "The first day can't be in the past.")
    end = values["end_date"]
    if now >= datetime.combine(end, values["closes_at"], timeutil.MANILA):
        raise invalid("closes_at", "That closing time has already passed.")


def has_live_queue(db: Session, user: User, except_id: str | None = None) -> bool:
    """Live means open or paused; an upcoming queue doesn't count (DECISIONS.md "Organizer
    verification lives on the organizer")."""
    query = select(Queue.id).where(Queue.organizer_id == user.id,
                                   Queue.status.in_((QueueStatus.OPEN, QueueStatus.PAUSED)))
    if except_id:
        query = query.where(Queue.id != except_id)
    return db.scalar(query.limit(1)) is not None


def one_live_queue_error() -> ApiError:
    return ApiError(409, "ONE_LIVE_QUEUE",
                    "Unverified organizers can run one live queue at a time. "
                    "Close or finish the other one first, or get verified.")


# ---- Reading --------------------------------------------------------------------

@router.get("/queues", response_model=list[QueueOut])
def browse(municipality: str | None = None, category: Category | None = None,
           q: str | None = None, db: Session = Depends(get_db)) -> list[QueueOut]:
    """Browse: upcoming, open and paused queues (never closed), filtered by town, category
    and a search over the name and venue."""
    schedule.refresh_all(db, timeutil.now())
    db.commit()

    query = (select(Queue)
             .where(Queue.status != QueueStatus.CLOSED)
             .options(selectinload(Queue.organizer))  # all organizers in one extra query
             .order_by(STATUS_ORDER, Queue.start_date, Queue.name))
    if municipality:
        query = query.where(Queue.municipality == municipality)
    if category:
        query = query.where(Queue.category == category)
    if q and q.strip():
        # ILIKE: case-insensitive match anywhere in the text
        pattern = f"%{q.strip()}%"
        query = query.where(or_(Queue.name.ilike(pattern), Queue.venue.ilike(pattern)))
    return to_out(db, list(db.scalars(query)))


@router.get("/queues/{queue_id}", response_model=QueueOut, responses=documented(404))
def detail(queue_id: str, db: Session = Depends(get_db)) -> QueueOut:
    """One queue, closed ones included (Queue history links to them)."""
    queue = queue_or_404(db, queue_id)
    schedule.refresh(db, queue, timeutil.now())
    db.commit()
    return one_out(db, queue)


@router.get("/me/queues", response_model=list[QueueOut], responses=documented(401, 403))
def my_queues(user: User = Depends(current_user), db: Session = Depends(get_db)) -> list[QueueOut]:
    """The organizer's own queues, every status: live first, then upcoming, then closed."""
    schedule.refresh_all(db, timeutil.now())
    db.commit()
    query = (select(Queue).where(Queue.organizer_id == user.id)
             .options(selectinload(Queue.organizer))
             .order_by(STATUS_ORDER, Queue.start_date.desc(), Queue.name))
    return to_out(db, list(db.scalars(query)))


@router.get("/queues/{queue_id}/stats", response_model=QueueStatsOut,
            responses=documented(401, 403, 404))
def stats(queue: Queue = Depends(owned_queue), db: Session = Depends(get_db)) -> QueueStatsOut:
    """The organizer's numbers for today (MODELS.md "QueueStats"), counted in one query.
    A PRANK removal counts as a no-show (MODELS.md "Rules the server enforces")."""
    now = timeutil.now()
    schedule.refresh(db, queue, now)
    db.commit()
    today = timeutil.today()
    served, no_shows, waiting = db.execute(
        select(func.count().filter(Ticket.status == TicketStatus.SERVED),
               func.count().filter(or_(Ticket.status == TicketStatus.NO_SHOW,
                                       Ticket.removal_reason == RemovalReason.PRANK)),
               func.count().filter(Ticket.status == TicketStatus.WAITING))
        .where(Ticket.queue_id == queue.id, Ticket.service_date == today)).one()
    per_person = estimator.estimate(db, queue.id, today)
    return QueueStatsOut(
        served_today=served,
        no_shows_today=no_shows,
        waiting_now=waiting,
        average_service_minutes=round(per_person.minutes_per_person, 1),
        service_sample_count=per_person.samples,
        projected_wait_minutes=estimator.wait_minutes(per_person.minutes_per_person, waiting),
        estimate_source=per_person.source,
        model_samples=0,  # phase 9: how many served people the learning model has seen
    )


# ---- Creating and editing -----------------------------------------------------------

@router.post("/queues", response_model=QueueOut, status_code=201,
             responses=documented(401, 403, 409, 422))
def create(body: QueueIn, user: User = Depends(current_user),
           db: Session = Depends(get_db)) -> QueueOut:
    now = timeutil.now()
    values = body.model_dump()
    check_fit(values)
    check_not_past(values, now)

    opens_now = now >= datetime.combine(values["start_date"], values["opens_at"], timeutil.MANILA)
    if (opens_now and user.verification_status != VerificationStatus.VERIFIED
            and has_live_queue(db, user)):
        raise one_live_queue_error()

    queue = Queue(**values, organizer_id=user.id,
                  status=QueueStatus.OPEN if opens_now else QueueStatus.UPCOMING)
    db.add(queue)
    db.commit()
    return one_out(db, queue)


@router.patch("/queues/{queue_id}", response_model=QueueOut,
              responses=documented(401, 403, 404, 409, 422))
def edit(body: QueuePatch, queue: Queue = Depends(owned_queue),
         db: Session = Depends(get_db)) -> QueueOut:
    """Only the fields sent change. A new first day can't be in the past; an old one can
    stay (the queue may have started already)."""
    now = timeutil.now()
    schedule.refresh(db, queue, now)
    if queue.status == QueueStatus.CLOSED:
        raise ApiError(409, "WRONG_STATUS", "A closed queue can't be edited.")

    changes = body.model_dump(exclude_unset=True)
    for field, value in changes.items():
        if value is None and field not in ("details", "bring"):
            raise invalid(field, "Fill this in.")
    values = {field: getattr(queue, field) for field in QueuePatch.model_fields}
    values.update(changes)
    check_fit(values)
    if "start_date" in changes and changes["start_date"] < now.astimezone(timeutil.MANILA).date():
        raise invalid("start_date", "The first day can't be in the past.")

    for field in QueuePatch.model_fields:
        setattr(queue, field, values[field])
    schedule.refresh(db, queue, now)
    db.commit()
    return one_out(db, queue)


# ---- Running a queue ----------------------------------------------------------------

@router.post("/queues/{queue_id}/pause", response_model=QueueOut,
             responses=documented(401, 403, 404, 409))
def pause(queue: Queue = Depends(owned_queue), db: Session = Depends(get_db)) -> QueueOut:
    """No new joins; people already in line keep their place ("No new joins since 10:05")."""
    now = timeutil.now()
    schedule.refresh(db, queue, now)
    if queue.status != QueueStatus.OPEN:
        raise ApiError(409, "WRONG_STATUS", "Only an open queue can be paused.")
    queue.status = QueueStatus.PAUSED
    queue.paused_at = now
    db.commit()
    return one_out(db, queue)


@router.post("/queues/{queue_id}/resume", response_model=QueueOut,
             responses=documented(401, 403, 404, 409))
def resume(queue: Queue = Depends(owned_queue), db: Session = Depends(get_db)) -> QueueOut:
    schedule.refresh(db, queue, timeutil.now())
    if queue.status != QueueStatus.PAUSED:
        raise ApiError(409, "WRONG_STATUS", "Only a paused queue can be resumed.")
    queue.status = QueueStatus.OPEN
    queue.paused_at = None
    db.commit()
    return one_out(db, queue)


@router.post("/queues/{queue_id}/close", response_model=QueueOut,
             responses=documented(401, 403, 404, 409))
def close(queue: Queue = Depends(owned_queue), db: Session = Depends(get_db)) -> QueueOut:
    """Closes it for good. Everyone still in line is released as QUEUE_CLOSED, which never
    counts as a no-show."""
    now = timeutil.now()
    schedule.refresh(db, queue, now)
    if queue.status == QueueStatus.CLOSED:
        raise ApiError(409, "WRONG_STATUS", "This queue is already closed.")
    schedule.close_now(db, queue, now)
    db.commit()
    return one_out(db, queue)


@router.post("/queues/{queue_id}/extend", response_model=QueueOut,
             responses=documented(401, 403, 404, 409, 422))
def extend(body: ExtendIn, queue: Queue = Depends(owned_queue),
           db: Session = Depends(get_db)) -> QueueOut:
    """A later closing time. The hours are the same every day of a run, so this changes them
    for the days left too (MODELS.md: "Extending closing time changes this")."""
    schedule.refresh(db, queue, timeutil.now())
    if queue.status not in (QueueStatus.OPEN, QueueStatus.PAUSED):
        raise ApiError(409, "WRONG_STATUS", "Only an open or paused queue can be extended.")
    if body.closes_at <= queue.closes_at:
        raise invalid("closes_at", "Pick a time after the current closing time.")
    queue.closes_at = body.closes_at
    db.commit()
    return one_out(db, queue)

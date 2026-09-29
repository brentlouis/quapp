"""The queuer's side: joining, my tickets, one ticket, leaving, "I'm here" and "I need more
time" (BACKEND.md phases 4 and 5).

The rules are in services/tickets.py; this file turns requests into calls to it, and tickets
into JSON.
"""

from fastapi import APIRouter, Depends, Response
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app import timeutil
from app.database import get_db
from app.deps import current_user
from app.enums import TicketStatus
from app.errors import ApiError, documented
from app.models import Ticket, User
from app.schemas import CooldownOut, JoinIn, MoveBackIn, TicketOut
from app.services import estimator, schedule
from app.services import tickets as rules

router = APIRouter()


def mask_phone(phone: str | None) -> str | None:
    """"09171234567" → "0917 ••• 4567": enough for an organizer to tell two Marias apart,
    not enough to call them (MODELS.md "Ticket", holder_phone)."""
    if not phone:
        return phone
    return f"{phone[:4]} ••• {phone[-4:]}"


def tickets_out(db: Session, tickets: list[Ticket], positions: dict[str, int] | None = None,
                masked: bool = False) -> list[TicketOut]:
    """Ticket rows → TicketOut. Positions are counted unless the caller already knows them
    (the console's line is in order already). `masked` hides phones, for organizers."""
    places = positions if positions is not None else rules.positions(db, tickets)
    per_person = estimator.estimates(db, list({t.queue_id for t in tickets}), timeutil.today())
    out = []
    for ticket in tickets:
        position = places[ticket.id]
        # The people ahead are position − 1; nobody is ahead once called or finished
        wait = (estimator.wait_minutes(per_person[ticket.queue_id].minutes_per_person, position - 1)
                if position > 0 else 0)
        columns = {column.key: getattr(ticket, column.key) for column in Ticket.__table__.columns}
        if masked:
            columns["holder_phone"] = mask_phone(ticket.holder_phone)
        out.append(TicketOut(
            **columns,
            # Copied from the queue, so lists don't need the queue (MODELS.md)
            queue_name=ticket.queue.name,
            venue=ticket.queue.venue,
            position=position,
            estimated_wait_minutes=wait,
        ))
    return out


def one_out(db: Session, ticket: Ticket) -> TicketOut:
    return tickets_out(db, [ticket])[0]


def my_ticket(db: Session, ticket_id: str, user: User) -> Ticket:
    """One of the caller's tickets. Someone else's is "not found", not "forbidden", so ticket
    ids can't be probed."""
    ticket = db.get(Ticket, ticket_id)
    if ticket is None or ticket.user_id != user.id:
        raise ApiError(404, "TICKET_NOT_FOUND", "That ticket doesn't exist.")
    return ticket


@router.post("/queues/{queue_id}/tickets", response_model=TicketOut, status_code=201,
             responses=documented(401, 403, 404, 409, 422))
def join(queue_id: str, body: JoinIn | None = None, user: User = Depends(current_user),
         db: Session = Depends(get_db)) -> TicketOut:
    body = body or JoinIn()
    ticket = rules.join(db, queue_id, user, body.latitude, body.longitude, timeutil.now(),
                        body.holder_name, body.holder_phone)
    return one_out(db, ticket)


@router.get("/me/tickets", response_model=list[TicketOut], responses=documented(401, 403))
def my_tickets(live: bool = True, user: User = Depends(current_user),
               db: Session = Depends(get_db)) -> list[TicketOut]:
    """live=true: tickets you're in line with (waiting or called), soonest day first.
    live=false: Queue history, newest first."""
    now = timeutil.now()
    mine = select(Ticket).where(Ticket.user_id == user.id).options(selectinload(Ticket.queue))
    # Bring the queues these tickets belong to up to date first, so a line that ended at
    # 5 PM isn't still listed as live at 6.
    schedule.refresh_many(db, list({t.queue for t in db.scalars(
        mine.where(Ticket.status.in_(rules.LIVE)))}), now)
    db.commit()

    if live:
        query = mine.where(Ticket.status.in_(rules.LIVE)).order_by(
            Ticket.service_date, Ticket.joined_at)
    else:
        query = mine.where(Ticket.status.not_in(rules.LIVE)).order_by(
            Ticket.finished_at.desc().nulls_last(), Ticket.joined_at.desc())
    return tickets_out(db, list(db.scalars(query)))


@router.get("/me/cooldown", response_model=CooldownOut, responses=documented(401, 403))
def my_cooldown(user: User = Depends(current_user), db: Session = Depends(get_db)) -> CooldownOut:
    """Where the no-show penalty stands, so the app can say so before a join is refused."""
    until, strikes = rules.cooldown_state(db, user, timeutil.now())
    return CooldownOut(until=until, strikes=strikes, strike_limit=rules.STRIKES_FOR_COOLDOWN,
                       duration_minutes=int(rules.COOLDOWN.total_seconds() // 60))


@router.get("/tickets/{ticket_id}", response_model=TicketOut, responses=documented(401, 403, 404))
def one_ticket(ticket_id: str, user: User = Depends(current_user),
               db: Session = Depends(get_db)) -> TicketOut:
    """Active ticket polls this for its position, wait and status."""
    ticket = my_ticket(db, ticket_id, user)
    schedule.refresh(db, ticket.queue, timeutil.now())
    db.commit()
    return one_out(db, ticket)


@router.delete("/tickets/{ticket_id}", status_code=204,
               responses=documented(401, 403, 404, 409))
def leave(ticket_id: str, user: User = Depends(current_user),
          db: Session = Depends(get_db)) -> Response:
    """Leave the queue: the ticket is simply gone. Not a no-show, and not in history
    (FakeData.leave). Its number is never given to anyone else."""
    ticket = my_ticket(db, ticket_id, user)
    if ticket.status not in (TicketStatus.WAITING, TicketStatus.CALLED):
        raise ApiError(409, "WRONG_STATUS", "This ticket has already finished.")
    db.delete(ticket)
    db.commit()
    return Response(status_code=204)


@router.post("/tickets/{ticket_id}/here", response_model=TicketOut,
             responses=documented(401, 403, 404, 409))
def im_here(ticket_id: str, user: User = Depends(current_user),
            db: Session = Depends(get_db)) -> TicketOut:
    ticket = rules.here(db, my_ticket(db, ticket_id, user), timeutil.now())
    return one_out(db, ticket)


@router.post("/tickets/{ticket_id}/move-back", response_model=TicketOut,
             responses=documented(401, 403, 404, 409, 422))
def move_back(ticket_id: str, body: MoveBackIn, dry_run: bool = False,
              user: User = Depends(current_user), db: Session = Depends(get_db)) -> TicketOut:
    """"I need more time". dry_run=true answers with where the ticket would land, without
    saving: the sheet's preview before the queuer confirms."""
    ticket = my_ticket(db, ticket_id, user)
    if dry_run:
        # A savepoint: make the move, read the result, undo it. The queue's lock goes when
        # the request's session closes (database.get_db).
        savepoint = db.begin_nested()
        rules.move_back(db, ticket, body.minutes_needed, timeutil.now())
        preview = one_out(db, ticket)
        savepoint.rollback()
        return preview
    rules.move_back(db, ticket, body.minutes_needed, timeutil.now())
    db.commit()
    return one_out(db, ticket)

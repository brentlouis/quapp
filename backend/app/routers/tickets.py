"""The queuer's side: joining, my tickets, one ticket, leaving (BACKEND.md phase 4).

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
from app.schemas import JoinIn, TicketOut
from app.services import schedule
from app.services import tickets as rules

router = APIRouter()


def to_out(db: Session, tickets: list[Ticket]) -> list[TicketOut]:
    places = rules.positions(db, tickets)
    out = []
    for ticket in tickets:
        position = places[ticket.id]
        columns = {column.key: getattr(ticket, column.key) for column in Ticket.__table__.columns}
        out.append(TicketOut(
            **columns,
            # Copied from the queue, so lists don't need the queue (MODELS.md)
            queue_name=ticket.queue.name,
            venue=ticket.queue.venue,
            position=position,
            estimated_wait_minutes=rules.wait_for(ticket, position),
        ))
    return out


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
    ticket = rules.join(db, queue_id, user, body.latitude, body.longitude, timeutil.now())
    return to_out(db, [ticket])[0]


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
    return to_out(db, list(db.scalars(query)))


@router.get("/tickets/{ticket_id}", response_model=TicketOut, responses=documented(401, 403, 404))
def one_ticket(ticket_id: str, user: User = Depends(current_user),
               db: Session = Depends(get_db)) -> TicketOut:
    """Active ticket polls this for its position, wait and status."""
    ticket = my_ticket(db, ticket_id, user)
    schedule.refresh(db, ticket.queue, timeutil.now())
    db.commit()
    return to_out(db, [ticket])[0]


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

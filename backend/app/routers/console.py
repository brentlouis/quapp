"""The organizer's Live console (BACKEND.md phase 5; MODELS.md "Line"). Every endpoint here
is organizer-only: owned_queue refuses anyone else with NOT_OWNER.

The rules are in services/console.py.
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app import timeutil
from app.database import get_db
from app.deps import current_user, owned_queue
from app.errors import ApiError, documented
from app.models import Queue, Ticket, User
from app.routers.tickets import one_out, tickets_out
from app.schemas import LineOut, RemoveIn, TicketOut, TicketRemovalOut, WalkInIn
from app.services import console, schedule

router = APIRouter()


def line_out(db: Session, queue: Queue) -> LineOut:
    line = console.line(db, queue, timeutil.now())
    # The line is in call order already, so the positions are just 1, 2, 3, …
    positions = {ticket.id: index + 1 for index, ticket in enumerate(line.waiting)}
    serving = line.now_serving
    if serving is not None:
        positions[serving.id] = 0
    everyone = ([serving] if serving else []) + line.waiting
    out = tickets_out(db, everyone, positions, masked=True)
    return LineOut(
        now_serving=out[0] if serving else None,
        now_serving_here_at=serving.here_at if serving else None,
        now_serving_timed_out=line.now_serving_timed_out,
        waiting=out[1:] if serving else out,
    )


@router.get("/queues/{queue_id}/line", response_model=LineOut,
            responses=documented(401, 403, 404))
def get_line(queue: Queue = Depends(owned_queue), db: Session = Depends(get_db)) -> LineOut:
    """The console polls this: who's at the counter, whether they confirmed or timed out,
    and today's line in call order."""
    schedule.refresh(db, queue, timeutil.now())
    db.commit()
    return line_out(db, queue)


@router.post("/queues/{queue_id}/call-next", response_model=LineOut,
             responses=documented(401, 403, 404, 409))
def call_next(queue: Queue = Depends(owned_queue), db: Session = Depends(get_db)) -> LineOut:
    return line_out(db, console.call_next(db, queue.id, timeutil.now()))


@router.post("/queues/{queue_id}/no-show", response_model=LineOut,
             responses=documented(401, 403, 404, 409))
def no_show(queue: Queue = Depends(owned_queue), db: Session = Depends(get_db)) -> LineOut:
    return line_out(db, console.no_show(db, queue.id, timeutil.now()))


@router.post("/queues/{queue_id}/walk-ins", response_model=TicketOut, status_code=201,
             responses=documented(401, 403, 404, 409, 422))
def walk_in(body: WalkInIn, queue: Queue = Depends(owned_queue),
            db: Session = Depends(get_db)) -> TicketOut:
    return one_out(db, console.walk_in(db, queue.id, body.name, timeutil.now()))


@router.post("/tickets/{ticket_id}/remove", response_model=TicketRemovalOut,
             responses=documented(401, 403, 404, 409, 422))
def remove(ticket_id: str, body: RemoveIn, user: User = Depends(current_user),
           db: Session = Depends(get_db)) -> TicketRemovalOut:
    """Take someone out of the line, with a reason. Only the queue's organizer can."""
    ticket = db.get(Ticket, ticket_id)
    if ticket is None:
        raise ApiError(404, "TICKET_NOT_FOUND", "That ticket doesn't exist.")
    if ticket.queue.organizer_id != user.id:
        raise ApiError(403, "NOT_OWNER", "Only the queue's organizer can do that.")
    removal = console.remove(db, ticket, body.reason, user, timeutil.now())
    return TicketRemovalOut.model_validate(removal)

"""A queue's live numbers, counted from its tickets (nothing here is stored; MODELS.md
"Database": what isn't stored)."""

from dataclasses import dataclass
from datetime import date

from sqlalchemy import func, select
from sqlalchemy.dialects.postgresql import distinct_on
from sqlalchemy.orm import Session

from app.enums import TicketStatus
from app.models import Ticket


@dataclass
class LiveNumbers:
    people_waiting: int = 0
    # The number of the ticket called most recently today; None before the first call
    now_serving: int | None = None


def live_numbers(db: Session, queue_ids: list[str], today: date) -> dict[str, LiveNumbers]:
    """The numbers for many queues in two queries, however many queues there are. Counting
    queue by queue would run two queries per card on Browse (the "N+1 queries" problem)."""
    numbers = {queue_id: LiveNumbers() for queue_id in queue_ids}
    if not queue_ids:
        return numbers

    waiting = db.execute(
        select(Ticket.queue_id, func.count())
        .where(Ticket.queue_id.in_(queue_ids), Ticket.status == TicketStatus.WAITING)
        .group_by(Ticket.queue_id))
    for queue_id, count in waiting:
        numbers[queue_id].people_waiting = count

    # DISTINCT ON (queue_id), a Postgres feature: one row per queue, the first in the ORDER BY,
    # which is the latest call.
    serving = db.execute(
        select(Ticket.queue_id, Ticket.ticket_number)
        .where(Ticket.queue_id.in_(queue_ids), Ticket.called_at.is_not(None),
               Ticket.service_date == today)
        .order_by(Ticket.queue_id, Ticket.called_at.desc())
        .ext(distinct_on(Ticket.queue_id)))
    for queue_id, number in serving:
        numbers[queue_id].now_serving = number

    return numbers

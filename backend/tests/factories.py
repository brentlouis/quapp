"""Quick ways to make rows in tests. Each has sensible defaults, so a test only spells out the
fields its rule is about:

    queue = make_queue(db, organizer, proximity_check_enabled=True, join_radius_meters=1000)
"""

from datetime import time
from itertools import count

from sqlalchemy.orm import Session

from app import timeutil
from app.enums import Category, QueueStatus, TicketStatus
from app.models import Queue, Ticket, User

_phones = count(1)


def make_user(db: Session, **fields) -> User:
    """A user with a unique phone (09170000001, 09170000002, …)."""
    values = dict(
        name="Maria Santos",
        phone=f"0917{next(_phones):07d}",
        password_hash="not-a-real-hash",
        device_install_id="test-device",
    )
    values.update(fields)
    user = User(**values)
    db.add(user)
    db.flush()
    return user


def make_queue(db: Session, organizer: User, **fields) -> Queue:
    """An OPEN one-day queue today, 8 AM to 5 PM, in Tagbilaran, with no checks on."""
    values = dict(
        organizer_id=organizer.id,
        name="Barangay Relief Distribution",
        category=Category.RELIEF,
        short_description="Family food packs for registered households",
        venue="Brgy. Poblacion Hall",
        municipality="Tagbilaran City",
        latitude=9.6496,
        longitude=123.8547,
        start_date=timeutil.today(),
        end_date=timeutil.today(),
        opens_at=time(8, 0),
        closes_at=time(17, 0),
        status=QueueStatus.OPEN,
    )
    values.update(fields)
    queue = Queue(**values)
    db.add(queue)
    db.flush()
    return queue


def make_ticket(db: Session, queue: Queue, user: User | None = None, **fields) -> Ticket:
    """The next ticket in the queue: takes queue.next_ticket_number like joining will.
    No user makes it a walk-in."""
    number = queue.next_ticket_number
    queue.next_ticket_number += 1
    values = dict(
        queue_id=queue.id,
        user_id=user.id if user else None,
        holder_name=user.name if user else "Walk-in",
        holder_phone=user.phone if user else None,
        walk_in=user is None,
        ticket_number=number,
        line_order=float(number),
        service_date=queue.start_date if queue.start_date > timeutil.today() else timeutil.today(),
        status=TicketStatus.WAITING,
    )
    values.update(fields)
    ticket = Ticket(**values)
    db.add(ticket)
    db.flush()
    return ticket


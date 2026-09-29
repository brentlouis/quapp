"""The demo data loads, obeys every database rule, and has what the demo relies on."""

from sqlalchemy import func, select

from app.enums import QueueStatus, TicketStatus, UserStatus
from app.models import Queue, Ticket, User
from seed import DEMO_PASSWORD, seed
from app.security import check_password


def test_the_demo_seeds(db):
    seed(db)

    assert db.scalar(select(func.count()).select_from(Queue)) == 12
    statuses = dict(db.execute(select(Queue.id, Queue.status)).all())
    assert statuses["q5"] == QueueStatus.CLOSED
    assert statuses["q8"] == QueueStatus.PAUSED
    assert statuses["q6"] == QueueStatus.UPCOMING

    waiting_in_q1 = db.scalar(select(func.count()).select_from(Ticket).where(
        Ticket.queue_id == "q1", Ticket.status == TicketStatus.WAITING))
    assert waiting_in_q1 == 42

    rhea = db.scalar(select(User).where(User.phone == "09175550002"))
    assert {q.id for q in db.scalars(select(Queue).where(Queue.organizer_id == rhea.id))} \
        == {"q1", "q3", "q5"}
    assert check_password(DEMO_PASSWORD, rhea.password_hash)

    jun = db.scalar(select(User).where(User.phone == "09180000000"))
    assert jun.status == UserStatus.SUSPENDED

    maria = db.scalar(select(User).where(User.phone == "09175550001"))
    history = db.scalar(select(func.count()).select_from(Ticket).where(Ticket.user_id == maria.id))
    assert history == 4

"""The database's own rules (BACKEND.md step 1.3): each test tries to save something the
contract forbids and expects Postgres to refuse it, whatever the endpoint code does."""

from datetime import time

import pytest
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError

from app.enums import RemovalReason, TicketStatus, VerificationStatus
from app.models import Report, User
from tests.factories import make_queue, make_ticket, make_user


def refused(db, action=None):
    """Asserts the database refuses whatever `action` saves (or what's pending), then clears it."""
    with pytest.raises(IntegrityError):
        if action:
            action()
        db.flush()
    db.rollback()


def test_a_user_round_trips_with_defaults(db):
    user = make_user(db, name="Rhea Cruz")
    db.expire_all()
    loaded = db.get(User, user.id)
    assert loaded.name == "Rhea Cruz"
    assert loaded.verification_status == VerificationStatus.NONE
    assert loaded.is_admin is False
    assert loaded.created_at.tzinfo is not None  # TIMESTAMPTZ comes back timezone-aware


def test_phones_are_unique(db):
    make_user(db, phone="09171234567")
    refused(db, lambda: make_user(db, phone="09171234567"))


def test_short_description_is_at_most_50_characters(db):
    organizer = make_user(db)
    make_queue(db, organizer, short_description="x" * 50)
    refused(db, lambda: make_queue(db, organizer, short_description="x" * 51))


def test_radius_is_one_of_the_allowed_values(db):
    organizer = make_user(db)
    refused(db, lambda: make_queue(db, organizer, proximity_check_enabled=True,
                                   join_radius_meters=300))


def test_radius_is_zero_exactly_when_proximity_is_off(db):
    organizer = make_user(db)
    make_queue(db, organizer, proximity_check_enabled=True, join_radius_meters=1000)
    refused(db, lambda: make_queue(db, organizer, proximity_check_enabled=False,
                                   join_radius_meters=1000))


def test_closing_time_is_after_opening_time(db):
    organizer = make_user(db)
    refused(db, lambda: make_queue(db, organizer, opens_at=time(17), closes_at=time(8)))


def test_an_unknown_status_is_refused_by_the_database_itself(db):
    """The ORM would catch a typo first; raw SQL shows the CHECK is there too."""
    queue = make_queue(db, make_user(db))
    refused(db, lambda: db.execute(
        text("UPDATE queues SET status = 'OPNE' WHERE id = :id"), {"id": queue.id}))


def test_ticket_numbers_are_unique_within_a_queue(db):
    queue = make_queue(db, make_user(db))
    make_ticket(db, queue, make_user(db), ticket_number=7)
    refused(db, lambda: make_ticket(db, queue, make_user(db), ticket_number=7))


def test_one_live_ticket_per_person_per_queue(db):
    queue = make_queue(db, make_user(db))
    maria = make_user(db)
    make_ticket(db, queue, maria)
    refused(db, lambda: make_ticket(db, queue, maria))


def test_an_old_ticket_doesnt_stop_joining_again(db):
    queue = make_queue(db, make_user(db))
    maria = make_user(db)
    make_ticket(db, queue, maria, status=TicketStatus.SERVED)
    make_ticket(db, queue, maria)  # WAITING next to a SERVED one is fine


def test_walk_ins_have_no_account_and_others_do(db):
    queue = make_queue(db, make_user(db))
    make_ticket(db, queue)  # a walk-in
    refused(db, lambda: make_ticket(db, queue, walk_in=True, user_id=make_user(db).id))


def test_a_removed_ticket_needs_a_reason_and_only_then(db):
    queue = make_queue(db, make_user(db))
    make_ticket(db, queue, make_user(db), status=TicketStatus.REMOVED,
                removal_reason=RemovalReason.PRANK)
    refused(db, lambda: make_ticket(db, queue, make_user(db), status=TicketStatus.REMOVED))
    refused(db, lambda: make_ticket(db, queue, make_user(db),
                                    removal_reason=RemovalReason.DUPLICATE))


def test_one_report_per_person_per_queue(db):
    queue = make_queue(db, make_user(db))
    reporter = make_user(db)
    db.add(Report(queue_id=queue.id, reporter_id=reporter.id, reason="FAKE"))
    db.flush()
    refused(db, lambda: db.add(Report(queue_id=queue.id, reporter_id=reporter.id,
                                      reason="OTHER")))

"""Joining, my tickets, one ticket, leaving (BACKEND.md steps 4.1 and 4.2). The clock is fixed at
10:00 AM on 2026-09-29, and make_queue's default is an OPEN queue that day, 8 AM to 5 PM."""

from concurrent.futures import ThreadPoolExecutor
from datetime import time, timedelta

import pytest
from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.enums import QueueStatus, RemovalReason, TicketStatus
from app.models import Queue, Ticket, Token, User
from app.services import tickets as rules
from tests.factories import make_queue, make_ticket, make_user, sign_in

VENUE = (9.6496, 123.8547)  # make_queue's default venue


def join(client, queue, headers, **location):
    return client.post(f"/queues/{queue.id}/tickets", json=location or None, headers=headers)


# ---- Joining ------------------------------------------------------------------

def test_joining_puts_you_at_the_back_of_the_line(client, db):
    queue = make_queue(db, make_user(db))
    first = join(client, queue, sign_in(db, make_user(db)))
    assert first.status_code == 201
    assert first.json()["ticket_number"] == 1
    assert first.json()["position"] == 1
    assert first.json()["estimated_wait_minutes"] == 0

    second = join(client, queue, sign_in(db, make_user(db))).json()
    assert second["ticket_number"] == 2
    assert second["position"] == 2
    assert second["estimated_wait_minutes"] == 5  # one ahead × the default 5 min
    assert second["queue_name"] == "Barangay Relief Distribution"


def test_joining_for_someone_else_puts_their_name_on_the_ticket(client, db):
    queue = make_queue(db, make_user(db))
    body = join(client, queue, sign_in(db, make_user(db, name="Maria Santos")),
                holder_name="Lola Nena", holder_phone="0917 555 0101").json()
    assert body["holder_name"] == "Lola Nena"
    assert body["holder_phone"] == "09175550101"


def test_joining_early_is_for_the_first_day(client, db, clock):
    tomorrow = clock.today() + timedelta(days=1)
    queue = make_queue(db, make_user(db), status=QueueStatus.UPCOMING,
                       start_date=tomorrow, end_date=tomorrow)
    ticket_id = join(client, queue, sign_in(db, make_user(db))).json()["id"]
    assert db.get(Ticket, ticket_id).service_date == tomorrow


def test_a_paused_queue_takes_no_joins(client, db):
    queue = make_queue(db, make_user(db), status=QueueStatus.PAUSED)
    assert join(client, queue, sign_in(db, make_user(db))).json()["error"] == "QUEUE_PAUSED"


def test_a_closed_queue_takes_no_joins(client, db):
    queue = make_queue(db, make_user(db), status=QueueStatus.CLOSED)
    assert join(client, queue, sign_in(db, make_user(db))).json()["error"] == "QUEUE_NOT_OPEN"


def test_a_multi_day_queue_takes_no_joins_after_todays_closing(client, db, clock):
    queue = make_queue(db, make_user(db), end_date=clock.today() + timedelta(days=1))
    clock.set(time(18, 0))
    assert join(client, queue, sign_in(db, make_user(db))).json()["error"] == "QUEUE_NOT_OPEN"


def test_an_unknown_queue(client, db):
    response = client.post("/queues/nope/tickets", headers=sign_in(db, make_user(db)))
    assert response.status_code == 404


def test_one_live_ticket_per_queue(client, db):
    queue = make_queue(db, make_user(db))
    headers = sign_in(db, make_user(db))
    first = join(client, queue, headers).json()
    again = join(client, queue, headers)
    assert again.status_code == 409
    assert again.json()["error"] == "ALREADY_IN_LINE"
    assert again.json()["ticket_id"] == first["id"]


def test_joining_again_after_a_missed_call_settles_it_as_a_no_show(client, db, clock):
    queue = make_queue(db, make_user(db), grace_period_enabled=True)
    maria = make_user(db)
    headers = sign_in(db, maria)
    called = make_ticket(db, queue, maria, status=TicketStatus.CALLED,
                         called_at=clock.now() - timedelta(minutes=2))

    # Still inside the 3 minutes: that ticket is the one in line
    assert join(client, queue, headers).json()["error"] == "ALREADY_IN_LINE"

    clock.set(clock.now() + timedelta(minutes=1))  # 3 minutes since the call
    assert join(client, queue, headers).status_code == 201
    db.refresh(called)
    assert called.status == TicketStatus.NO_SHOW
    assert called.finished_at == clock.now()


# ---- Cooldown -----------------------------------------------------------------

def strike(db, user, clock, minutes_ago, **fields):
    """A finished ticket on a penalty queue: a no-show, unless fields say otherwise."""
    queue = make_queue(db, make_user(db), no_show_cooldown_enabled=True)
    values = dict(status=TicketStatus.NO_SHOW,
                  finished_at=clock.now() - timedelta(minutes=minutes_ago))
    values.update(fields)
    make_ticket(db, queue, user, **values)


def test_two_no_shows_start_a_cooldown_on_penalty_queues(client, db, clock):
    maria = make_user(db)
    strike(db, maria, clock, 10)
    strike(db, maria, clock, 5)
    headers = sign_in(db, maria)

    blocked = join(client, make_queue(db, make_user(db), no_show_cooldown_enabled=True), headers)
    assert blocked.status_code == 409
    assert blocked.json()["error"] == "COOLDOWN"
    assert blocked.json()["until"] == "2026-09-29T10:25:00+08:00"  # 30 min after the 2nd

    # A queue without the penalty ignores it
    assert join(client, make_queue(db, make_user(db)), headers).status_code == 201


def test_the_cooldown_runs_out(client, db, clock):
    maria = make_user(db)
    strike(db, maria, clock, 40)
    strike(db, maria, clock, 35)  # cooldown until 5 minutes ago
    queue = make_queue(db, make_user(db), no_show_cooldown_enabled=True)
    assert join(client, queue, sign_in(db, maria)).status_code == 201


def test_the_count_starts_again_after_a_cooldown(client, db, clock):
    maria = make_user(db)
    strike(db, maria, clock, 50)
    strike(db, maria, clock, 45)  # cooldown, over by now
    strike(db, maria, clock, 5)   # one new strike alone isn't a cooldown
    queue = make_queue(db, make_user(db), no_show_cooldown_enabled=True)
    assert join(client, queue, sign_in(db, maria)).status_code == 201


def test_the_app_can_read_where_the_cooldown_stands(client, db, clock):
    maria = make_user(db)
    headers = sign_in(db, maria)
    assert client.get("/me/cooldown", headers=headers).json() == {
        "until": None, "strikes": 0, "strike_limit": 2, "duration_minutes": 30}
    strike(db, maria, clock, 10)
    assert client.get("/me/cooldown", headers=headers).json()["strikes"] == 1
    strike(db, maria, clock, 5)
    body = client.get("/me/cooldown", headers=headers).json()
    assert body["until"] == "2026-09-29T10:25:00+08:00"
    assert body["strikes"] == 0  # the count starts again after a cooldown


def test_a_prank_removal_is_a_strike_and_a_closed_queue_is_not(client, db, clock):
    maria = make_user(db)
    strike(db, maria, clock, 10, status=TicketStatus.REMOVED, removal_reason=RemovalReason.PRANK)
    strike(db, maria, clock, 8, status=TicketStatus.QUEUE_CLOSED)  # not a strike
    strike(db, maria, clock, 5)
    queue = make_queue(db, make_user(db), no_show_cooldown_enabled=True)
    assert join(client, queue, sign_in(db, maria)).json()["error"] == "COOLDOWN"


# ---- Overlap ------------------------------------------------------------------

def test_two_lines_at_the_same_time_are_refused(client, db):
    maria = make_user(db)
    morning = make_queue(db, make_user(db), name="Clinic", opens_at=time(8), closes_at=time(12))
    make_ticket(db, morning, maria)
    overlapping = make_queue(db, make_user(db), opens_at=time(11), closes_at=time(15))
    response = join(client, overlapping, sign_in(db, maria))
    assert response.status_code == 409
    assert response.json()["error"] == "HOURS_OVERLAP"
    assert response.json()["other_queue_name"] == "Clinic"


def test_touching_ends_dont_overlap(client, db):
    maria = make_user(db)
    make_ticket(db, make_queue(db, make_user(db), opens_at=time(8), closes_at=time(12)), maria)
    afternoon = make_queue(db, make_user(db), opens_at=time(12), closes_at=time(17))
    assert join(client, afternoon, sign_in(db, maria)).status_code == 201


def test_the_same_hours_on_different_days_dont_overlap(client, db, clock):
    maria = make_user(db)
    make_ticket(db, make_queue(db, make_user(db)), maria)
    tomorrow = clock.today() + timedelta(days=1)
    upcoming = make_queue(db, make_user(db), status=QueueStatus.UPCOMING,
                          start_date=tomorrow, end_date=tomorrow)
    assert join(client, upcoming, sign_in(db, maria)).status_code == 201


def test_a_finished_ticket_doesnt_block_anything(client, db):
    maria = make_user(db)
    make_ticket(db, make_queue(db, make_user(db)), maria, status=TicketStatus.SERVED)
    assert join(client, make_queue(db, make_user(db)), sign_in(db, maria)).status_code == 201


# ---- Distance -----------------------------------------------------------------

def near_venue(north_degrees: float) -> dict:
    """A point due north of the venue. 0.009° of latitude is about 1 km."""
    return {"latitude": VENUE[0] + north_degrees, "longitude": VENUE[1]}


def test_distance_is_measured_like_a_map(db):
    assert rules.distance_meters(*VENUE, VENUE[0] + 0.009, VENUE[1]) == pytest.approx(1000.8, abs=1)


def test_the_radius_check(client, db):
    queue = make_queue(db, make_user(db), proximity_check_enabled=True, join_radius_meters=1000)

    missing = join(client, queue, sign_in(db, make_user(db)))
    assert missing.status_code == 422
    assert missing.json()["error"] == "LOCATION_NEEDED"

    far = join(client, queue, sign_in(db, make_user(db)), **near_venue(0.011))  # ~1.2 km
    assert far.status_code == 409
    assert far.json()["error"] == "TOO_FAR"
    assert far.json()["distance_meters"] == pytest.approx(1223, abs=2)

    inside = join(client, queue, sign_in(db, make_user(db)), **near_venue(0.008))  # ~890 m
    assert inside.status_code == 201


def test_the_edge_of_the_radius_is_forgiving(client, db):
    """1005 m on a 1 km radius: the app's measure could say 1000, so let it through."""
    queue = make_queue(db, make_user(db), proximity_check_enabled=True, join_radius_meters=1000)
    response = join(client, queue, sign_in(db, make_user(db)), **near_venue(0.00904))
    assert response.status_code == 201


# ---- Numbering under load -----------------------------------------------------

def test_twenty_people_joining_at_once_get_twenty_numbers(engine, clock):
    """Real concurrent transactions, so this test commits for real on the test database and
    cleans up after itself instead of using the rollback fixture."""
    with Session(engine) as db:
        organizer = make_user(db)
        queue = make_queue(db, organizer)
        people = [make_user(db) for _ in range(20)]
        db.commit()
        queue_id, people_ids = queue.id, [person.id for person in people]
        everyone = [organizer.id] + people_ids

    def join_as(user_id: str) -> int:
        with Session(engine) as session:
            user = session.get(User, user_id)
            return rules.join(session, queue_id, user, None, None, clock.now()).ticket_number

    try:
        with ThreadPoolExecutor(max_workers=20) as pool:
            numbers = list(pool.map(join_as, people_ids))
        assert sorted(numbers) == list(range(1, 21))
    finally:
        with Session(engine) as db:
            db.execute(delete(Ticket).where(Ticket.queue_id == queue_id))
            db.execute(delete(Queue).where(Queue.id == queue_id))
            db.execute(delete(Token).where(Token.user_id.in_(everyone)))
            db.execute(delete(User).where(User.id.in_(everyone)))
            db.commit()


# ---- My tickets, one ticket, leaving ------------------------------------------

def test_my_tickets_live_and_history(client, db, clock):
    maria = make_user(db)
    make_ticket(db, make_queue(db, make_user(db), name="In line"), maria)
    make_ticket(db, make_queue(db, make_user(db), name="Served"), maria,
                status=TicketStatus.SERVED, finished_at=clock.now() - timedelta(days=2))
    make_ticket(db, make_queue(db, make_user(db), name="Missed"), maria,
                status=TicketStatus.NO_SHOW, finished_at=clock.now() - timedelta(days=1))
    headers = sign_in(db, maria)

    live = client.get("/me/tickets", headers=headers).json()
    assert [t["queue_name"] for t in live] == ["In line"]
    history = client.get("/me/tickets", params={"live": "false"}, headers=headers).json()
    assert [t["queue_name"] for t in history] == ["Missed", "Served"]  # newest first
    assert history[0]["position"] == 0


def test_a_line_that_ended_moves_to_history(client, db, clock):
    maria = make_user(db)
    make_ticket(db, make_queue(db, make_user(db)), maria)
    headers = sign_in(db, maria)
    clock.set(time(17, 1))
    assert client.get("/me/tickets", headers=headers).json() == []
    history = client.get("/me/tickets", params={"live": "false"}, headers=headers).json()
    assert history[0]["status"] == "QUEUE_CLOSED"


def test_positions_move_up_when_someone_leaves(client, db):
    queue = make_queue(db, make_user(db))
    people = [make_user(db) for _ in range(3)]
    tickets = [join(client, queue, sign_in(db, person)).json() for person in people]
    third_headers = {"Authorization": f"Bearer token-{people[2].id}"}
    assert client.get(f"/tickets/{tickets[2]['id']}", headers=third_headers).json()["position"] == 3

    middle_headers = {"Authorization": f"Bearer token-{people[1].id}"}
    assert client.delete(f"/tickets/{tickets[1]['id']}", headers=middle_headers).status_code == 204

    assert client.get(f"/tickets/{tickets[2]['id']}", headers=third_headers).json()["position"] == 2
    assert db.scalar(select(Ticket).where(Ticket.id == tickets[1]["id"])) is None  # gone


def test_someone_elses_ticket_is_not_found(client, db):
    ticket = make_ticket(db, make_queue(db, make_user(db)), make_user(db))
    headers = sign_in(db, make_user(db))
    assert client.get(f"/tickets/{ticket.id}", headers=headers).status_code == 404
    assert client.delete(f"/tickets/{ticket.id}", headers=headers).status_code == 404


def test_a_finished_ticket_cant_be_left(client, db):
    maria = make_user(db)
    ticket = make_ticket(db, make_queue(db, make_user(db)), maria, status=TicketStatus.SERVED)
    response = client.delete(f"/tickets/{ticket.id}", headers=sign_in(db, maria))
    assert response.status_code == 409

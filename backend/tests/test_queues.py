"""Queue endpoints (BACKEND.md steps 3.1 and 3.3). The clock is fixed at 10:00 AM on
2026-09-29 (conftest.py), and make_queue's default is an OPEN queue that day, 8 AM to 5 PM."""

from datetime import timedelta

from sqlalchemy import event

from app.enums import Category, QueueStatus, TicketStatus, VerificationStatus
from tests.conftest import Clock
from tests.factories import make_queue, make_ticket, make_user, sign_in


def db_day(days_from_test_day: int):
    """A date relative to the test day (2026-09-29)."""
    return Clock.DAY + timedelta(days=days_from_test_day)


def queue_body(**fields) -> dict:
    """A valid Create Queue body for the test day, 8 AM to 5 PM."""
    body = dict(name="Free Medical Mission", category="MEDICAL",
                short_description="Free check-ups, BP tests, and medicines",
                venue="Tagbilaran City Gym", municipality="Tagbilaran City",
                latitude=9.6543, longitude=123.8601, start_date="2026-09-29",
                end_date="2026-09-29", opens_at="08:00", closes_at="17:00")
    body.update(fields)
    return body


def verified_organizer(db, **fields):
    return make_user(db, verification_status=VerificationStatus.VERIFIED,
                     organization_name="City Health Office", **fields)


# ---- Browse -------------------------------------------------------------------

def test_browse_lists_live_and_upcoming_queues_but_not_closed_ones(client, db):
    organizer = make_user(db)
    make_queue(db, organizer, name="Upcoming", status=QueueStatus.UPCOMING,
               start_date=db_day(1), end_date=db_day(1))
    make_queue(db, organizer, name="Open")
    make_queue(db, organizer, name="Paused", status=QueueStatus.PAUSED)
    make_queue(db, organizer, name="Closed", status=QueueStatus.CLOSED)

    names = [queue["name"] for queue in client.get("/queues").json()]
    assert names == ["Open", "Paused", "Upcoming"]  # live first


def test_browse_filters(client, db):
    organizer = make_user(db)
    make_queue(db, organizer, name="Relief in Loon", municipality="Loon")
    make_queue(db, organizer, name="Clinic", category=Category.MEDICAL, venue="City Gym")
    make_queue(db, organizer, name="Relief in town")

    def names(params):
        return {queue["name"] for queue in client.get("/queues", params=params).json()}

    assert names({"municipality": "Loon"}) == {"Relief in Loon"}
    assert names({"category": "MEDICAL"}) == {"Clinic"}
    assert names({"q": "relief"}) == {"Relief in Loon", "Relief in town"}  # any case
    assert names({"q": "gym"}) == {"Clinic"}  # the venue counts too


def test_the_live_numbers(client, db, clock):
    queue = make_queue(db, verified_organizer(db))
    make_ticket(db, queue, make_user(db), status=TicketStatus.SERVED,
                called_at=clock.now() - timedelta(minutes=10))
    make_ticket(db, queue, make_user(db), status=TicketStatus.CALLED,
                called_at=clock.now() - timedelta(minutes=2))
    make_ticket(db, queue, make_user(db))
    make_ticket(db, queue, make_user(db))

    body = client.get(f"/queues/{queue.id}").json()
    assert body["people_waiting"] == 2
    assert body["now_serving"] == 2  # the latest call
    # The calls were 8 minutes apart, so 8 min per person × 2 waiting
    assert body["estimated_wait_minutes"] == 16
    assert body["organizer_name"] == "City Health Office"
    assert body["organizer_verified"] is True


def test_an_unverified_organizer_shows_under_their_own_name(client, db):
    queue = make_queue(db, make_user(db, name="Juan Tamad"))
    body = client.get(f"/queues/{queue.id}").json()
    assert body["organizer_name"] == "Juan Tamad"
    assert body["organizer_verified"] is False


def test_times_and_dates_are_in_the_contract_format(client, db, clock):
    queue = make_queue(db, make_user(db), status=QueueStatus.PAUSED, paused_at=clock.now())
    body = client.get(f"/queues/{queue.id}").json()
    assert body["opens_at"] == "08:00"
    assert body["start_date"] == "2026-09-29"
    assert body["paused_at"] == "2026-09-29T10:00:00+08:00"


def test_browse_runs_the_same_number_of_queries_for_any_number_of_queues(client, db, engine):
    def queries_to_browse() -> int:
        statements = []
        listener = lambda *args: statements.append(args[2])  # noqa: E731
        event.listen(engine, "before_cursor_execute", listener)
        client.get("/queues")
        event.remove(engine, "before_cursor_execute", listener)
        return len(statements)

    organizer = make_user(db)
    for _ in range(2):
        make_ticket(db, make_queue(db, organizer), make_user(db))
    few = queries_to_browse()
    for _ in range(8):
        make_ticket(db, make_queue(db, make_user(db)), make_user(db))
    assert queries_to_browse() == few


def test_an_unknown_queue_is_not_found(client):
    response = client.get("/queues/nope")
    assert response.status_code == 404
    assert response.json()["error"] == "QUEUE_NOT_FOUND"


def test_a_closed_queue_still_has_its_page(client, db):
    queue = make_queue(db, make_user(db), status=QueueStatus.CLOSED)
    assert client.get(f"/queues/{queue.id}").json()["status"] == "CLOSED"


def test_browse_moves_queues_along_the_schedule(client, db, clock):
    make_queue(db, make_user(db), name="Ends at 5")
    clock.set(clock.now().replace(hour=17, minute=1))
    assert client.get("/queues").json() == []


# ---- My queues ----------------------------------------------------------------

def test_my_queues_are_mine_in_every_status(client, db):
    me = make_user(db)
    make_queue(db, me, name="Mine, closed", status=QueueStatus.CLOSED)
    make_queue(db, me, name="Mine, open")
    make_queue(db, make_user(db), name="Someone else's")
    names = [queue["name"] for queue in client.get("/me/queues", headers=sign_in(db, me)).json()]
    assert names == ["Mine, open", "Mine, closed"]


# ---- Create -------------------------------------------------------------------

def test_creating_a_queue_that_has_opened(client, db):
    response = client.post("/queues", json=queue_body(), headers=sign_in(db, make_user(db)))
    assert response.status_code == 201
    assert response.json()["status"] == "OPEN"


def test_creating_a_queue_for_tomorrow(client, db):
    response = client.post("/queues", json=queue_body(start_date="2026-09-30",
                                                      end_date="2026-09-30"),
                           headers=sign_in(db, make_user(db)))
    assert response.json()["status"] == "UPCOMING"


def test_creating_needs_signing_in(client):
    assert client.post("/queues", json=queue_body()).status_code == 401


def test_fields_that_dont_fit_together_name_the_field(client, db):
    headers = sign_in(db, make_user(db))

    def field_of(**fields):
        response = client.post("/queues", json=queue_body(**fields), headers=headers)
        assert response.status_code == 422
        return set(response.json()["fields"])

    assert field_of(end_date="2026-09-28") == {"end_date"}
    assert field_of(closes_at="07:00") == {"closes_at"}
    assert field_of(proximity_check_enabled=True, join_radius_meters=300) == {"join_radius_meters"}
    assert field_of(start_date="2026-09-28") == {"start_date"}
    assert field_of(closes_at="09:00") == {"closes_at"}  # already passed at 10 AM
    assert field_of(short_description="x" * 51) == {"short_description"}


def test_no_proximity_check_means_no_radius(client, db):
    body = client.post("/queues", json=queue_body(join_radius_meters=1000),
                       headers=sign_in(db, make_user(db))).json()
    assert body["join_radius_meters"] == 0


def test_an_unverified_organizer_gets_one_live_queue(client, db):
    organizer = make_user(db)
    running = make_queue(db, organizer)
    response = client.post("/queues", json=queue_body(), headers=sign_in(db, organizer))
    assert response.status_code == 409
    assert response.json()["error"] == "ONE_LIVE_QUEUE"
    # It names the one running, for the app's sheet
    assert response.json()["live_queue_id"] == running.id
    assert response.json()["live_queue_name"] == running.name
    assert response.json()["live_queue_status"] == "OPEN"


def test_an_unverified_organizer_can_still_plan_an_upcoming_one(client, db):
    organizer = make_user(db)
    make_queue(db, organizer)
    response = client.post("/queues", json=queue_body(start_date="2026-09-30",
                                                      end_date="2026-09-30"),
                           headers=sign_in(db, organizer))
    assert response.status_code == 201


def test_a_verified_organizer_has_no_limit(client, db):
    organizer = verified_organizer(db)
    make_queue(db, organizer)
    response = client.post("/queues", json=queue_body(), headers=sign_in(db, organizer))
    assert response.status_code == 201


# ---- Edit ---------------------------------------------------------------------

def test_only_the_organizer_can_edit(client, db):
    queue = make_queue(db, make_user(db))
    response = client.patch(f"/queues/{queue.id}", json={"name": "Mine now"},
                            headers=sign_in(db, make_user(db)))
    assert response.status_code == 403
    assert response.json()["error"] == "NOT_OWNER"


def test_editing_changes_only_what_was_sent(client, db):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    body = client.patch(f"/queues/{queue.id}", json={"name": "Relief, day 2"},
                        headers=sign_in(db, organizer)).json()
    assert body["name"] == "Relief, day 2"
    assert body["venue"] == "Brgy. Poblacion Hall"


def test_an_edit_still_has_to_fit(client, db):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    response = client.patch(f"/queues/{queue.id}", json={"closes_at": "07:00"},
                            headers=sign_in(db, organizer))
    assert response.status_code == 422
    assert "closes_at" in response.json()["fields"]


def test_a_required_field_cant_be_emptied(client, db):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    response = client.patch(f"/queues/{queue.id}", json={"name": None},
                            headers=sign_in(db, organizer))
    assert response.status_code == 422


def test_a_closed_queue_cant_be_edited(client, db):
    organizer = make_user(db)
    queue = make_queue(db, organizer, status=QueueStatus.CLOSED)
    response = client.patch(f"/queues/{queue.id}", json={"name": "Again"},
                            headers=sign_in(db, organizer))
    assert response.json()["error"] == "WRONG_STATUS"


# ---- Running ------------------------------------------------------------------

def test_pause_and_resume(client, db):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    headers = sign_in(db, organizer)

    paused = client.post(f"/queues/{queue.id}/pause", headers=headers).json()
    assert paused["status"] == "PAUSED"
    assert paused["paused_at"] == "2026-09-29T10:00:00+08:00"
    again = client.post(f"/queues/{queue.id}/pause", headers=headers)
    assert again.status_code == 409

    resumed = client.post(f"/queues/{queue.id}/resume", headers=headers).json()
    assert resumed["status"] == "OPEN"
    assert resumed["paused_at"] is None


def test_closing_releases_the_line(client, db):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    ticket = make_ticket(db, queue, make_user(db))
    headers = sign_in(db, organizer)

    body = client.post(f"/queues/{queue.id}/close", headers=headers).json()
    assert body["status"] == "CLOSED"
    assert body["people_waiting"] == 0
    db.refresh(ticket)
    assert ticket.status == TicketStatus.QUEUE_CLOSED
    assert client.post(f"/queues/{queue.id}/close", headers=headers).status_code == 409


def test_extending_the_closing_time(client, db):
    organizer = make_user(db)
    queue = make_queue(db, organizer)
    headers = sign_in(db, organizer)
    later = client.post(f"/queues/{queue.id}/extend", json={"closes_at": "18:30"},
                        headers=headers)
    assert later.json()["closes_at"] == "18:30"
    earlier = client.post(f"/queues/{queue.id}/extend", json={"closes_at": "16:00"},
                          headers=headers)
    assert earlier.status_code == 422


def test_running_someone_elses_queue_is_refused(client, db):
    queue = make_queue(db, make_user(db))
    response = client.post(f"/queues/{queue.id}/pause", headers=sign_in(db, make_user(db)))
    assert response.status_code == 403

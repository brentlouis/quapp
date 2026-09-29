"""The Live console and the queuer's answers to a call (BACKEND.md steps 5.1 to 5.3). The clock
is fixed at 10:00 AM on 2026-09-29; make_queue's default is an OPEN queue that day, 8 AM to 5 PM,
and the estimator's default is 5 minutes per person."""

from datetime import datetime, time, timedelta

from app.enums import QueueStatus, RemovalReason, TicketStatus
from app.models import TicketRemoval
from app.timeutil import MANILA
from tests.conftest import Clock
from tests.factories import make_queue, make_ticket, make_user, sign_in


def at(hour: int, minute: int = 0) -> datetime:
    """A moment on the test day (2026-09-29), Manila time."""
    return datetime.combine(Clock.DAY, time(hour, minute), MANILA)


def running_queue(db, **fields):
    """A queue, its organizer, and the organizer's headers."""
    organizer = make_user(db)
    queue = make_queue(db, organizer, **fields)
    return queue, sign_in(db, organizer)


def line_of(client, queue, headers) -> dict:
    return client.get(f"/queues/{queue.id}/line", headers=headers).json()


# ---- The line -----------------------------------------------------------------

def test_the_line_in_call_order_with_phones_masked(client, db):
    queue, headers = running_queue(db)
    maria = make_user(db, name="Maria Santos", phone="09171230002")
    make_ticket(db, queue, maria)
    make_ticket(db, queue, holder_name="Lola Nena")  # a walk-in
    line = line_of(client, queue, headers)
    assert line["now_serving"] is None
    assert [t["holder_name"] for t in line["waiting"]] == ["Maria Santos", "Lola Nena"]
    assert [t["position"] for t in line["waiting"]] == [1, 2]
    assert line["waiting"][0]["holder_phone"] == "0917 ••• 0002"
    assert line["waiting"][1]["holder_phone"] is None


def test_only_the_organizer_sees_the_line(client, db):
    queue, _ = running_queue(db)
    response = client.get(f"/queues/{queue.id}/line", headers=sign_in(db, make_user(db)))
    assert response.status_code == 403


# ---- Calling ------------------------------------------------------------------

def test_a_round_at_the_counter(client, db, clock):
    """join ×3, call, call, no-show: the BACKEND.md 5.2 script."""
    queue, headers = running_queue(db)
    first, second, third = (make_ticket(db, queue, make_user(db)) for _ in range(3))

    line = client.post(f"/queues/{queue.id}/call-next", headers=headers).json()
    assert line["now_serving"]["ticket_number"] == 1

    clock.set(time(10, 4))
    line = client.post(f"/queues/{queue.id}/call-next", headers=headers).json()
    assert line["now_serving"]["ticket_number"] == 2

    clock.set(time(10, 9))
    line = client.post(f"/queues/{queue.id}/no-show", headers=headers).json()
    assert line["now_serving"]["ticket_number"] == 3
    assert line["waiting"] == []

    for ticket in (first, second, third):
        db.refresh(ticket)
    assert (first.status, second.status, third.status) == (
        TicketStatus.SERVED, TicketStatus.NO_SHOW, TicketStatus.CALLED)
    assert first.finished_at == second.called_at  # served when the next was called
    assert second.finished_at == third.called_at == clock.now()


def test_calling_the_last_one_empties_the_counter(client, db):
    queue, headers = running_queue(db)
    make_ticket(db, queue, make_user(db), status=TicketStatus.CALLED, called_at=at(10))
    line = client.post(f"/queues/{queue.id}/call-next", headers=headers).json()
    assert line["now_serving"] is None


def test_a_no_show_needs_someone_at_the_counter(client, db):
    queue, headers = running_queue(db)
    make_ticket(db, queue, make_user(db))
    response = client.post(f"/queues/{queue.id}/no-show", headers=headers)
    assert response.status_code == 409
    assert response.json()["error"] == "NOBODY_CALLED"


def test_a_paused_queue_still_serves_its_line_and_an_upcoming_one_doesnt(client, db, clock):
    paused, headers = running_queue(db, status=QueueStatus.PAUSED)
    make_ticket(db, paused, make_user(db))
    assert client.post(f"/queues/{paused.id}/call-next", headers=headers).status_code == 200

    tomorrow = clock.today() + timedelta(days=1)
    upcoming, headers = running_queue(db, status=QueueStatus.UPCOMING,
                                      start_date=tomorrow, end_date=tomorrow)
    response = client.post(f"/queues/{upcoming.id}/call-next", headers=headers)
    assert response.json()["error"] == "WRONG_STATUS"


def test_a_call_times_out_after_three_minutes_without_im_here(client, db, clock):
    queue, headers = running_queue(db, grace_period_enabled=True)
    maria = make_user(db)
    ticket = make_ticket(db, queue, maria)
    client.post(f"/queues/{queue.id}/call-next", headers=headers)

    clock.set(time(10, 2, 59))
    assert line_of(client, queue, headers)["now_serving_timed_out"] is False
    clock.set(time(10, 3))
    assert line_of(client, queue, headers)["now_serving_timed_out"] is True
    # The server leaves the decision to the organizer
    db.refresh(ticket)
    assert ticket.status == TicketStatus.CALLED


def test_im_here_confirms_on_the_console(client, db, clock):
    queue, headers = running_queue(db, grace_period_enabled=True)
    maria = make_user(db)
    ticket = make_ticket(db, queue, maria)
    client.post(f"/queues/{queue.id}/call-next", headers=headers)

    clock.set(time(10, 1))
    response = client.post(f"/tickets/{ticket.id}/here", headers=sign_in(db, maria))
    assert response.status_code == 200
    line = line_of(client, queue, headers)
    assert line["now_serving_here_at"] == "2026-09-29T10:01:00+08:00"
    clock.set(time(10, 5))
    assert line_of(client, queue, headers)["now_serving_timed_out"] is False


# ---- I'm here -----------------------------------------------------------------

def test_im_here_only_while_called_and_within_the_grace_window(client, db, clock):
    queue, headers = running_queue(db, grace_period_enabled=True)
    maria = make_user(db)
    ticket = make_ticket(db, queue, maria)
    maria_headers = sign_in(db, maria)

    early = client.post(f"/tickets/{ticket.id}/here", headers=maria_headers)
    assert early.json()["error"] == "NOT_CALLED"

    client.post(f"/queues/{queue.id}/call-next", headers=headers)
    clock.set(time(10, 3))
    late = client.post(f"/tickets/{ticket.id}/here", headers=maria_headers)
    assert late.json()["error"] == "GRACE_OVER"


def test_without_the_grace_period_im_here_has_no_deadline(client, db, clock):
    queue, headers = running_queue(db, grace_period_enabled=False)
    maria = make_user(db)
    ticket = make_ticket(db, queue, maria)
    client.post(f"/queues/{queue.id}/call-next", headers=headers)
    clock.set(time(10, 30))
    assert client.post(f"/tickets/{ticket.id}/here", headers=sign_in(db, maria)).status_code == 200


# ---- Walk-ins and removals ----------------------------------------------------

def test_a_walk_in_joins_at_the_back_with_no_phone(client, db):
    queue, headers = running_queue(db)
    make_ticket(db, queue, make_user(db))
    body = client.post(f"/queues/{queue.id}/walk-ins", json={"name": "Lola Nena"},
                       headers=headers).json()
    assert body["ticket_number"] == 2
    assert body["walk_in"] is True
    assert body["holder_phone"] is None
    assert body["position"] == 2


def test_removing_someone_with_a_reason(client, db, clock):
    queue, headers = running_queue(db)
    ticket = make_ticket(db, queue, make_user(db))
    response = client.post(f"/tickets/{ticket.id}/remove", json={"reason": "PRANK"},
                           headers=headers)
    assert response.status_code == 200
    assert response.json()["reason"] == "PRANK"
    db.refresh(ticket)
    assert ticket.status == TicketStatus.REMOVED
    assert ticket.removal_reason == RemovalReason.PRANK
    assert db.get(TicketRemoval, response.json()["id"]).ticket_id == ticket.id

    again = client.post(f"/tickets/{ticket.id}/remove", json={"reason": "DUPLICATE"},
                        headers=headers)
    assert again.status_code == 409


def test_only_the_organizer_removes(client, db):
    queue, _ = running_queue(db)
    ticket = make_ticket(db, queue, make_user(db))
    response = client.post(f"/tickets/{ticket.id}/remove", json={"reason": "PRANK"},
                           headers=sign_in(db, make_user(db)))
    assert response.status_code == 403


# ---- I need more time ---------------------------------------------------------

def line_with_maria(db, queue, maria_at: int, size: int):
    """`size` people waiting, Maria holding ticket number `maria_at`."""
    maria = make_user(db)
    tickets = [make_ticket(db, queue, maria if n == maria_at else make_user(db))
               for n in range(1, size + 1)]
    return maria, tickets[maria_at - 1]


def test_moving_back_lets_people_ahead_and_keeps_the_number(client, db):
    queue, _ = running_queue(db)
    maria, ticket = line_with_maria(db, queue, maria_at=2, size=6)
    # 10 minutes at 5 minutes per person = 2 places: behind #3 and #4, before #5
    body = client.post(f"/tickets/{ticket.id}/move-back", json={"minutes_needed": 10},
                       headers=sign_in(db, maria)).json()
    assert body["ticket_number"] == 2
    assert body["position"] == 4
    assert body["moved_back"] is True
    db.refresh(ticket)
    assert ticket.line_order == 4.5


def test_moving_back_stops_at_the_end_of_the_line(client, db):
    queue, _ = running_queue(db)
    maria, ticket = line_with_maria(db, queue, maria_at=5, size=6)
    body = client.post(f"/tickets/{ticket.id}/move-back", json={"minutes_needed": 45},
                       headers=sign_in(db, maria)).json()
    assert body["position"] == 6


def test_moving_back_once_only(client, db):
    queue, _ = running_queue(db)
    maria, ticket = line_with_maria(db, queue, maria_at=1, size=6)
    headers = sign_in(db, maria)
    client.post(f"/tickets/{ticket.id}/move-back", json={"minutes_needed": 5}, headers=headers)
    again = client.post(f"/tickets/{ticket.id}/move-back", json={"minutes_needed": 5},
                        headers=headers)
    assert again.json()["error"] == "ALREADY_MOVED_BACK"


def test_the_last_in_line_cant_move_back(client, db):
    queue, _ = running_queue(db)
    maria, ticket = line_with_maria(db, queue, maria_at=3, size=3)
    response = client.post(f"/tickets/{ticket.id}/move-back", json={"minutes_needed": 5},
                           headers=sign_in(db, maria))
    assert response.json()["error"] == "LAST_IN_LINE"


def test_the_preview_changes_nothing(client, db):
    queue, _ = running_queue(db)
    maria, ticket = line_with_maria(db, queue, maria_at=2, size=6)
    headers = sign_in(db, maria)
    preview = client.post(f"/tickets/{ticket.id}/move-back", params={"dry_run": "true"},
                          json={"minutes_needed": 10}, headers=headers).json()
    assert preview["position"] == 4
    db.refresh(ticket)
    assert ticket.line_order == 2.0
    assert ticket.moved_back is False
    # And the real move still works afterwards
    real = client.post(f"/tickets/{ticket.id}/move-back", json={"minutes_needed": 10},
                       headers=headers).json()
    assert real["position"] == 4


def test_a_called_ticket_hands_the_counter_back(client, db):
    queue, headers = running_queue(db)
    maria, ticket = line_with_maria(db, queue, maria_at=1, size=4)
    client.post(f"/queues/{queue.id}/call-next", headers=headers)  # Maria is called
    body = client.post(f"/tickets/{ticket.id}/move-back", json={"minutes_needed": 10},
                       headers=sign_in(db, maria)).json()
    assert body["status"] == "WAITING"
    assert body["called_at"] is None
    assert body["position"] == 3  # #2 and #3 go first
    assert line_of(client, queue, headers)["now_serving"] is None


def test_only_the_sheets_choices(client, db):
    queue, _ = running_queue(db)
    maria, ticket = line_with_maria(db, queue, maria_at=1, size=3)
    response = client.post(f"/tickets/{ticket.id}/move-back", json={"minutes_needed": 7},
                           headers=sign_in(db, maria))
    assert response.status_code == 422

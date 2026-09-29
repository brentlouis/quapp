"""The admin web page (BACKEND.md phase 8): signing in, verification, reports, users."""

from sqlalchemy import select

from app.enums import OrganizationType, RemovalReason, TicketStatus, UserStatus, VerificationStatus
from app.models import Report, Ticket, TicketRemoval, Token, VerificationRequest
from app.routers.admin import COOKIE
from app.security import hash_password
from tests.factories import make_queue, make_ticket, make_user, sign_in

PASSWORD = "admin-pass"


def admin_client(client, db):
    """The test client signed in as a fresh admin, through the login form."""
    admin = make_user(db, is_admin=True, phone="09990000000", password_hash=hash_password(PASSWORD))
    response = client.post("/admin/login", data={"phone": "0999 000 0000", "password": PASSWORD},
                           follow_redirects=False)
    assert response.status_code == 303
    client.cookies.set(COOKIE, response.cookies[COOKIE])
    return admin


def pending_request(db, **user_fields):
    user = make_user(db, verification_status=VerificationStatus.PENDING, **user_fields)
    request = VerificationRequest(user_id=user.id, organization_name="Brgy. Cogon Council",
                                  organization_type=OrganizationType.BARANGAY,
                                  position="Secretary", office_phone="(038) 411 2345")
    db.add(request)
    db.flush()
    return user, request


# ---- Signing in ---------------------------------------------------------------------

def test_pages_need_an_admin(client, db):
    response = client.get("/admin/verifications", follow_redirects=False)
    assert response.status_code == 303
    assert response.headers["location"] == "/admin/login"


def test_a_normal_account_cant_sign_in(client, db):
    make_user(db, phone="09171234567", password_hash=hash_password(PASSWORD))
    response = client.post("/admin/login", data={"phone": "09171234567", "password": PASSWORD},
                           follow_redirects=False)
    assert response.headers["location"] == "/admin/login?error=1"
    assert COOKIE not in response.cookies


def test_the_cookie_is_httponly_and_samesite(client, db):
    make_user(db, is_admin=True, phone="09990000000", password_hash=hash_password(PASSWORD))
    response = client.post("/admin/login", data={"phone": "09990000000", "password": PASSWORD},
                           follow_redirects=False)
    cookie = response.headers["set-cookie"].lower()
    assert "httponly" in cookie
    assert "samesite=lax" in cookie


def test_logging_out_ends_the_session(client, db):
    admin_client(client, db)
    client.post("/admin/logout", follow_redirects=False)
    response = client.get("/admin/verifications", follow_redirects=False)
    assert response.status_code == 303


def test_the_app_api_doesnt_list_the_admin_page(client):
    paths = client.get("/openapi.json").json()["paths"]
    assert not any(path.startswith("/admin") for path in paths)


# ---- Verification -----------------------------------------------------------------

def test_the_page_lists_whats_waiting(client, db):
    admin_client(client, db)
    pending_request(db, name="Rhea Cruz")
    page = client.get("/admin/verifications").text
    assert "Brgy. Cogon Council" in page
    assert "(038) 411 2345" in page
    assert "Rhea Cruz" in page


def test_approving_gives_the_badge_and_wipes_the_answers(client, db):
    admin = admin_client(client, db)
    user, request = pending_request(db)
    client.post(f"/admin/verifications/{request.id}/approve", data={"note": "Spoke to Kap. Reyes"})
    db.refresh(user)
    db.refresh(request)
    assert user.verification_status == VerificationStatus.VERIFIED
    assert user.organization_name == "Brgy. Cogon Council"
    assert request.status == VerificationStatus.VERIFIED
    assert request.admin_note == "Spoke to Kap. Reyes"
    assert request.decided_by == admin.id
    assert (request.organization_name, request.position, request.office_phone) == (None, None, None)


def test_rejecting(client, db):
    admin_client(client, db)
    user, request = pending_request(db)
    client.post(f"/admin/verifications/{request.id}/reject", data={"note": "Office never heard of them"})
    db.refresh(user)
    assert user.verification_status == VerificationStatus.REJECTED
    assert user.organization_name is None


def test_a_request_is_decided_once(client, db):
    admin_client(client, db)
    user, request = pending_request(db)
    client.post(f"/admin/verifications/{request.id}/approve", data={})
    response = client.post(f"/admin/verifications/{request.id}/reject", data={},
                           follow_redirects=False)
    assert response.headers["location"].endswith("already-decided")
    db.refresh(user)
    assert user.verification_status == VerificationStatus.VERIFIED


# ---- Reports ----------------------------------------------------------------------

def test_reports_grouped_by_queue_and_marked_handled(client, db):
    admin_client(client, db)
    queue = make_queue(db, make_user(db), name="Suspicious Payout")
    for reason in ("ASKED_FOR_MONEY", "FAKE"):
        db.add(Report(queue_id=queue.id, reporter_id=make_user(db).id, reason=reason,
                      details="They asked for ₱100" if reason == "ASKED_FOR_MONEY" else None))
    db.flush()

    page = client.get("/admin/reports").text
    assert "Suspicious Payout" in page
    assert "2 reports" in page
    assert "They asked for ₱100" in page

    client.post(f"/admin/reports/{queue.id}/handled")
    assert all(r.handled_at for r in db.scalars(select(Report)))


def test_prank_removals_show_up(client, db):
    admin_client(client, db)
    queue = make_queue(db, make_user(db), name="Relief")
    ticket = make_ticket(db, queue, make_user(db, name="Juan Tamad"),
                         status=TicketStatus.REMOVED, removal_reason=RemovalReason.PRANK)
    db.add(TicketRemoval(ticket_id=ticket.id, queue_id=queue.id, reason=RemovalReason.PRANK,
                         removed_by=queue.organizer_id))
    db.flush()
    assert "Juan Tamad" in client.get("/admin/reports").text


# ---- Users ------------------------------------------------------------------------

def test_finding_someone_by_phone(client, db):
    admin_client(client, db)
    make_user(db, name="Jun Dela Cruz", phone="09181234567")
    page = client.get("/admin/users", params={"phone": "0918 123"}).text
    assert "Jun Dela Cruz" in page


def test_suspending_signs_them_out_everywhere(client, db):
    admin_client(client, db)
    jun = make_user(db)
    app_headers = sign_in(db, jun)
    client.post(f"/admin/users/{jun.id}/suspend", data={"reason": "Posting a fake queue"})

    db.refresh(jun)
    assert jun.status == UserStatus.SUSPENDED
    assert db.scalar(select(Token).where(Token.user_id == jun.id)) is None
    # The app's next call is refused (NOT_SIGNED_IN: the token is gone)
    assert client.get("/me", headers=app_headers).status_code == 401


def test_suspending_closes_their_queues_and_releases_their_tickets(client, db):
    admin_client(client, db)
    jun = make_user(db)
    his_queue = make_queue(db, jun)
    someone = make_ticket(db, his_queue, make_user(db))
    his_ticket = make_ticket(db, make_queue(db, make_user(db)), jun)
    his_ticket_id = his_ticket.id

    client.post(f"/admin/users/{jun.id}/suspend", data={"reason": "Posting a fake queue"})

    db.refresh(his_queue)
    db.refresh(someone)
    assert his_queue.status.value == "CLOSED"
    assert someone.status == TicketStatus.QUEUE_CLOSED  # not a no-show for them
    db.expire_all()
    assert db.get(Ticket, his_ticket_id) is None  # released, like leaving


def test_unsuspending(client, db):
    admin_client(client, db)
    jun = make_user(db, status=UserStatus.SUSPENDED, suspended_reason="Test")
    client.post(f"/admin/users/{jun.id}/unsuspend")
    db.refresh(jun)
    assert jun.status == UserStatus.ACTIVE
    assert jun.suspended_reason is None


def test_revoking_the_badge(client, db):
    admin_client(client, db)
    rhea = make_user(db, verification_status=VerificationStatus.VERIFIED,
                     organization_name="Brgy. Poblacion Council")
    queue = make_queue(db, rhea)
    client.post(f"/admin/users/{rhea.id}/revoke")
    body = client.get(f"/queues/{queue.id}").json()
    assert body["organizer_verified"] is False
    assert body["organizer_name"] == rhea.name


def test_admins_cant_be_suspended_from_the_page(client, db):
    admin = admin_client(client, db)
    response = client.post(f"/admin/users/{admin.id}/suspend", data={"reason": "oops"},
                           follow_redirects=False)
    assert response.headers["location"].endswith("not-allowed")
    db.refresh(admin)
    assert admin.status == UserStatus.ACTIVE

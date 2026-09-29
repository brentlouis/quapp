"""Reports and verification requests (BACKEND.md steps 7.1 and 7.2)."""

from sqlalchemy import select

from app.enums import VerificationStatus
from app.models import Report
from tests.factories import make_queue, make_user, sign_in

VERIFY = dict(organization_name="Brgy. Poblacion Council", organization_type="BARANGAY",
              position="Barangay Secretary", office_phone="(038) 411 2345")


# ---- Reports ------------------------------------------------------------------

def test_reporting_a_queue_keeps_the_reporter_private(client, db):
    queue = make_queue(db, make_user(db))
    maria = make_user(db)
    response = client.post(f"/queues/{queue.id}/reports",
                           json={"reason": "ASKED_FOR_MONEY", "details": "  Asked ₱50 to join  "},
                           headers=sign_in(db, maria))
    assert response.status_code == 201
    body = response.json()
    assert "reporter_id" not in body
    assert body["details"] == "Asked ₱50 to join"
    assert db.scalar(select(Report.reporter_id)) == maria.id  # kept on the server


def test_one_report_per_person_per_queue(client, db):
    queue = make_queue(db, make_user(db))
    headers = sign_in(db, make_user(db))
    client.post(f"/queues/{queue.id}/reports", json={"reason": "FAKE"}, headers=headers)
    again = client.post(f"/queues/{queue.id}/reports", json={"reason": "OTHER"}, headers=headers)
    assert again.status_code == 409
    assert again.json()["error"] == "ALREADY_REPORTED"


def test_reporting_needs_a_real_queue_and_reason(client, db):
    headers = sign_in(db, make_user(db))
    assert client.post("/queues/nope/reports", json={"reason": "FAKE"},
                       headers=headers).status_code == 404
    queue = make_queue(db, make_user(db))
    assert client.post(f"/queues/{queue.id}/reports", json={"reason": "BORING"},
                       headers=headers).status_code == 422


# ---- Verification -------------------------------------------------------------

def test_asking_to_be_verified_makes_the_account_pending(client, db):
    rhea = make_user(db)
    headers = sign_in(db, rhea)
    response = client.post("/me/verification", json=VERIFY, headers=headers)
    assert response.status_code == 201
    assert response.json()["status"] == "PENDING"
    assert response.json()["organization_type"] == "BARANGAY"
    db.refresh(rhea)
    assert rhea.verification_status == VerificationStatus.PENDING
    assert client.get("/me", headers=headers).json()["verification_status"] == "PENDING"


def test_one_request_at_a_time(client, db):
    headers = sign_in(db, make_user(db))
    client.post("/me/verification", json=VERIFY, headers=headers)
    again = client.post("/me/verification", json=VERIFY, headers=headers)
    assert again.status_code == 409
    assert again.json()["error"] == "VERIFICATION_PENDING"


def test_verified_organizers_dont_ask_again(client, db):
    headers = sign_in(db, make_user(db, verification_status=VerificationStatus.VERIFIED))
    response = client.post("/me/verification", json=VERIFY, headers=headers)
    assert response.json()["error"] == "ALREADY_VERIFIED"


def test_after_a_rejection_you_may_ask_again(client, db):
    headers = sign_in(db, make_user(db, verification_status=VerificationStatus.REJECTED))
    assert client.post("/me/verification", json=VERIFY, headers=headers).status_code == 201


def test_the_office_number_must_be_callable(client, db):
    headers = sign_in(db, make_user(db))
    for phone in ("12345", "0917 123 4567 890"):  # too short, too long
        response = client.post("/me/verification", json=dict(VERIFY, office_phone=phone),
                               headers=headers)
        assert response.status_code == 422
        assert "office_phone" in response.json()["fields"]


def test_the_pending_card_reads_the_latest_request(client, db):
    headers = sign_in(db, make_user(db))
    assert client.get("/me/verification", headers=headers).json() is None
    client.post("/me/verification", json=VERIFY, headers=headers)
    latest = client.get("/me/verification", headers=headers).json()
    assert latest["organization_name"] == "Brgy. Poblacion Council"
    assert latest["status"] == "PENDING"

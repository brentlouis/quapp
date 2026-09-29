"""Register, log in, log out, /me (BACKEND.md steps 2.2 and 2.3), and the error shape
(MODELS.md "Errors")."""

from sqlalchemy import select

from app import timeutil
from app.enums import UserStatus
from app.models import Token, User
from app.security import hash_password
from tests.factories import make_user

PASSWORD = "secret-pass"


def register(client, **fields):
    body = dict(name="Maria Santos", phone="09171234567", password=PASSWORD,
                device_install_id="device-a")
    body.update(fields)
    return client.post("/auth/register", json=body)


def signed_in(token):
    return {"Authorization": f"Bearer {token}"}


# ---- Register -----------------------------------------------------------------

def test_register_returns_a_token_and_the_user_without_secrets(client, db):
    response = register(client)
    assert response.status_code == 201
    body = response.json()
    assert set(body["user"]) == {"id", "name", "phone", "status", "suspended_reason",
                                 "suspended_at", "verification_status", "organization_name"}
    assert body["user"]["status"] == "ACTIVE"
    assert body["user"]["verification_status"] == "NONE"
    assert db.get(Token, body["token"]).user_id == body["user"]["id"]


def test_the_password_is_stored_hashed(client, db):
    register(client)
    user = db.scalar(select(User))
    assert user.password_hash != PASSWORD
    assert user.password_hash.startswith("$2b$")  # bcrypt


def test_spaces_and_dashes_in_the_phone_are_ignored(client):
    response = register(client, phone="0917 123-4567")
    assert response.json()["user"]["phone"] == "09171234567"


def test_a_taken_phone_is_refused(client):
    register(client)
    response = register(client, device_install_id="device-b")
    assert response.status_code == 409
    assert response.json()["error"] == "PHONE_TAKEN"


def test_a_third_account_on_one_device_is_refused(client):
    register(client, phone="09170000001")
    register(client, phone="09170000002")
    response = register(client, phone="09170000003")
    assert response.status_code == 409
    assert response.json()["error"] == "DEVICE_LIMIT"


def test_a_short_password_is_invalid_input(client):
    response = register(client, password="short")
    assert response.status_code == 422
    body = response.json()
    assert body["error"] == "INVALID_INPUT"
    assert "password" in body["fields"]
    assert body["message"] == "Use at least 8 characters."


def test_a_landline_is_not_a_mobile_number(client):
    response = register(client, phone="(038) 411 2345")
    assert response.status_code == 422
    assert "phone" in response.json()["fields"]


def test_a_blank_name_is_invalid_input(client):
    response = register(client, name="   ")
    assert response.status_code == 422
    assert "name" in response.json()["fields"]


# ---- Login --------------------------------------------------------------------

def test_login_with_the_right_password(client, db):
    make_user(db, phone="09171234567", password_hash=hash_password(PASSWORD))
    response = client.post("/auth/login", json={"phone": "0917 123 4567", "password": PASSWORD})
    assert response.status_code == 200
    assert response.json()["user"]["phone"] == "09171234567"


def test_a_wrong_password_and_an_unknown_phone_get_the_same_answer(client, db):
    make_user(db, phone="09171234567", password_hash=hash_password(PASSWORD))
    wrong = client.post("/auth/login", json={"phone": "09171234567", "password": "nope-nope"})
    unknown = client.post("/auth/login", json={"phone": "09179999999", "password": PASSWORD})
    assert wrong.status_code == unknown.status_code == 401
    assert wrong.json() == unknown.json()
    assert wrong.json()["error"] == "WRONG_CREDENTIALS"


def test_a_suspended_user_gets_the_reason(client, db):
    make_user(db, phone="09180000000", password_hash=hash_password(PASSWORD),
              status=UserStatus.SUSPENDED, suspended_reason="Posting a fake queue",
              suspended_at=timeutil.now())
    response = client.post("/auth/login", json={"phone": "09180000000", "password": PASSWORD})
    assert response.status_code == 403
    body = response.json()
    assert body["error"] == "SUSPENDED"
    assert body["suspended_reason"] == "Posting a fake queue"
    assert body["suspended_at"].endswith("+08:00")  # Manila time, as MODELS.md says


def test_suspension_is_only_revealed_to_the_right_password(client, db):
    make_user(db, phone="09180000000", password_hash=hash_password(PASSWORD),
              status=UserStatus.SUSPENDED, suspended_reason="Posting a fake queue")
    response = client.post("/auth/login", json={"phone": "09180000000", "password": "nope-nope"})
    assert response.json()["error"] == "WRONG_CREDENTIALS"


# ---- Signed in ----------------------------------------------------------------

def test_me_with_a_token(client):
    token = register(client).json()["token"]
    response = client.get("/me", headers=signed_in(token))
    assert response.status_code == 200
    assert response.json()["name"] == "Maria Santos"


def test_me_without_a_token(client):
    response = client.get("/me")
    assert response.status_code == 401
    assert response.json()["error"] == "NOT_SIGNED_IN"


def test_a_made_up_token_is_not_signed_in(client):
    response = client.get("/me", headers=signed_in("made-up"))
    assert response.status_code == 401


def test_logout_ends_that_session_only(client):
    first = register(client).json()["token"]
    second = client.post("/auth/login", json={"phone": "09171234567",
                                              "password": PASSWORD}).json()["token"]
    assert client.post("/auth/logout", headers=signed_in(first)).status_code == 204
    assert client.get("/me", headers=signed_in(first)).status_code == 401
    assert client.get("/me", headers=signed_in(second)).status_code == 200


def test_suspension_signs_the_user_out_everywhere(client, db):
    body = register(client).json()
    user = db.get(User, body["user"]["id"])
    user.status = UserStatus.SUSPENDED
    user.suspended_reason = "Posting a fake queue"
    db.flush()

    response = client.get("/me", headers=signed_in(body["token"]))
    assert response.status_code == 403
    assert response.json()["error"] == "SUSPENDED"
    assert db.scalar(select(Token).where(Token.user_id == user.id)) is None

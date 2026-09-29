"""Accounts: register, log in, log out, and who am I (MODELS.md "Endpoints")."""

from fastapi import APIRouter, Depends, Response
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.database import get_db
from app.deps import current_token, current_user, suspended_error
from app.enums import UserStatus
from app.errors import ApiError
from app.models import Token, User
from app.schemas import AuthOut, LoginIn, RegisterIn, UserOut
from app.security import check_password, hash_password, new_token

router = APIRouter()

# At most this many accounts per app install (DECISIONS.md, trust and safety)
MAX_ACCOUNTS_PER_DEVICE = 2


def sign_in(db: Session, user: User) -> AuthOut:
    """Starts a session: a new token row, returned with the user."""
    token = Token(token=new_token(), user_id=user.id)
    db.add(token)
    db.commit()
    return AuthOut(token=token.token, user=UserOut.model_validate(user))


@router.post("/auth/register", response_model=AuthOut, status_code=201)
def register(body: RegisterIn, db: Session = Depends(get_db)) -> AuthOut:
    if db.scalar(select(User.id).where(User.phone == body.phone)):
        raise ApiError(409, "PHONE_TAKEN", "That number already has an account. Log in instead.")

    accounts_here = db.scalar(select(func.count()).select_from(User)
                              .where(User.device_install_id == body.device_install_id))
    if accounts_here >= MAX_ACCOUNTS_PER_DEVICE:
        raise ApiError(409, "DEVICE_LIMIT",
                       f"This phone already has {MAX_ACCOUNTS_PER_DEVICE} accounts.")

    user = User(name=body.name, phone=body.phone, password_hash=hash_password(body.password),
                device_install_id=body.device_install_id)
    db.add(user)
    try:
        db.flush()
    except IntegrityError:
        # Two registrations with the same phone at the same moment: the check above passed
        # for both, and the UNIQUE on phone stopped the second one here.
        db.rollback()
        raise ApiError(409, "PHONE_TAKEN", "That number already has an account. Log in instead.")
    return sign_in(db, user)


@router.post("/auth/login", response_model=AuthOut)
def login(body: LoginIn, db: Session = Depends(get_db)) -> AuthOut:
    user = db.scalar(select(User).where(User.phone == body.phone))
    # The same answer for an unknown phone and a wrong password, so login can't be used to
    # find out which numbers have accounts.
    if user is None or not check_password(body.password, user.password_hash):
        raise ApiError(401, "WRONG_CREDENTIALS", "Wrong number or password.")
    # Checked after the password, so only the account's owner learns it's suspended
    if user.status == UserStatus.SUSPENDED:
        raise suspended_error(user)
    return sign_in(db, user)


@router.post("/auth/logout", status_code=204)
def logout(token: Token = Depends(current_token), db: Session = Depends(get_db)) -> Response:
    db.delete(token)
    db.commit()
    return Response(status_code=204)


@router.get("/me", response_model=UserOut)
def me(user: User = Depends(current_user)) -> User:
    return user

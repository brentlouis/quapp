"""Dependencies endpoints ask for, to find out who's calling.

    def my_endpoint(user: User = Depends(current_user)): …

FastAPI runs current_user before the endpoint; if it raises, the endpoint never runs.
"""

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import delete
from sqlalchemy.orm import Session

from app.database import get_db
from app.enums import UserStatus
from app.errors import ApiError
from app.models import Queue, Token, User
from app.schemas import UserOut

# Reads "Authorization: Bearer <token>". auto_error=False: a missing header gives our own
# NOT_SIGNED_IN error instead of FastAPI's default 403.
bearer = HTTPBearer(auto_error=False)


def suspended_error(user: User) -> ApiError:
    """403 SUSPENDED with the reason and date, so the app can open Account suspended."""
    out = UserOut.model_validate(user).model_dump(mode="json")
    return ApiError(403, "SUSPENDED", "This account is suspended.",
                    suspended_reason=out["suspended_reason"], suspended_at=out["suspended_at"])


def current_token(credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
                  db: Session = Depends(get_db)) -> Token:
    """The caller's token row. 401 without a token or with one that was logged out."""
    if credentials is None:
        raise ApiError(401, "NOT_SIGNED_IN", "Sign in first.")
    token = db.get(Token, credentials.credentials)
    if token is None:
        raise ApiError(401, "NOT_SIGNED_IN", "Your session has ended. Sign in again.")
    return token


def current_user(token: Token = Depends(current_token), db: Session = Depends(get_db)) -> User:
    """The signed-in user. A user suspended while signed in is signed out here, everywhere."""
    user = token.user
    if user.status == UserStatus.SUSPENDED:
        db.execute(delete(Token).where(Token.user_id == user.id))
        db.commit()
        raise suspended_error(user)
    return user


def queue_or_404(db: Session, queue_id: str) -> Queue:
    queue = db.get(Queue, queue_id)
    if queue is None:
        raise ApiError(404, "QUEUE_NOT_FOUND", "That queue doesn't exist.")
    return queue


def owned_queue(queue_id: str, user: User = Depends(current_user),
                db: Session = Depends(get_db)) -> Queue:
    """For organizer-only endpoints: the queue in the path, if the caller runs it.
    `queue_id` comes from the path (/queues/{queue_id}/pause)."""
    queue = queue_or_404(db, queue_id)
    if queue.organizer_id != user.id:
        raise ApiError(403, "NOT_OWNER", "Only the queue's organizer can do that.")
    return queue

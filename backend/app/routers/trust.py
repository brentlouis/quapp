"""Trust and safety from the app's side: reporting a queue, and asking to be verified
(BACKEND.md phase 7; DECISIONS.md "Organizer verification: optional, checked by phone, by hand").
The admin's side, deciding on them, is the admin page (phase 8).
"""

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app import timeutil
from app.database import get_db
from app.deps import current_user, queue_or_404
from app.enums import VerificationStatus
from app.errors import ApiError, documented
from app.models import Report, User, VerificationRequest
from app.schemas import ReportIn, ReportOut, VerificationIn, VerificationOut

router = APIRouter()


@router.post("/queues/{queue_id}/reports", response_model=ReportOut, status_code=201,
             responses=documented(401, 403, 404, 409, 422))
def report(queue_id: str, body: ReportIn, user: User = Depends(current_user),
           db: Session = Depends(get_db)) -> Report:
    """Report a queue. It goes to the admin page; the organizer never learns who reported.
    One report per person per queue, so nobody can flood a queue."""
    queue = queue_or_404(db, queue_id)
    already = db.scalar(select(Report.id).where(Report.queue_id == queue.id,
                                                Report.reporter_id == user.id))
    if already:
        raise ApiError(409, "ALREADY_REPORTED", "You've already reported this queue. Thank you.")
    report = Report(queue_id=queue.id, reporter_id=user.id, reason=body.reason,
                    details=body.details, created_at=timeutil.now())
    db.add(report)
    try:
        db.commit()
    except IntegrityError:
        # Two reports at the same moment: the UNIQUE (queue_id, reporter_id) stopped this one
        db.rollback()
        raise ApiError(409, "ALREADY_REPORTED", "You've already reported this queue. Thank you.")
    return report


@router.post("/me/verification", response_model=VerificationOut, status_code=201,
             responses=documented(401, 403, 409, 422))
def ask_to_be_verified(body: VerificationIn, user: User = Depends(current_user),
                       db: Session = Depends(get_db)) -> VerificationRequest:
    """The organizer asks for the badge; the admin calls the office number to check. The
    account shows as pending until the admin decides. After a rejection or a revoke they may
    ask again."""
    if user.verification_status == VerificationStatus.VERIFIED:
        raise ApiError(409, "ALREADY_VERIFIED", "You're already verified.")
    if user.verification_status == VerificationStatus.PENDING:
        raise ApiError(409, "VERIFICATION_PENDING",
                       "Your request is being checked. We'll call the office number you gave.")
    request = VerificationRequest(user_id=user.id, **body.model_dump(),
                                  status=VerificationStatus.PENDING, created_at=timeutil.now())
    db.add(request)
    user.verification_status = VerificationStatus.PENDING
    db.commit()
    return request


@router.get("/me/verification", response_model=VerificationOut | None,
            responses=documented(401, 403))
def my_verification(user: User = Depends(current_user),
                    db: Session = Depends(get_db)) -> VerificationRequest | None:
    """The latest request, for Profile's pending card; null if they never asked."""
    return db.scalar(select(VerificationRequest).where(VerificationRequest.user_id == user.id)
                     .order_by(VerificationRequest.created_at.desc()).limit(1))

"""The admin web page (BACKEND.md phase 8): verification requests, reports, and suspending or
revoking accounts. Server-rendered HTML with Jinja2, plain forms, no JavaScript.

Signing in uses the same tokens table as the app, but the token travels in a cookie, because
this is a browser page. The cookie is httponly (page scripts can't read it) and SameSite=Lax (a
form on another site can't submit with it attached), which is what protects these forms from
cross-site request forgery.

Every form is a POST that redirects back to its page (post/redirect/get), so refreshing the page
never submits a form twice.
"""

from collections import defaultdict
from pathlib import Path

from fastapi import APIRouter, Depends, Form, HTTPException, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy import delete, select
from sqlalchemy.orm import Session, selectinload

from app import timeutil
from app.database import get_db
from app.enums import RemovalReason, UserStatus, VerificationStatus
from app.models import Queue, Report, Ticket, TicketRemoval, Token, User, VerificationRequest
from app.schemas import normalize_phone
from app.security import check_password, new_token

router = APIRouter(prefix="/admin", include_in_schema=False)  # not part of the app's API
templates = Jinja2Templates(directory=Path(__file__).parent.parent / "templates")
templates.env.filters["manila"] = lambda moment: (
    moment.astimezone(timeutil.MANILA).strftime("%b %d, %I:%M %p") if moment else "")

# The labels the app shows for each organization type (strings.xml, org_type_*)
templates.env.globals["ORG_TYPES"] = {
    "LGU_OFFICE": "LGU office", "BARANGAY": "Barangay", "HEALTH": "Health center or clinic",
    "SCHOOL": "School", "COMMUNITY_GROUP": "Community group", "OTHER": "Other",
}

COOKIE = "quapp_admin"


# ---- Signing in ---------------------------------------------------------------------

def admin_user(request: Request, db: Session = Depends(get_db)) -> User:
    """The signed-in admin, or a redirect to the login page. Suspended or non-admin accounts
    don't get in even with a valid token."""
    token = db.get(Token, request.cookies.get(COOKIE, ""))
    user = token.user if token else None
    if user is None or not user.is_admin or user.status != UserStatus.ACTIVE:
        # An HTTPException with a Location header is how a dependency can redirect
        raise HTTPException(status_code=303, headers={"Location": "/admin/login"})
    return user


def back_to(path: str, message: str = "") -> RedirectResponse:
    """303 See Other: the browser follows it with a GET, so a refresh won't re-send the form."""
    url = f"{path}?done={message}" if message else path
    return RedirectResponse(url, status_code=303)


@router.get("/login", response_class=HTMLResponse)
def login_page(request: Request, error: str = ""):
    return templates.TemplateResponse(request, "admin/login.html", {"error": error})


@router.post("/login")
def login(phone: str = Form(...), password: str = Form(...), db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(User.phone == normalize_phone(phone)))
    if (user is None or not check_password(password, user.password_hash)
            or not user.is_admin or user.status != UserStatus.ACTIVE):
        return RedirectResponse("/admin/login?error=1", status_code=303)
    token = Token(token=new_token(), user_id=user.id)
    db.add(token)
    db.commit()
    response = back_to("/admin/verifications")
    response.set_cookie(COOKIE, token.token, httponly=True, samesite="lax", max_age=12 * 3600)
    return response


@router.post("/logout")
def logout(request: Request, db: Session = Depends(get_db)):
    db.execute(delete(Token).where(Token.token == request.cookies.get(COOKIE, "")))
    db.commit()
    response = RedirectResponse("/admin/login", status_code=303)
    response.delete_cookie(COOKIE)
    return response


@router.get("")
def home(admin: User = Depends(admin_user)):
    return back_to("/admin/verifications")


# ---- Verification requests ----------------------------------------------------------------

@router.get("/verifications", response_class=HTMLResponse)
def verifications(request: Request, done: str = "", admin: User = Depends(admin_user),
                  db: Session = Depends(get_db)):
    pending = db.scalars(select(VerificationRequest)
                         .where(VerificationRequest.status == VerificationStatus.PENDING)
                         .order_by(VerificationRequest.created_at)).all()
    decided = db.scalars(select(VerificationRequest)
                         .where(VerificationRequest.status != VerificationStatus.PENDING)
                         .order_by(VerificationRequest.decided_at.desc()).limit(20)).all()
    users = {u.id: u for u in db.scalars(select(User).where(
        User.id.in_({r.user_id for r in [*pending, *decided]})))}
    return templates.TemplateResponse(request, "admin/verifications.html", {
        "admin": admin, "done": done, "pending": pending, "decided": decided, "users": users})


def decide(db: Session, request_id: str, admin: User, verdict: VerificationStatus,
           note: str) -> VerificationRequest | None:
    """Approve or reject a pending request. The answers are wiped either way; only the
    result and the note stay (MODELS.md "VerificationRequest")."""
    verification = db.get(VerificationRequest, request_id)
    if verification is None or verification.status != VerificationStatus.PENDING:
        return None
    user = db.get(User, verification.user_id)
    user.verification_status = verdict
    if verdict == VerificationStatus.VERIFIED:
        # "Posting as Brgy. Poblacion Council" from now on
        user.organization_name = verification.organization_name
    verification.status = verdict
    verification.admin_note = note.strip() or None
    verification.decided_at = timeutil.now()
    verification.decided_by = admin.id
    verification.organization_name = None
    verification.organization_type = None
    verification.position = None
    verification.office_phone = None
    db.commit()
    return verification


@router.post("/verifications/{request_id}/approve")
def approve(request_id: str, note: str = Form(""), admin: User = Depends(admin_user),
            db: Session = Depends(get_db)):
    done = decide(db, request_id, admin, VerificationStatus.VERIFIED, note)
    return back_to("/admin/verifications", "approved" if done else "already-decided")


@router.post("/verifications/{request_id}/reject")
def reject(request_id: str, note: str = Form(""), admin: User = Depends(admin_user),
           db: Session = Depends(get_db)):
    done = decide(db, request_id, admin, VerificationStatus.REJECTED, note)
    return back_to("/admin/verifications", "rejected" if done else "already-decided")


# ---- Reports --------------------------------------------------------------------------

@router.get("/reports", response_class=HTMLResponse)
def reports(request: Request, done: str = "", admin: User = Depends(admin_user),
            db: Session = Depends(get_db)):
    """Reports grouped by queue, queues with unhandled reports first. Prank removals from the
    console are listed too: DECISIONS.md says they go to the admin."""
    rows = db.scalars(select(Report).order_by(Report.created_at.desc())).all()
    queues = {q.id: q for q in db.scalars(select(Queue).options(selectinload(Queue.organizer))
                                          .where(Queue.id.in_({r.queue_id for r in rows})))}
    grouped: dict[str, list[Report]] = defaultdict(list)
    for report in rows:
        grouped[report.queue_id].append(report)
    groups = sorted(grouped.items(), key=lambda item: (
        all(r.handled_at for r in item[1]),        # unhandled first
        -len(item[1])))                            # then the most reported
    pranks = db.execute(
        select(TicketRemoval, Ticket, Queue)
        .join(Ticket, Ticket.id == TicketRemoval.ticket_id)
        .join(Queue, Queue.id == TicketRemoval.queue_id)
        .where(TicketRemoval.reason == RemovalReason.PRANK)
        .order_by(TicketRemoval.created_at.desc()).limit(50)).all()
    return templates.TemplateResponse(request, "admin/reports.html", {
        "admin": admin, "done": done, "groups": groups, "queues": queues, "pranks": pranks})


@router.post("/reports/{queue_id}/handled")
def mark_handled(queue_id: str, admin: User = Depends(admin_user),
                 db: Session = Depends(get_db)):
    for report in db.scalars(select(Report).where(Report.queue_id == queue_id,
                                                  Report.handled_at.is_(None))):
        report.handled_at = timeutil.now()
    db.commit()
    return back_to("/admin/reports", "handled")


# ---- Users --------------------------------------------------------------------------

@router.get("/users", response_class=HTMLResponse)
def users(request: Request, phone: str = "", done: str = "", admin: User = Depends(admin_user),
          db: Session = Depends(get_db)):
    """Find someone by phone, to suspend, unsuspend or revoke their badge. With no search,
    the suspended accounts are listed."""
    if phone.strip():
        found = db.scalars(select(User).where(User.phone.contains(normalize_phone(phone)))
                           .order_by(User.name).limit(20)).all()
    else:
        found = db.scalars(select(User).where(User.status == UserStatus.SUSPENDED)
                           .order_by(User.suspended_at.desc())).all()
    return templates.TemplateResponse(request, "admin/users.html", {
        "admin": admin, "done": done, "phone": phone, "found": found})


def target(db: Session, user_id: str, admin: User) -> User | None:
    """The account an action is on. Admins can't act on themselves or other admins, so nobody
    locks the admin page out by accident."""
    user = db.get(User, user_id)
    return None if user is None or user.is_admin or user.id == admin.id else user


@router.post("/users/{user_id}/suspend")
def suspend(user_id: str, reason: str = Form(...), admin: User = Depends(admin_user),
            db: Session = Depends(get_db)):
    """Suspended accounts can't log in, and every session they have ends now: their tokens
    are deleted, so the app's next call gets SUSPENDED (deps.current_user)."""
    user = target(db, user_id, admin)
    if user is None or not reason.strip():
        return back_to("/admin/users", "not-allowed")
    user.status = UserStatus.SUSPENDED
    user.suspended_reason = reason.strip()
    user.suspended_at = timeutil.now()
    db.execute(delete(Token).where(Token.user_id == user.id))
    db.commit()
    return back_to("/admin/users", "suspended")


@router.post("/users/{user_id}/unsuspend")
def unsuspend(user_id: str, admin: User = Depends(admin_user), db: Session = Depends(get_db)):
    user = target(db, user_id, admin)
    if user is None:
        return back_to("/admin/users", "not-allowed")
    user.status = UserStatus.ACTIVE
    user.suspended_reason = None
    user.suspended_at = None
    db.commit()
    return back_to("/admin/users", "unsuspended")


@router.post("/users/{user_id}/revoke")
def revoke(user_id: str, admin: User = Depends(admin_user), db: Session = Depends(get_db)):
    """Takes the badge away. Their queues show the account's own name again, with the
    unverified note, and the one-live-queue limit applies."""
    user = target(db, user_id, admin)
    if user is None or user.verification_status != VerificationStatus.VERIFIED:
        return back_to("/admin/users", "not-allowed")
    user.verification_status = VerificationStatus.REVOKED
    user.organization_name = None
    db.commit()
    return back_to("/admin/users", "revoked")

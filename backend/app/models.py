"""The database tables, column by column from MODELS.md "Database (PostgreSQL)".

Only what can't be counted from other rows is stored. People waiting, now serving, positions,
waits, stats and the no-show cooldown are computed per request (services/, later steps).

The CHECK constraints and indexes below are the database's own copy of the contract's rules:
even a bug in an endpoint can't save a 51-character short description or two live tickets for
one person in one queue.
"""

import re
from datetime import date, datetime, time
from enum import StrEnum
from uuid import uuid4

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
    Double,
    Enum,
    ForeignKey,
    Index,
    Integer,
    String,
    Text,
    Time,
    UniqueConstraint,
    false,
    text,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app import timeutil
from app.database import Base
from app.enums import (
    Category,
    OrganizationType,
    QueueStatus,
    RemovalReason,
    ReportReason,
    TicketStatus,
    UserStatus,
    VerificationStatus,
)


def new_id() -> str:
    """IDs are strings (MODELS.md): a random uuid4, made by the server."""
    return str(uuid4())


def db_enum(enum_class: type[StrEnum]) -> Enum:
    """A VARCHAR limited to the enum's values by a CHECK (MODELS.md "Database", Types).

    native_enum=False keeps it a plain VARCHAR rather than a Postgres ENUM type, which is
    awkward to add a value to later. The CHECK is named after the enum ("ck_queues_queue_status").
    """
    name = re.sub(r"(?<!^)(?=[A-Z])", "_", enum_class.__name__).lower()
    return Enum(enum_class, name=name, native_enum=False, create_constraint=True)


# Moments in time: stored as TIMESTAMPTZ (Postgres keeps UTC, the API shows +08:00).
Moment = DateTime(timezone=True)


class User(Base):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_id)
    name: Mapped[str] = mapped_column(Text)
    phone: Mapped[str] = mapped_column(Text, unique=True)
    # bcrypt; never leaves the server (schemas.py won't have it)
    password_hash: Mapped[str] = mapped_column(Text)
    status: Mapped[UserStatus] = mapped_column(db_enum(UserStatus), default=UserStatus.ACTIVE)
    suspended_reason: Mapped[str | None] = mapped_column(Text)
    suspended_at: Mapped[datetime | None] = mapped_column(Moment)
    verification_status: Mapped[VerificationStatus] = mapped_column(
        db_enum(VerificationStatus), default=VerificationStatus.NONE)
    # Set when verified: "Posting as Brgy. Poblacion Council"
    organization_name: Mapped[str | None] = mapped_column(Text)
    # The install that registered the account; at most 2 accounts per value (checked in code)
    device_install_id: Mapped[str] = mapped_column(Text, index=True)
    # False until an SMS gateway exists
    phone_verified: Mapped[bool] = mapped_column(Boolean, default=False, server_default=false())
    # Who can open /admin
    is_admin: Mapped[bool] = mapped_column(Boolean, default=False, server_default=false())
    created_at: Mapped[datetime] = mapped_column(Moment, default=timeutil.now)


class Token(Base):
    """A signed-in session. Logout deletes one; suspension deletes all of a user's."""

    __tablename__ = "tokens"

    token: Mapped[str] = mapped_column(String, primary_key=True)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    created_at: Mapped[datetime] = mapped_column(Moment, default=timeutil.now)

    user: Mapped[User] = relationship()


class Queue(Base):
    __tablename__ = "queues"
    __table_args__ = (
        CheckConstraint("length(short_description) <= 50", name="short_description"),
        CheckConstraint("end_date >= start_date", name="dates"),
        CheckConstraint("closes_at > opens_at", name="hours"),
        CheckConstraint("join_radius_meters IN (0, 500, 1000, 2000, 5000)", name="radius"),
        # MODELS.md: the radius is 0 exactly when the proximity check is off
        CheckConstraint("(join_radius_meters = 0) = (NOT proximity_check_enabled)",
                        name="radius_matches_check"),
        # Browse filters by town and hides closed queues
        Index(None, "municipality", "status"),
    )

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_id)
    organizer_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    name: Mapped[str] = mapped_column(Text)
    category: Mapped[Category] = mapped_column(db_enum(Category))
    short_description: Mapped[str] = mapped_column(Text)
    details: Mapped[str | None] = mapped_column(Text)
    bring: Mapped[str | None] = mapped_column(Text)
    venue: Mapped[str] = mapped_column(Text)
    municipality: Mapped[str] = mapped_column(Text)
    latitude: Mapped[float] = mapped_column(Double)
    longitude: Mapped[float] = mapped_column(Double)
    start_date: Mapped[date] = mapped_column(Date)
    end_date: Mapped[date] = mapped_column(Date)
    opens_at: Mapped[time] = mapped_column(Time)
    closes_at: Mapped[time] = mapped_column(Time)
    status: Mapped[QueueStatus] = mapped_column(db_enum(QueueStatus))
    paused_at: Mapped[datetime | None] = mapped_column(Moment)
    closed_at: Mapped[datetime | None] = mapped_column(Moment)
    grace_period_enabled: Mapped[bool] = mapped_column(Boolean, default=False, server_default=false())
    no_show_cooldown_enabled: Mapped[bool] = mapped_column(Boolean, default=False, server_default=false())
    proximity_check_enabled: Mapped[bool] = mapped_column(Boolean, default=False, server_default=false())
    join_radius_meters: Mapped[int] = mapped_column(Integer, default=0, server_default=text("0"))
    # Hands out #1, #2, … and never reuses one (joining locks the row first; BACKEND.md 4.1)
    next_ticket_number: Mapped[int] = mapped_column(Integer, default=1, server_default=text("1"))
    created_at: Mapped[datetime] = mapped_column(Moment, default=timeutil.now)

    organizer: Mapped[User] = relationship()


class Ticket(Base):
    __tablename__ = "tickets"
    __table_args__ = (
        UniqueConstraint("queue_id", "ticket_number"),
        # One live ticket per person per queue. Partial: only WAITING and CALLED rows count,
        # so an old SERVED ticket doesn't stop someone joining the same queue again.
        Index("uq_tickets_one_live_per_person", "queue_id", "user_id", unique=True,
              postgresql_where=text("status IN ('WAITING', 'CALLED')")),
        # A walk-in has no account; everyone else has one
        CheckConstraint("walk_in = (user_id IS NULL)", name="walk_in_has_no_user"),
        # A reason exactly when removed
        CheckConstraint("(status = 'REMOVED') = (removal_reason IS NOT NULL)",
                        name="removed_has_reason"),
        # The line in call order, and positions (BACKEND.md 4.2, 5.1)
        Index(None, "queue_id", "status", "line_order"),
        # My tickets
        Index(None, "user_id", "status"),
    )

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_id)
    queue_id: Mapped[str] = mapped_column(ForeignKey("queues.id"))
    user_id: Mapped[str | None] = mapped_column(ForeignKey("users.id"))
    holder_name: Mapped[str] = mapped_column(Text)
    holder_phone: Mapped[str | None] = mapped_column(Text)
    walk_in: Mapped[bool] = mapped_column(Boolean, default=False, server_default=false())
    ticket_number: Mapped[int] = mapped_column(Integer)
    # The call order. Starts equal to ticket_number; moving back sets it between two others
    # (45.5 to land after #45), so the ticket keeps its number (MODELS.md "Database").
    line_order: Mapped[float] = mapped_column(Double)
    # A ticket is for one day (the overlap rule)
    service_date: Mapped[date] = mapped_column(Date)
    status: Mapped[TicketStatus] = mapped_column(db_enum(TicketStatus), default=TicketStatus.WAITING)
    joined_at: Mapped[datetime] = mapped_column(Moment, default=timeutil.now)
    called_at: Mapped[datetime | None] = mapped_column(Moment)
    # When they tapped "I'm here"
    here_at: Mapped[datetime | None] = mapped_column(Moment)
    finished_at: Mapped[datetime | None] = mapped_column(Moment)
    moved_back: Mapped[bool] = mapped_column(Boolean, default=False, server_default=false())
    moved_back_at: Mapped[datetime | None] = mapped_column(Moment)
    removal_reason: Mapped[RemovalReason | None] = mapped_column(db_enum(RemovalReason))

    queue: Mapped[Queue] = relationship()
    user: Mapped[User | None] = relationship()


class TicketRemoval(Base):
    __tablename__ = "ticket_removals"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_id)
    ticket_id: Mapped[str] = mapped_column(ForeignKey("tickets.id"), unique=True)
    queue_id: Mapped[str] = mapped_column(ForeignKey("queues.id"), index=True)
    reason: Mapped[RemovalReason] = mapped_column(db_enum(RemovalReason))
    removed_by: Mapped[str] = mapped_column(ForeignKey("users.id"))
    created_at: Mapped[datetime] = mapped_column(Moment, default=timeutil.now)


class VerificationRequest(Base):
    """An organizer asking for the badge. The answers are wiped after the decision, so they're
    nullable; only the status and the admin's note stay (MODELS.md)."""

    __tablename__ = "verification_requests"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_id)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    organization_name: Mapped[str | None] = mapped_column(Text)
    organization_type: Mapped[OrganizationType | None] = mapped_column(db_enum(OrganizationType))
    position: Mapped[str | None] = mapped_column(Text)
    office_phone: Mapped[str | None] = mapped_column(Text)
    status: Mapped[VerificationStatus] = mapped_column(
        db_enum(VerificationStatus), default=VerificationStatus.PENDING)
    admin_note: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(Moment, default=timeutil.now)
    decided_at: Mapped[datetime | None] = mapped_column(Moment)
    decided_by: Mapped[str | None] = mapped_column(ForeignKey("users.id"))


class Report(Base):
    """Someone reporting a queue. The organizer never sees who (reporter_id stays server-side)."""

    __tablename__ = "reports"
    __table_args__ = (
        # One report per person per queue: nobody can flood a queue with reports
        UniqueConstraint("queue_id", "reporter_id"),
    )

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_id)
    queue_id: Mapped[str] = mapped_column(ForeignKey("queues.id"), index=True)
    reporter_id: Mapped[str] = mapped_column(ForeignKey("users.id"))
    reason: Mapped[ReportReason] = mapped_column(db_enum(ReportReason))
    details: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(Moment, default=timeutil.now)
    # Set from the admin page
    handled_at: Mapped[datetime | None] = mapped_column(Moment)

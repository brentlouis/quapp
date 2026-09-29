"""The JSON the API takes and gives, field for field from MODELS.md.

*In models are request bodies (validated before the endpoint runs); *Out models are responses.
Python field names are already snake_case, so they're the JSON names too.
"""

import re
from datetime import date, datetime, time
from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field, PlainSerializer, field_validator

from app.enums import (
    Category,
    EstimateSource,
    QueueStatus,
    RemovalReason,
    TicketStatus,
    UserStatus,
    VerificationStatus,
)
from app.security import MAX_PASSWORD_BYTES
from app.services.tickets import MORE_TIME_CHOICES
from app.timeutil import MANILA

# A moment in time, sent in Manila time: "2026-09-27T10:05:00+08:00" (MODELS.md conventions).
# Postgres hands back UTC; this converts on the way out.
Moment = Annotated[datetime, PlainSerializer(
    lambda value: value.astimezone(MANILA).isoformat(timespec="seconds"), return_type=str)]

# A time of day, sent as "08:00" (MODELS.md conventions); Pydantic's default is "08:00:00"
ClockTime = Annotated[time, PlainSerializer(lambda value: value.strftime("%H:%M"), return_type=str)]

# Philippine mobile numbers: 09 and nine more digits (Validation.java)
PH_MOBILE = re.compile(r"^09\d{9}$")
MIN_PASSWORD_LENGTH = 8


def normalize_phone(phone: str) -> str:
    """Spaces and dashes are allowed while typing ("0917 123 4567"): drop them, like
    Validation.normalizePhone does in the app."""
    return re.sub(r"[\s-]", "", phone)


class Out(BaseModel):
    """Base for responses: can be built straight from a SQLAlchemy row (User → UserOut)."""

    model_config = ConfigDict(from_attributes=True)


# ---- Users --------------------------------------------------------------------

class UserOut(Out):
    """MODELS.md "User". Server-only columns (password_hash, device_install_id, phone_verified,
    is_admin) aren't listed, so they can never be sent."""

    id: str
    name: str
    phone: str
    status: UserStatus
    suspended_reason: str | None
    suspended_at: Moment | None
    verification_status: VerificationStatus
    organization_name: str | None


# ---- Auth ---------------------------------------------------------------------

class RegisterIn(BaseModel):
    name: str = Field(min_length=1, max_length=80)
    phone: str
    password: str
    device_install_id: str = Field(min_length=1, max_length=100)

    @field_validator("name")
    @classmethod
    def name_not_blank(cls, name: str) -> str:
        name = name.strip()
        if not name:
            raise ValueError("Enter your name.")
        return name

    @field_validator("phone")
    @classmethod
    def phone_is_ph_mobile(cls, phone: str) -> str:
        phone = normalize_phone(phone)
        if not PH_MOBILE.match(phone):
            raise ValueError("Enter a mobile number like 0917 123 4567.")
        return phone

    @field_validator("password")
    @classmethod
    def password_length(cls, password: str) -> str:
        if len(password) < MIN_PASSWORD_LENGTH:
            raise ValueError(f"Use at least {MIN_PASSWORD_LENGTH} characters.")
        if len(password.encode()) > MAX_PASSWORD_BYTES:
            raise ValueError(f"Use at most {MAX_PASSWORD_BYTES} characters.")
        return password


class LoginIn(BaseModel):
    phone: str
    password: str

    @field_validator("phone")
    @classmethod
    def normalize(cls, phone: str) -> str:
        # No format check here: a badly typed phone just doesn't match an account (401)
        return normalize_phone(phone)


class AuthOut(BaseModel):
    """Register and login both answer with a token for the Authorization header, and the user."""

    token: str
    user: UserOut


# ---- Queues -------------------------------------------------------------------

class QueueOut(Out):
    """MODELS.md "Queue", in its order. The last five aren't columns: routers/queues.py fills
    them in (organizer from the users table, the live numbers from tickets)."""

    id: str
    organizer_id: str
    organizer_name: str
    organizer_verified: bool
    name: str
    category: Category
    short_description: str
    details: str | None
    bring: str | None
    venue: str
    municipality: str
    latitude: float
    longitude: float
    start_date: date
    end_date: date
    opens_at: ClockTime
    closes_at: ClockTime
    status: QueueStatus
    paused_at: Moment | None
    closed_at: Moment | None
    grace_period_enabled: bool
    no_show_cooldown_enabled: bool
    proximity_check_enabled: bool
    join_radius_meters: int
    people_waiting: int
    now_serving: int | None
    estimated_wait_minutes: int


def _stripped(value: str | None) -> str | None:
    """Trim spaces; an empty optional text becomes null."""
    if value is None:
        return None
    value = value.strip()
    return value or None


class QueueIn(BaseModel):
    """Create Queue. Each field on its own is checked here; how they fit together (dates in
    order, the radius matching the check) is checked in routers/queues.py, so the error can
    name the field to fix."""

    name: str = Field(min_length=1, max_length=80)
    category: Category
    short_description: str = Field(min_length=1, max_length=50)
    details: str | None = Field(default=None, max_length=1000)
    bring: str | None = Field(default=None, max_length=200)
    venue: str = Field(min_length=1, max_length=120)
    municipality: str = Field(min_length=1, max_length=60)
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    start_date: date
    end_date: date
    opens_at: time
    closes_at: time
    grace_period_enabled: bool = False
    no_show_cooldown_enabled: bool = False
    proximity_check_enabled: bool = False
    join_radius_meters: int = 0

    @field_validator("name", "short_description", "venue", "municipality")
    @classmethod
    def required_text(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("Fill this in.")
        return value

    @field_validator("details", "bring")
    @classmethod
    def optional_text(cls, value: str | None) -> str | None:
        return _stripped(value)


class QueuePatch(BaseModel):
    """Edit Queue: every field optional; only the ones sent change."""

    name: str | None = Field(default=None, min_length=1, max_length=80)
    category: Category | None = None
    short_description: str | None = Field(default=None, min_length=1, max_length=50)
    details: str | None = Field(default=None, max_length=1000)
    bring: str | None = Field(default=None, max_length=200)
    venue: str | None = Field(default=None, min_length=1, max_length=120)
    municipality: str | None = Field(default=None, min_length=1, max_length=60)
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    start_date: date | None = None
    end_date: date | None = None
    opens_at: time | None = None
    closes_at: time | None = None
    grace_period_enabled: bool | None = None
    no_show_cooldown_enabled: bool | None = None
    proximity_check_enabled: bool | None = None
    join_radius_meters: int | None = None

    @field_validator("name", "short_description", "venue", "municipality")
    @classmethod
    def required_text(cls, value: str | None) -> str | None:
        if value is None:
            return None
        value = value.strip()
        if not value:
            raise ValueError("Fill this in.")
        return value

    @field_validator("details", "bring")
    @classmethod
    def optional_text(cls, value: str | None) -> str | None:
        return _stripped(value)


class QueueStatsOut(BaseModel):
    """MODELS.md "QueueStats": the organizer's numbers for one queue today."""

    served_today: int
    no_shows_today: int
    waiting_now: int
    average_service_minutes: float
    service_sample_count: int
    projected_wait_minutes: int
    estimate_source: EstimateSource
    model_samples: int


class ExtendIn(BaseModel):
    """Extend closing time: the new closing time for today (the queue's last day)."""

    closes_at: time


# ---- Tickets ------------------------------------------------------------------

class JoinIn(BaseModel):
    """Where the queuer is, read once when they tap Join. Only needed when the queue checks
    proximity; nothing is kept after the check (ProximityCheck.java)."""

    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)


class TicketOut(BaseModel):
    """MODELS.md "Ticket", in its order. queue_name, venue, position and the wait aren't
    ticket columns; routers/tickets.py fills them in."""

    id: str
    queue_id: str
    queue_name: str
    venue: str
    holder_name: str
    holder_phone: str | None
    walk_in: bool
    ticket_number: int
    position: int
    estimated_wait_minutes: int
    status: TicketStatus
    joined_at: Moment
    called_at: Moment | None
    finished_at: Moment | None
    moved_back: bool
    moved_back_at: Moment | None
    removal_reason: RemovalReason | None


class MoveBackIn(BaseModel):
    """"I need more time": how many minutes, from the sheet's choices."""

    minutes_needed: int

    @field_validator("minutes_needed")
    @classmethod
    def one_of_the_choices(cls, minutes: int) -> int:
        if minutes not in MORE_TIME_CHOICES:
            raise ValueError("Pick 5, 10, 15, 20, 30 or 45 minutes.")
        return minutes


# ---- The console --------------------------------------------------------------

class LineOut(BaseModel):
    """MODELS.md "Line": what the Live console shows."""

    now_serving: TicketOut | None
    now_serving_here_at: Moment | None
    now_serving_timed_out: bool
    waiting: list[TicketOut]


class WalkInIn(BaseModel):
    name: str = Field(min_length=1, max_length=80)

    @field_validator("name")
    @classmethod
    def not_blank(cls, name: str) -> str:
        name = name.strip()
        if not name:
            raise ValueError("Enter their name.")
        return name


class RemoveIn(BaseModel):
    reason: RemovalReason


class TicketRemovalOut(Out):
    """MODELS.md "TicketRemoval"."""

    id: str
    ticket_id: str
    queue_id: str
    reason: RemovalReason
    removed_by: str
    created_at: Moment

"""The JSON the API takes and gives, field for field from MODELS.md.

*In models are request bodies (validated before the endpoint runs); *Out models are responses.
Python field names are already snake_case, so they're the JSON names too.
"""

import re
from datetime import datetime
from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field, PlainSerializer, field_validator

from app.enums import UserStatus, VerificationStatus
from app.security import MAX_PASSWORD_BYTES
from app.timeutil import MANILA

# A moment in time, sent in Manila time: "2026-09-27T10:05:00+08:00" (MODELS.md conventions).
# Postgres hands back UTC; this converts on the way out.
Moment = Annotated[datetime, PlainSerializer(
    lambda value: value.astimezone(MANILA).isoformat(timespec="seconds"), return_type=str)]

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

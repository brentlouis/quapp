"""Every enum in MODELS.md "Enums", letter for letter.

StrEnum members are real strings (QueueStatus.OPEN == "OPEN"), so they go into the database
and out as JSON as their caps names, which is what the Android enums expect.
"""

from enum import StrEnum


class QueueStatus(StrEnum):
    UPCOMING = "UPCOMING"
    OPEN = "OPEN"
    PAUSED = "PAUSED"
    CLOSED = "CLOSED"


class Category(StrEnum):
    RELIEF = "RELIEF"
    MEDICAL = "MEDICAL"
    GOVERNMENT = "GOVERNMENT"
    EDUCATION = "EDUCATION"
    BILLS = "BILLS"
    IDS = "IDS"
    JOBS = "JOBS"
    OTHER = "OTHER"


class TicketStatus(StrEnum):
    WAITING = "WAITING"
    CALLED = "CALLED"
    SERVED = "SERVED"
    NO_SHOW = "NO_SHOW"
    QUEUE_CLOSED = "QUEUE_CLOSED"
    REMOVED = "REMOVED"


class RemovalReason(StrEnum):
    PRANK = "PRANK"
    DUPLICATE = "DUPLICATE"
    ASKED_TO_LEAVE = "ASKED_TO_LEAVE"


class UserStatus(StrEnum):
    ACTIVE = "ACTIVE"
    SUSPENDED = "SUSPENDED"


class VerificationStatus(StrEnum):
    NONE = "NONE"
    PENDING = "PENDING"
    VERIFIED = "VERIFIED"
    REJECTED = "REJECTED"
    REVOKED = "REVOKED"


class OrganizationType(StrEnum):
    LGU_OFFICE = "LGU_OFFICE"
    BARANGAY = "BARANGAY"
    HEALTH = "HEALTH"
    SCHOOL = "SCHOOL"
    COMMUNITY_GROUP = "COMMUNITY_GROUP"
    OTHER = "OTHER"


class ReportReason(StrEnum):
    FAKE = "FAKE"
    ASKED_FOR_MONEY = "ASKED_FOR_MONEY"
    WRONG_PLACE_OR_TIME = "WRONG_PLACE_OR_TIME"
    OTHER = "OTHER"


class EstimateSource(StrEnum):
    ROLLING_AVERAGE = "ROLLING_AVERAGE"
    MODEL = "MODEL"

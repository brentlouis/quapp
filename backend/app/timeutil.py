"""Time, in one place. The app and the API speak Asia/Manila time (MODELS.md conventions).

Every "now" and "today" in the server comes from here, never from datetime.now() or
date.today() directly: the server's clock may not be set to Manila, and tests need one
place to reason about time.
"""

from datetime import date, datetime, timedelta, timezone

# The Philippines has no daylight saving time, so Manila is always UTC+8. A fixed offset is
# exact, and it needs no timezone database (Windows doesn't ship one for Python's zoneinfo).
MANILA = timezone(timedelta(hours=8), "Asia/Manila")


def now() -> datetime:
    """This moment, timezone-aware. Postgres stores TIMESTAMPTZ values in UTC either way."""
    return datetime.now(timezone.utc)


def today() -> date:
    """Today's date in Manila. Not date.today(), which uses the server's own timezone."""
    return datetime.now(MANILA).date()

"""What the model looks at: one queue at one moment, as a row of numbers.

The same function turns a simulated moment (simulate.py) and a real one (the server, in
app/services/estimator.py) into numbers, so the model sees the same thing in training and in
use. DECISIONS.md "Wait-time estimation learns online" lists the features and why.
"""

from dataclasses import dataclass

# Kept as plain strings, in a fixed order, so this file doesn't depend on the app package and
# the one-hot columns never change places between training and use.
CATEGORIES = ["RELIEF", "MEDICAL", "GOVERNMENT", "EDUCATION", "BILLS", "IDS", "JOBS", "OTHER"]

# Times of day as four parts rather than one hour number: a straight line can't learn that
# lunch is slower than both 10 AM and 3 PM, but it can learn one number per part of the day.
DAY_PARTS = ["MORNING", "LUNCH", "AFTERNOON", "LATE"]


@dataclass
class Snapshot:
    """A queue right now, in the terms the counter staff would use."""
    rolling_average: float      # minutes per person over the last 5 served (the old estimate)
    served_last_15: int         # people called in the last 15 minutes
    in_line: int                # people waiting
    no_shows_last_hour: int
    moved_back_waiting: int     # people in line who asked for more time
    hour: int                   # 0–23, Manila time
    weekday: int                # 0 = Monday … 6 = Sunday
    category: str               # one of CATEGORIES
    minutes_open: float         # since the queue opened today
    grace_period: bool
    proximity_check: bool


def day_part(hour: int) -> str:
    if hour < 11:
        return "MORNING"
    if hour < 13:
        return "LUNCH"
    if hour < 16:
        return "AFTERNOON"
    return "LATE"


FEATURE_NAMES = (
    ["rolling_average", "served_last_15", "in_line", "no_shows_last_hour", "moved_back_waiting",
     "minutes_open", "grace_period", "proximity_check"]
    + [f"part_{p.lower()}" for p in DAY_PARTS]
    + [f"day_{d}" for d in ["mon", "tue", "wed", "thu", "fri", "sat", "sun"]]
    + [f"category_{c.lower()}" for c in CATEGORIES]
)


def to_row(s: Snapshot) -> list[float]:
    """The snapshot as numbers, in FEATURE_NAMES order. Yes/no becomes 1/0; day part, weekday
    and category become one-hot columns (1 in their own column, 0 in the others)."""
    part = day_part(s.hour)
    row = [
        s.rolling_average,
        float(s.served_last_15),
        float(s.in_line),
        float(s.no_shows_last_hour),
        float(s.moved_back_waiting),
        # Capped: after 10 hours, "how long it's been open" stops meaning anything new
        min(s.minutes_open, 600.0),
        1.0 if s.grace_period else 0.0,
        1.0 if s.proximity_check else 0.0,
    ]
    row += [1.0 if part == p else 0.0 for p in DAY_PARTS]
    row += [1.0 if s.weekday == d else 0.0 for d in range(7)]
    row += [1.0 if s.category == c else 0.0 for c in CATEGORIES]
    return row

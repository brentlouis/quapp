"""How many minutes each person takes: the number every wait in the app is built from
(MODELS.md "The estimator"): wait = minutes per person × people ahead.

For now it's the default. Step 6.1 replaces it with the rolling average of real service times,
and phase 9 with the learning model, without anything that calls it having to change.
"""

from app.models import Queue

# Before anyone has been served (MODELS.md: "the default 5 min per person is used")
DEFAULT_MINUTES_PER_PERSON = 5.0


def minutes_per_person(queue: Queue) -> float:
    return DEFAULT_MINUTES_PER_PERSON


def wait_minutes(queue: Queue, people_ahead: int) -> int:
    return round(minutes_per_person(queue) * people_ahead)

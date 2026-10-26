"""Simulated queue days, to train and test the wait-time model on (BACKEND.md step 9.1).

There's no real usage yet, so these days stand in for it. Every rule below is made up, but
each is the kind of thing a real counter does, so a model that learns them would learn the real
ones too. The write-up says so (DECISIONS.md "Wait-time estimation learns online").

How a simulated day works:
- People arrive through the day, most around opening (a Poisson process: random gaps whose
  average depends on the hour).
- The counter calls them one at a time. How long each takes depends on:
  the category (a medical check-up is slower than paying a bill), the part of the day (slow at
  lunch, a bit slower late), the weekday (Monday is busier), how long the line is (staff hurry
  when it's long), and whether the grace period is on ("I'm here" adds a little).
  On top of that, random variation (log-normal noise: mostly near the average, sometimes much
  longer, never negative).
- Some people don't come: a no-show costs the counter the time spent waiting for them.
  Prank joins never come at all; the proximity check stops most of them at Join.
- Some ask for more time and move back a few places.
- The counter takes one long break around lunch, which the server ignores (over 30 minutes).

Each time someone is called, it writes one event: what the queue looked like at that moment
(features.Snapshot) and how long the previous person actually took. That's exactly what the
server has at Call next (app/services/estimator.py).

Run it to see what it produces:
    .venv\\Scripts\\python -m ml.simulate
"""

import math
import random
from collections import deque
from dataclasses import dataclass

from ml.features import CATEGORIES, Snapshot

# ---- The rules ------------------------------------------------------------------------

# Average minutes per person by category, on a calm Tuesday morning
BASE_MINUTES = {"RELIEF": 3.0, "MEDICAL": 6.5, "GOVERNMENT": 4.5, "EDUCATION": 5.0,
                "BILLS": 2.0, "IDS": 7.0, "JOBS": 8.0, "OTHER": 4.0}

# Slower at lunch (fewer staff), a bit slower late in the day (tired)
PART_FACTOR = {"MORNING": 1.0, "LUNCH": 1.35, "AFTERNOON": 1.05, "LATE": 1.15}

# Monday is the busiest and slowest day; Saturday offices run a lighter crew
WEEKDAY_FACTOR = [1.15, 1.0, 1.0, 1.0, 1.05, 1.1, 1.0]

# How many people arrive per hour at the busiest hour (opening), by category
ARRIVALS_PER_HOUR = {"RELIEF": 30, "MEDICAL": 12, "GOVERNMENT": 16, "EDUCATION": 14,
                     "BILLS": 28, "IDS": 10, "JOBS": 9, "OTHER": 15}

NO_SHOW_CHANCE = 0.06
PRANK_CHANCE = 0.03            # of joins; proximity stops most of them
PROXIMITY_STOPS_PRANKS = 0.8
MOVE_BACK_CHANCE = 0.05
MOVE_BACK_PLACES = 4
NOISE = 0.30                   # spread of the log-normal noise
GRACE_EXTRA_MINUTES = 0.4      # waiting for "I'm here"
NO_SHOW_WAIT_GRACE = 3.0       # with the grace period the counter waits the full 3 minutes
NO_SHOW_WAIT = 1.5             # without it, staff call twice and move on
BREAK_RANGE = (35, 50)         # minutes, once a day around lunch
OPENS_AT = 8 * 60              # minutes after midnight
CLOSES_AT = 17 * 60


def minutes_for(category: str, hour: int, weekday: int, in_line: int, grace: bool,
                rng: random.Random) -> float:
    """How long one person takes at the counter."""
    from ml.features import day_part
    minutes = BASE_MINUTES[category] * PART_FACTOR[day_part(hour)] * WEEKDAY_FACTOR[weekday]
    # Up to 20% faster when 25 or more are waiting
    minutes *= 1.0 - 0.2 * min(in_line, 25) / 25
    if grace:
        minutes += GRACE_EXTRA_MINUTES
    # exp(normal) is log-normal; the -NOISE²/2 keeps its average at 1
    return minutes * math.exp(rng.gauss(-NOISE * NOISE / 2, NOISE))


# ---- One day --------------------------------------------------------------------------


@dataclass
class Event:
    snapshot: Snapshot
    minutes: float        # how long the previous person took (call to call)
    day: int              # which simulated day (to keep test days apart from training days)
    queue: str            # "MEDICAL-3"


@dataclass
class Person:
    prank: bool
    no_show: bool
    moves_back: bool
    moved: bool = False


def arrivals(category: str, rng: random.Random) -> list[float]:
    """Join times, in minutes after midnight. Busiest at opening, fading through the day,
    a small bump after lunch."""
    times = []
    t = OPENS_AT - 30  # people join a little before opening
    while True:
        hour = t / 60
        shape = 1.0 if hour < 9 else 0.7 if hour < 11 else 0.4 if hour < 13 else 0.55 if hour < 15 else 0.3
        rate = ARRIVALS_PER_HOUR[category] * shape / 60  # per minute
        t += rng.expovariate(rate)
        if t >= CLOSES_AT - 45:  # no new joins in the last 45 minutes
            return times
        times.append(t)


def simulate_day(day: int, category: str, grace: bool, proximity: bool,
                 rng: random.Random, number: int = 0) -> list[Event]:
    weekday = day % 7
    joins = deque(arrivals(category, rng))
    line: list[Person] = []
    events: list[Event] = []
    gaps: deque[float] = deque(maxlen=5)          # the rolling average's window
    calls: list[float] = []                        # when each call happened
    no_shows: list[float] = []
    break_at = rng.uniform(11.5 * 60, 12.5 * 60)
    took_break = False
    t = float(OPENS_AT)
    last_call = None

    def admit(until: float) -> None:
        """Everyone who joined by `until` stands in line (or is a prank the check stopped)."""
        while joins and joins[0] <= until:
            joins.popleft()
            prank = rng.random() < PRANK_CHANCE
            if prank and proximity and rng.random() < PROXIMITY_STOPS_PRANKS:
                continue  # stopped at Join: too far away
            line.append(Person(prank=prank, no_show=prank or rng.random() < NO_SHOW_CHANCE,
                               moves_back=not prank and rng.random() < MOVE_BACK_CHANCE))

    while t < CLOSES_AT + 120:  # the line is served even after joins stop
        admit(t)
        if not line:
            if not joins:
                break
            t = joins[0]  # nobody waiting: the counter is idle until the next join
            continue
        if not took_break and t >= break_at:
            took_break = True
            t += rng.uniform(*BREAK_RANGE)
            continue

        # Calling the first in line: one event, about the person called before.
        hour = int(t // 60)
        if last_call is not None:
            gap = t - last_call
            snapshot = Snapshot(
                rolling_average=sum(gaps) / len(gaps) if gaps else 5.0,
                served_last_15=sum(1 for c in calls if c > t - 15),
                in_line=len(line),
                no_shows_last_hour=sum(1 for n in no_shows if n > t - 60),
                moved_back_waiting=sum(1 for p in line if p.moved),
                hour=hour,
                weekday=weekday,
                category=category,
                minutes_open=t - OPENS_AT,
                grace_period=grace,
                proximity_check=proximity,
            )
            if gap <= 30:  # a break isn't a service time (the server skips it too)
                events.append(Event(snapshot, gap, day, f"{category}-{number}"))
                gaps.append(gap)
        last_call = t
        calls.append(t)

        person = line.pop(0)
        if person.no_show:
            no_shows.append(t)
            t += (NO_SHOW_WAIT_GRACE if grace else NO_SHOW_WAIT) * rng.uniform(0.8, 1.2)
        elif person.moves_back and not person.moved:
            person.moved = True
            line.insert(min(MOVE_BACK_PLACES, len(line)), person)
            t += rng.uniform(0.3, 0.8)  # a quick word at the counter
        else:
            t += minutes_for(category, hour, weekday, len(line), grace, rng)
    return events


def simulate(days: int, seed: int = 7, queues_per_category: int = 1) -> list[Event]:
    """`days` days of every category, each queue with its own random checks. Same seed, same
    data, so the evaluation can be run again and get the same numbers."""
    rng = random.Random(seed)
    events = []
    for day in range(days):
        for category in CATEGORIES:
            for number in range(queues_per_category):
                events += simulate_day(day, category, grace=rng.random() < 0.7,
                                       proximity=rng.random() < 0.4, rng=rng, number=number)
    return events


if __name__ == "__main__":
    sample = simulate(days=14)
    print(f"{len(sample)} served events over 14 days, {len(CATEGORIES)} categories")
    by_cat: dict[str, list[float]] = {}
    for e in sample:
        by_cat.setdefault(e.snapshot.category, []).append(e.minutes)
    for category, minutes in by_cat.items():
        print(f"  {category:<11} {len(minutes):5} events, {sum(minutes) / len(minutes):4.1f} min average")

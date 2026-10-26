"""Which anti-prank checks are worth turning on? (BACKEND.md step 9.4, second half)

The same kind of simulated day as simulate.py, but a whole town at once: one queue per category,
all open 8 AM to 5 PM, sharing the same people. That matters because two of the rules work across
queues: an account holds one live ticket at a time (the queues' hours overlap, so the server's
HOURS_OVERLAP rule refuses a second one), and the no-show cooldown counts strikes from every
penalty queue.

Each day is run once per strategy with exactly the same people, arrival times and delays (every
person and prankster has their own random generator), so the differences come from the checks
and not from luck.

The people:
- Honest queuers arrive as in simulate.arrivals. 6% don't come (a real no-show). The rest take a
  while to answer the call: usually under a minute, but 5% had the phone away and take 2 to 10
  minutes. 10% join from home before setting out, so the proximity check refuses them; half of
  those try again 15 to 45 minutes later, from close by. Someone marked missed although they were
  coming tries again at the back of the line half the time.
- Pranksters each try 6 joins through the day, at random queues. 80% of them are far away
  (simulate.PROXIMITY_STOPS_PRANKS). A prank ticket never comes when called.

When someone is called:
- Grace period on: they must tap "I'm here" within 3 minutes. Nobody tapped, the counter has
  waited the full 3 minutes and records a no-show. Somebody tapped, the counter waits for them to
  walk up (simulate.GRACE_EXTRA_MINUTES, inside the service time).
- Grace period off: there's nothing to tap; staff call, call again and move on after about 1.5
  minutes (simulate.NO_SHOW_WAIT). Anyone slower is recorded as a no-show.
Every no-show is a strike, the server's way (app/services/tickets.cooldown_state).

What it measures, per strategy:
- detection rate: the share of prank join attempts an anti-prank check refused (proximity or
  cooldown). The one-ticket rule refuses some in every strategy; that's counted separately.
- counter minutes lost to pranks, per queue per day
- false positives: honest people refused at Join, blocked by the cooldown, or recorded as a
  no-show although they were on their way, as a share of honest people
- throughput: honest people served per counter hour, and their average wait from join to call

    .venv\\Scripts\\python -m ml.verification

Writes ml/results/verification.json for the report.
"""

import heapq
import json
import random
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

from ml.features import CATEGORIES
from ml.simulate import (BREAK_RANGE, CLOSES_AT, NO_SHOW_CHANCE, NO_SHOW_WAIT, OPENS_AT,
                         PROXIMITY_STOPS_PRANKS, arrivals, minutes_for)

RESULTS = Path(__file__).parent / "results"
DAYS = 60

# The server's rules (app/services/tickets.py; tests/test_verification.py checks they match)
GRACE_MINUTES = 3.0
STRIKES_FOR_COOLDOWN = 2
COOLDOWN_MINUTES = 30.0

# The people (made up, like simulate.py's rules; the write-up says so)
PRANKSTERS_PER_DAY = 4
PRANK_ATTEMPTS = 6
ANSWER_MEDIAN = 0.6            # minutes from the call to answering it (tap, or walking up)
ANSWER_SPREAD = 0.6            # log-normal spread
DISTRACTED_CHANCE = 0.05       # phone away: 2 to 10 minutes
DISTRACTED_RANGE = (2.0, 10.0)
HONEST_FAR_CHANCE = 0.10       # joins from home, before setting out
RETRY_WHEN_NEAR = 0.5          # of those refused for distance
RETRY_GAP = (15.0, 45.0)
REJOIN_AFTER_MISSED = 0.5      # of those recorded as missed although they were coming

STRATEGIES = {
    "none": (False, False, False),
    "grace_period": (True, False, False),
    "cooldown": (False, True, False),
    "proximity": (False, False, True),
    "grace_and_cooldown": (True, True, False),
    "all_three": (True, True, True),
}


@dataclass(frozen=True)
class Checks:
    grace: bool
    cooldown: bool
    proximity: bool


# ---- A day's people, the same for every strategy --------------------------------------


@dataclass
class Visitor:
    """One honest person joining one queue."""
    account: int
    category: str
    at: float
    far: bool
    no_show: bool
    seed: int


@dataclass
class Prankster:
    account: int
    far: bool
    attempts: list[tuple[float, str]]
    seed: int


def plan_day(day: int, seed: int) -> tuple[list[Visitor], list[Prankster]]:
    rng = random.Random(seed * 100_003 + day)
    visitors = []
    for category in CATEGORIES:
        for at in arrivals(category, rng):
            visitors.append(Visitor(len(visitors), category, at, rng.random() < HONEST_FAR_CHANCE,
                                    rng.random() < NO_SHOW_CHANCE, rng.getrandbits(32)))
    pranksters = []
    for n in range(PRANKSTERS_PER_DAY):
        attempts = sorted((rng.uniform(OPENS_AT - 30, CLOSES_AT - 45), rng.choice(CATEGORIES))
                          for _ in range(PRANK_ATTEMPTS))
        pranksters.append(Prankster(-1 - n, rng.random() < PROXIMITY_STOPS_PRANKS, attempts,
                                    rng.getrandbits(32)))
    return visitors, pranksters


def answer_minutes(rng: random.Random) -> float:
    """How long an honest person takes to answer a call."""
    minutes = ANSWER_MEDIAN * rng.lognormvariate(0.0, ANSWER_SPREAD)
    if rng.random() < DISTRACTED_CHANCE:
        minutes += rng.uniform(*DISTRACTED_RANGE)
    return minutes


def cooldown_blocks(strikes: list[float], now: float) -> bool:
    """app/services/tickets.cooldown_state: strikes oldest first, every second one starts a
    30-minute cooldown and the count starts again."""
    count, blocked_until = 0, None
    for struck_at in sorted(s for s in strikes if s <= now):
        count += 1
        if count == STRIKES_FOR_COOLDOWN:
            blocked_until = struck_at + COOLDOWN_MINUTES
            count = 0
    return blocked_until is not None and now < blocked_until


# ---- Running a day under one strategy -------------------------------------------------


@dataclass
class Tally:
    prank_attempts: int = 0
    prank_refused_one_ticket: int = 0
    prank_refused_proximity: int = 0
    prank_refused_cooldown: int = 0
    prank_called: int = 0
    prank_minutes: float = 0.0
    honest_people: int = 0
    honest_refused_far: int = 0
    honest_blocked_cooldown: int = 0
    honest_missed: int = 0          # recorded as a no-show although they were coming
    honest_served: int = 0
    honest_wait: float = 0.0
    counter_minutes: float = 0.0
    queue_days: int = 0

    def add(self, other: "Tally") -> None:
        for name in self.__dataclass_fields__:
            setattr(self, name, getattr(self, name) + getattr(other, name))


@dataclass(order=True)
class Event:
    at: float
    order: int
    kind: str = field(compare=False)           # "join", "counter", "finish"
    data: tuple = field(compare=False)


def run_day(day: int, visitors: list[Visitor], pranksters: list[Prankster],
            checks: Checks) -> Tally:
    tally = Tally(queue_days=len(CATEGORIES), honest_people=len(visitors))
    rngs = {v.account: random.Random(v.seed) for v in visitors}
    rngs.update({p.account: random.Random(p.seed) for p in pranksters})
    far_pranksters = {p.account for p in pranksters if p.far}
    break_rng = random.Random(day)
    break_at = {c: break_rng.uniform(11.5 * 60, 12.5 * 60) for c in CATEGORIES}
    took_break: set[str] = set()

    events: list[Event] = []
    order = 0

    def push(at: float, kind: str, *data) -> None:
        nonlocal order
        order += 1
        heapq.heappush(events, Event(at, order, kind, data))

    lines: dict[str, list[tuple]] = defaultdict(list)    # (account, honest, joined_at, visitor)
    idle = {c: True for c in CATEGORIES}
    live: set[int] = set()                                # accounts holding a ticket
    strikes: dict[int, list[float]] = defaultdict(list)
    opened_at = {c: None for c in CATEGORIES}

    for v in visitors:
        push(v.at, "join", v.account, True, v, v.far)
    for p in pranksters:
        for at, category in p.attempts:
            push(at, "join", p.account, False, category, p.account in far_pranksters)

    while events:
        event = heapq.heappop(events)
        t = event.at
        if event.kind == "join":
            account, honest, what, far = event.data
            category = what.category if honest else what
            if not honest:
                tally.prank_attempts += 1
            if account in live:            # HOURS_OVERLAP: one live ticket at a time
                if not honest:
                    tally.prank_refused_one_ticket += 1
                continue
            if checks.cooldown and cooldown_blocks(strikes[account], t):
                if honest:
                    tally.honest_blocked_cooldown += 1
                else:
                    tally.prank_refused_cooldown += 1
                continue
            if checks.proximity and far:
                if honest:
                    tally.honest_refused_far += 1
                    rng = rngs[account]
                    if rng.random() < RETRY_WHEN_NEAR:
                        push(t + rng.uniform(*RETRY_GAP), "join", account, True, what, False)
                else:
                    tally.prank_refused_proximity += 1
                continue
            live.add(account)
            lines[category].append((account, honest, t, what))
            if idle[category]:
                idle[category] = False
                push(max(t, OPENS_AT), "counter", category)

        elif event.kind == "counter":
            (category,) = event.data
            if opened_at[category] is None:
                opened_at[category] = t
            if category not in took_break and t >= break_at[category]:
                took_break.add(category)
                push(t + break_rng.uniform(*BREAK_RANGE), "counter", category)
                continue
            line = lines[category]
            if not line:
                idle[category] = True
                continue
            account, honest, joined_at, visitor = line.pop(0)
            # Drawn under every strategy, so each person's later draws stay the same
            patience = NO_SHOW_WAIT * rngs[account].uniform(0.8, 1.2)
            no_show_cost = GRACE_MINUTES if checks.grace else patience
            if not honest:
                tally.prank_called += 1
                tally.prank_minutes += no_show_cost
                cost, struck, rejoin = no_show_cost, True, False
            elif visitor.no_show:
                cost, struck, rejoin = no_show_cost, True, False
            else:
                rng = rngs[account]
                answer = answer_minutes(rng)
                in_time = answer < (GRACE_MINUTES if checks.grace else no_show_cost)
                if in_time:
                    serving = minutes_for(category, int(t // 60), day % 7, len(line),
                                          checks.grace, rng)
                    # Without the grace period the counter also waits for them to walk up
                    cost = serving if checks.grace else answer + serving
                    struck, rejoin = False, False
                    tally.honest_served += 1
                    tally.honest_wait += t - joined_at
                else:
                    tally.honest_missed += 1
                    cost, struck = no_show_cost, True
                    rejoin = rng.random() < REJOIN_AFTER_MISSED
            tally.counter_minutes += cost
            push(t + cost, "finish", account, struck, rejoin, honest, visitor)
            push(t + cost, "counter", category)

        else:  # finish: the ticket is settled, the account may join again
            account, struck, rejoin, honest, visitor = event.data
            live.discard(account)
            if struck:
                strikes[account].append(t)
            if rejoin and t < CLOSES_AT - 45:
                push(t, "join", account, True, visitor, False)
    return tally


# ---- The comparison -------------------------------------------------------------------


def compare(days: int = DAYS, seed: int = 11) -> dict:
    totals = {name: Tally() for name in STRATEGIES}
    for day in range(days):
        visitors, pranksters = plan_day(day, seed)
        for name, (grace, cooldown, proximity) in STRATEGIES.items():
            totals[name].add(run_day(day, visitors, pranksters, Checks(grace, cooldown, proximity)))
    return {"days": days, "queues_per_day": len(CATEGORIES),
            "pranksters_per_day": PRANKSTERS_PER_DAY, "prank_attempts_each": PRANK_ATTEMPTS,
            "strategies": {name: summary(t) for name, t in totals.items()}}


def summary(t: Tally) -> dict:
    caught = t.prank_refused_proximity + t.prank_refused_cooldown
    false_positives = t.honest_refused_far + t.honest_blocked_cooldown + t.honest_missed
    return {
        "detection_rate_percent": round(100 * caught / t.prank_attempts, 1),
        "prank_attempts": t.prank_attempts,
        "prank_refused": {"one_ticket_rule": t.prank_refused_one_ticket,
                          "proximity": t.prank_refused_proximity,
                          "cooldown": t.prank_refused_cooldown},
        "prank_tickets_called": t.prank_called,
        "counter_minutes_lost_to_pranks_per_queue_day": round(t.prank_minutes / t.queue_days, 2),
        "false_positive_percent": round(100 * false_positives / t.honest_people, 1),
        "false_positives": {"refused_too_far": t.honest_refused_far,
                            "blocked_by_cooldown": t.honest_blocked_cooldown,
                            "missed_although_coming": t.honest_missed},
        "honest_people": t.honest_people,
        "honest_served": t.honest_served,
        "served_per_counter_hour": round(60 * t.honest_served / t.counter_minutes, 2),
        "average_wait_minutes": round(t.honest_wait / t.honest_served, 1),
    }


if __name__ == "__main__":
    results = compare()
    RESULTS.mkdir(exist_ok=True)
    (RESULTS / "verification.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    print(f"{results['days']} simulated town days, {results['queues_per_day']} queues each, "
          f"{results['pranksters_per_day']} pranksters a day\n")
    print(f"{'strategy':<20}{'detected':>9}{'prank min':>10}{'false +':>9}"
          f"{'served/h':>10}{'wait':>7}")
    for name, s in results["strategies"].items():
        print(f"{name:<20}{s['detection_rate_percent']:>8}%"
              f"{s['counter_minutes_lost_to_pranks_per_queue_day']:>10}"
              f"{s['false_positive_percent']:>8}%{s['served_per_counter_hour']:>10}"
              f"{s['average_wait_minutes']:>7}")
    print(f"\nWrote {RESULTS / 'verification.json'}")

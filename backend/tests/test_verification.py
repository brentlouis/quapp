"""The anti-prank comparison (ml/verification.py, BACKEND.md step 9.4): it has to follow the
server's own rules, or its numbers say nothing about the app."""

from app.services import tickets
from ml.verification import (COOLDOWN_MINUTES, GRACE_MINUTES, STRIKES_FOR_COOLDOWN, Checks,
                             cooldown_blocks, plan_day, run_day)


def test_its_rules_are_the_servers():
    assert GRACE_MINUTES * 60 == tickets.GRACE.total_seconds()
    assert STRIKES_FOR_COOLDOWN == tickets.STRIKES_FOR_COOLDOWN
    assert COOLDOWN_MINUTES * 60 == tickets.COOLDOWN.total_seconds()


def test_the_second_strike_starts_the_cooldown_and_the_count_starts_again():
    assert not cooldown_blocks([600.0], 605.0)
    assert cooldown_blocks([600.0, 610.0], 620.0)
    assert not cooldown_blocks([600.0, 610.0], 641.0)              # 30 minutes later
    assert not cooldown_blocks([600.0, 610.0, 620.0], 650.0)       # a third strike is count 1
    assert not cooldown_blocks([600.0, 610.0], 605.0)              # not yet struck twice


def test_every_strategy_gets_the_same_people():
    first, again = plan_day(3, seed=5), plan_day(3, seed=5)
    assert [v.at for v in first[0]] == [v.at for v in again[0]]
    assert [p.attempts for p in first[1]] == [p.attempts for p in again[1]]


def test_with_no_checks_nothing_is_detected():
    visitors, pranksters = plan_day(0, seed=5)
    tally = run_day(0, visitors, pranksters, Checks(False, False, False))
    assert tally.prank_refused_proximity == tally.prank_refused_cooldown == 0
    assert tally.honest_refused_far == tally.honest_blocked_cooldown == 0


def test_proximity_only_refuses_pranksters_who_are_far():
    visitors, pranksters = plan_day(0, seed=5)
    tally = run_day(0, visitors, pranksters, Checks(False, False, True))
    far_attempts = sum(len(p.attempts) for p in pranksters if p.far)
    assert 0 < tally.prank_refused_proximity <= far_attempts


def test_the_grace_period_misses_fewer_honest_people():
    missed = {}
    for grace in (False, True):
        missed[grace] = 0
        for day in range(3):
            visitors, pranksters = plan_day(day, seed=5)
            missed[grace] += run_day(day, visitors, pranksters, Checks(grace, False, False)).honest_missed
    assert missed[True] < missed[False]

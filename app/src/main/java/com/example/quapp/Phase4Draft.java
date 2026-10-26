package com.example.quapp;

/** Presentation validation only; no eligibility, capacity, identity or permission rules. */
final class Phase4Draft {
    // UI-only saved-state budget, not a group-size or production field policy.
    static final int TEXT_LIMIT = 4000;
    private Phase4Draft() { }
    static boolean canReviewGroup(String holder, String members) {
        return holder != null && !holder.trim().isEmpty()
                && members != null && !members.trim().isEmpty();
    }
}

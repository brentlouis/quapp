package com.example.quapp;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * What the Live console shows (MODELS.md "Line"): who's at the counter, whether they confirmed
 * or ran out of time, and today's line in call order. Read from the server's JSON by Gson.
 */
public final class Line {

    @Nullable
    private final Ticket nowServing;
    @Nullable
    private final Instant nowServingHereAt;
    private final boolean nowServingTimedOut;
    private final List<Ticket> waiting;

    public Line(@Nullable Ticket nowServing, @Nullable Instant nowServingHereAt,
                boolean nowServingTimedOut, List<Ticket> waiting) {
        this.nowServing = nowServing;
        this.nowServingHereAt = nowServingHereAt;
        this.nowServingTimedOut = nowServingTimedOut;
        this.waiting = waiting;
    }

    /** The CALLED ticket; null when nobody is at the counter. */
    @Nullable
    public Ticket getNowServing() {
        return nowServing;
    }

    /** When they tapped "I'm here"; null until they do. */
    @Nullable
    public Instant getNowServingHereAt() {
        return nowServingHereAt;
    }

    /** Grace period on, 3 minutes since the call and no "I'm here". */
    public boolean isNowServingTimedOut() {
        return nowServingTimedOut;
    }

    public List<Ticket> getWaiting() {
        return waiting == null ? Collections.<Ticket>emptyList() : waiting;
    }
}

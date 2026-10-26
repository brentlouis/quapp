package com.example.quapp;

import androidx.annotation.Nullable;

import java.time.Instant;

/**
 * The no-show penalty, as the server last reported it (GET /me/cooldown, MODELS.md
 * "Cooldown"). Every {@link #NO_SHOW_LIMIT} no-shows on queues that have the penalty turned on
 * start a cooldown, during which those queues refuse joins; queues without the penalty ignore it.
 *
 * The server counts and enforces it (it can't be dodged by reinstalling); this class only keeps
 * its answer so Queue detail and the ticket outcomes can say so before a join is refused.
 * ActiveTicketStore refreshes it with the tickets.
 */
public final class Cooldown {

    /** The server's rule (services/tickets.py STRIKES_FOR_COOLDOWN and COOLDOWN). */
    public static final int NO_SHOW_LIMIT = 2;
    public static final int DURATION_MINUTES = 30;

    @Nullable
    private static Instant until;
    private static int strikes;

    private Cooldown() {
        // Utility class.
    }

    /** The server's latest answer. */
    static void update(QuappApi.CooldownState state) {
        until = state.until;
        strikes = state.strikes;
    }

    /** Signing out forgets the last account's penalty. */
    static void clear() {
        until = null;
        strikes = 0;
    }

    public static boolean isActive() {
        return remainingMs() > 0;
    }

    public static long remainingMs() {
        return until == null ? 0 : Math.max(0, until.toEpochMilli() - System.currentTimeMillis());
    }

    /** Rounded up, so "0 min left" never shows while the cooldown is still on. */
    public static int remainingMinutes() {
        return (int) ((remainingMs() + 59_999L) / 60_000L);
    }

    /** How many more no-shows until the next cooldown starts. */
    public static int noShowsUntilCooldown() {
        return NO_SHOW_LIMIT - strikes;
    }
}

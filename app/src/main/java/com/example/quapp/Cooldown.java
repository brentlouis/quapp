package com.example.quapp;

import android.os.SystemClock;

/**
 * The no-show penalty. Every {@link #NO_SHOW_LIMIT} no-shows on queues that have
 * the penalty turned on starts a cooldown, during which those queues refuse joins.
 * Queues without the penalty ignore it.
 *
 * In-memory for now — force-closing the app resets it. The backend will own this,
 * keyed by phone number, so it can't be dodged by reinstalling.
 */
public final class Cooldown {

    public static final int NO_SHOW_LIMIT = 2;
    public static final long DURATION_MS = 30 * 60_000L;
    public static final int DURATION_MINUTES = (int) (DURATION_MS / 60_000L);

    private static int noShowCount;
    private static long endsAt; // SystemClock.elapsedRealtime(); 0 = never started

    private Cooldown() {
        // Utility class.
    }

    public static void recordNoShow() {
        noShowCount++;
        if (noShowCount >= NO_SHOW_LIMIT) {
            noShowCount = 0;
            endsAt = SystemClock.elapsedRealtime() + DURATION_MS;
        }
    }

    public static boolean isActive() {
        return remainingMs() > 0;
    }

    public static long remainingMs() {
        return Math.max(0, endsAt - SystemClock.elapsedRealtime());
    }

    /** Rounded up, so "0 min left" never shows while the cooldown is still on. */
    public static int remainingMinutes() {
        return (int) ((remainingMs() + 59_999L) / 60_000L);
    }

    /** How many more no-shows until the next cooldown starts. */
    public static int noShowsUntilCooldown() {
        return NO_SHOW_LIMIT - noShowCount;
    }
}

package com.example.quapp;

import android.os.SystemClock;

/**
 * Holds the queuer's one active ticket and its grace-period deadline.
 *
 * The deadline lives here rather than in the Activity's timer so that leaving
 * Active Ticket doesn't pause the clock: come back after the window closed and
 * the ticket has already expired, exactly as it would on the server.
 */
public final class ActiveTicketStore {

    private static Ticket activeTicket;
    private static long graceDeadline; // SystemClock.elapsedRealtime(); 0 = not called yet

    private ActiveTicketStore() {
        // Utility class.
    }

    public static void setTicket(Ticket ticket) {
        activeTicket = ticket;
        graceDeadline = 0;
    }

    public static Ticket getTicket() {
        expireIfOverdue();
        return activeTicket;
    }

    /** True while the ticket still holds a place in line (waiting or being called). */
    public static boolean hasLiveTicket() {
        Ticket ticket = getTicket();
        return ticket != null
                && (ticket.getStatus() == Ticket.Status.WAITING
                || ticket.getStatus() == Ticket.Status.CALLED);
    }

    public static void markCalled(long gracePeriodMs) {
        if (activeTicket == null || activeTicket.getStatus() != Ticket.Status.WAITING) {
            return;
        }
        activeTicket = activeTicket.withStatus(Ticket.Status.CALLED);
        graceDeadline = SystemClock.elapsedRealtime() + gracePeriodMs;
    }

    public static long graceRemainingMs() {
        return Math.max(0, graceDeadline - SystemClock.elapsedRealtime());
    }

    public static void markServed() {
        if (activeTicket != null && activeTicket.getStatus() == Ticket.Status.CALLED) {
            activeTicket = activeTicket.withStatus(Ticket.Status.SERVED);
        }
    }

    /** The grace window ran out. Counts toward the cooldown if the queue has the penalty on. */
    public static void markNoShow() {
        if (activeTicket == null || activeTicket.getStatus() != Ticket.Status.CALLED) {
            return;
        }
        activeTicket = activeTicket.withStatus(Ticket.Status.NO_SHOW);

        Queue queue = FakeData.queueById(activeTicket.getQueueId());
        if (queue != null && queue.isNoShowPenaltyEnabled()) {
            Cooldown.recordNoShow();
        }
    }

    /** "Done" on a finished ticket: file it in history and free the slot. */
    public static void finishTicket() {
        if (activeTicket != null
                && (activeTicket.getStatus() == Ticket.Status.SERVED
                || activeTicket.getStatus() == Ticket.Status.NO_SHOW)) {
            FakeData.addToHistory(activeTicket);
        }
        clearTicket();
    }

    public static void clearTicket() {
        activeTicket = null;
        graceDeadline = 0;
    }

    private static void expireIfOverdue() {
        if (activeTicket != null
                && activeTicket.getStatus() == Ticket.Status.CALLED
                && graceRemainingMs() == 0) {
            markNoShow();
        }
    }
}

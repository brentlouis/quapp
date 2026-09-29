package com.example.quapp;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The queuer's tickets: several at once, as long as their queues' hours don't overlap
 * (MODELS.md). Each is kept by id until the queuer taps Done on its outcome.
 *
 * The line itself lives in FakeData (later, the server). Every read here asks FakeData where
 * each ticket stands now, so a Call next or a queue closing on the owner's console shows up on
 * the queuer's ticket without either screen telling the other.
 */
public final class ActiveTicketStore {

    /** Kept in the order they were joined; {@link #tickets()} sorts them for display. */
    private static final List<Ticket> held = new ArrayList<>();
    /** Tickets whose no-show has already gone toward the cooldown, so it counts once. */
    private static final Set<String> countedNoShows = new HashSet<>();

    private ActiveTicketStore() {
        // Utility class.
    }

    // ---- Reading ------------------------------------------------------------

    /**
     * Every ticket held, freshest state first: being called on top, then the rest by when their
     * queue opens, finished ones last.
     */
    public static List<Ticket> tickets() {
        refresh();
        List<Ticket> result = new ArrayList<>(held);
        Collections.sort(result, new Comparator<Ticket>() {
            @Override
            public int compare(Ticket a, Ticket b) {
                int byRank = Integer.compare(rank(a), rank(b));
                return byRank != 0 ? byRank : opensAt(a).compareTo(opensAt(b));
            }
        });
        return result;
    }

    /** Only the tickets still holding a place (waiting or called). */
    public static List<Ticket> liveTickets() {
        List<Ticket> live = new ArrayList<>();
        for (Ticket ticket : tickets()) {
            if (ticket.isLive()) {
                live.add(ticket);
            }
        }
        return live;
    }

    /** The one that matters most right now (called, else the soonest), or null. */
    @Nullable
    public static Ticket mostUrgent() {
        List<Ticket> live = liveTickets();
        return live.isEmpty() ? null : live.get(0);
    }

    @Nullable
    public static Ticket ticket(String ticketId) {
        refresh();
        int index = indexOf(ticketId);
        return index < 0 ? null : held.get(index);
    }

    /** Any ticket (live or not yet dismissed) for this queue. */
    @Nullable
    public static Ticket ticketForQueue(String queueId) {
        for (Ticket ticket : tickets()) {
            if (ticket.getQueueId().equals(queueId)) {
                return ticket;
            }
        }
        return null;
    }

    /** A live ticket whose queue is open at the same time as this one, or null. */
    @Nullable
    public static Ticket overlapping(Queue queue) {
        for (Ticket ticket : liveTickets()) {
            Queue other = FakeData.queueById(ticket.getQueueId());
            if (other != null && !other.getId().equals(queue.getId())
                    && TicketRules.hoursOverlap(other, queue, Format.today())) {
                return ticket;
            }
        }
        return null;
    }

    /**
     * Whether this ticket is one of the queuer's. Doesn't refresh from the line, so FakeData can
     * ask it in the middle of calling someone.
     */
    public static boolean holds(String ticketId) {
        return indexOf(ticketId) >= 0;
    }

    public static boolean hasLiveTicket() {
        return !liveTickets().isEmpty();
    }

    /** When the grace period for a called ticket ends: called_at + 3 min (MODELS.md). */
    public static long graceRemainingMs(Ticket ticket) {
        if (ticket.getCalledAt() == null) {
            return 0;
        }
        long deadline = ticket.getCalledAt().toEpochMilli() + CalledActivity.GRACE_PERIOD_MS;
        return Math.max(0, deadline - System.currentTimeMillis());
    }

    // ---- Changing -----------------------------------------------------------

    public static void add(Ticket ticket) {
        held.add(ticket);
    }

    /** Demo hook: the counter calls this ticket (see FakeData.callTicket). */
    public static void markCalled(String ticketId) {
        Ticket ticket = ticket(ticketId);
        if (ticket != null && ticket.getStatus() == Ticket.Status.WAITING) {
            FakeData.callTicket(ticket.getQueueId(), ticketId);
        }
    }

    /** "I'm here": served, as far as the queuer is concerned; the console sees it confirmed. */
    public static void markServed(String ticketId) {
        Ticket ticket = ticket(ticketId);
        if (ticket != null && ticket.getStatus() == Ticket.Status.CALLED) {
            FakeData.confirmArrival(ticket.getQueueId(), ticketId);
            replace(ticket.withStatus(Ticket.Status.SERVED));
        }
    }

    /** The grace window ran out. Counts toward the cooldown if the queue has the penalty on. */
    public static void markNoShow(String ticketId) {
        Ticket ticket = ticket(ticketId);
        if (ticket != null && ticket.getStatus() == Ticket.Status.CALLED) {
            FakeData.releaseCalled(ticket.getQueueId(), ticketId);
            refresh();
        }
    }

    /**
     * "I need more time": moves the ticket back by as many places as the estimator says the
     * minutes are worth. Returns the moved ticket, or null if it couldn't move.
     */
    @Nullable
    public static Ticket moveBack(String ticketId, int places) {
        Ticket ticket = ticket(ticketId);
        if (ticket == null || !ticket.isLive() || ticket.isMovedBack() || places <= 0) {
            return null;
        }
        Ticket moved = FakeData.moveBack(ticket.getQueueId(), ticketId, places);
        if (moved != null) {
            replace(moved);
        }
        return moved;
    }

    /** Leave queue: gives the place up for good. Not filed in history. */
    public static void leave(String ticketId) {
        Ticket ticket = ticket(ticketId);
        if (ticket != null) {
            FakeData.leave(ticket.getQueueId(), ticketId);
            held.remove(indexOf(ticketId));
        }
    }

    /** "Done" on a finished ticket: file it in history and stop holding it. */
    public static void finishTicket(String ticketId) {
        Ticket ticket = ticket(ticketId);
        if (ticket == null) {
            return;
        }
        if (!ticket.isLive()) {
            FakeData.addToHistory(ticket);
        }
        held.remove(indexOf(ticketId));
    }

    /** Logging out gives up every ticket. */
    public static void clear() {
        for (Ticket ticket : new ArrayList<>(held)) {
            if (ticket.isLive()) {
                FakeData.leave(ticket.getQueueId(), ticket.getId());
            }
        }
        held.clear();
        countedNoShows.clear();
    }

    // ---- Internals ----------------------------------------------------------

    /**
     * Brings every live ticket up to date with its line. A finished ticket keeps what it
     * became (the queuer's "I'm here" is final even before the counter taps Served).
     */
    private static void refresh() {
        for (int i = 0; i < held.size(); i++) {
            Ticket mine = held.get(i);
            if (!mine.isLive()) {
                continue;
            }
            Ticket now = FakeData.ticket(mine.getQueueId(), mine.getId());
            if (now == null) {
                continue;
            }
            // Called and the window closed while no screen was watching: release it now.
            if (now.getStatus() == Ticket.Status.CALLED && graceRemainingMs(now) == 0) {
                FakeData.releaseCalled(now.getQueueId(), now.getId());
                now = FakeData.ticket(mine.getQueueId(), mine.getId());
            }
            held.set(i, now);
            countNoShowOnce(now);
        }
    }

    /** A no-show, or a removal for pranking (which counts as one), goes toward the cooldown once. */
    private static void countNoShowOnce(Ticket ticket) {
        boolean counts = ticket.getStatus() == Ticket.Status.NO_SHOW
                || ticket.getRemovalReason() == Ticket.RemovalReason.PRANK;
        if (!counts || !countedNoShows.add(ticket.getId())) {
            return;
        }
        Queue queue = FakeData.queueById(ticket.getQueueId());
        if (queue != null && queue.isNoShowCooldownEnabled()) {
            Cooldown.recordNoShow();
        }
    }

    private static void replace(Ticket ticket) {
        int index = indexOf(ticket.getId());
        if (index >= 0) {
            held.set(index, ticket);
        }
    }

    private static int indexOf(String ticketId) {
        for (int i = 0; i < held.size(); i++) {
            if (held.get(i).getId().equals(ticketId)) {
                return i;
            }
        }
        return -1;
    }

    /** Called first, then waiting, then finished. */
    private static int rank(Ticket ticket) {
        switch (ticket.getStatus()) {
            case CALLED:
                return 0;
            case WAITING:
                return 1;
            default:
                return 2;
        }
    }

    /** When the ticket's queue next opens; ties go to the earlier join. */
    private static Instant opensAt(Ticket ticket) {
        Queue queue = FakeData.queueById(ticket.getQueueId());
        if (queue == null) {
            return ticket.getJoinedAt();
        }
        return queue.getStartDate().atTime(queue.getOpensAt()).atZone(Format.MANILA).toInstant();
    }
}

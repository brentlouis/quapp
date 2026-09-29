package com.example.quapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The queuer's tickets: several at once, as long as their queues' hours don't overlap
 * (MODELS.md). Each is kept until the queuer taps Done on its outcome.
 *
 * The tickets live on the server; this is the app's copy of them, so every screen can read
 * them straight away (the reading methods return at once). {@link #sync} brings the copy up to
 * date: QuappApplication calls it every 10 seconds while the app is open (DECISIONS.md "'You've
 * been called' reaches the phone by polling"), and every change the queuer makes (join, I'm here,
 * move back, leave) goes to the server first and then updates the copy. Screens that show tickets
 * {@link #addListener listen}, and redraw when the copy changes.
 *
 * Two things are shown differently from the server's word, on purpose:
 * - "I'm here" leaves the ticket CALLED on the server until the counter serves it; the queuer
 *   has done their part, so the app shows it as SERVED straight away.
 * - When the 3-minute grace window runs out without "I'm here", the server leaves the no-show
 *   to the organizer (MODELS.md "Line"); the app shows the slot as released, as it always has.
 *   If the counter serves the person anyway, the next sync shows SERVED.
 */
public final class ActiveTicketStore {

    /** The server's copies, in the order they were first seen; {@link #tickets()} sorts them. */
    private static final List<Ticket> held = new ArrayList<>();
    private static final List<Runnable> listeners = new ArrayList<>();
    /** A sync is on its way; another request for one waits for the next round. */
    private static boolean syncing;
    /**
     * Tickets the queuer closed with Done. A ticket confirmed with I'm here stays live on the
     * server until the counter calls the next person, so syncs would bring it back without this.
     */
    private static final Set<String> dismissed = new HashSet<>();

    private ActiveTicketStore() {
        // Utility class.
    }

    // ---- Reading ------------------------------------------------------------

    /**
     * Every ticket held, as the queuer should see it: being called on top, then the rest by when
     * their queue opens, finished ones last.
     */
    public static List<Ticket> tickets() {
        List<Ticket> result = new ArrayList<>();
        for (Ticket ticket : held) {
            result.add(shown(ticket));
        }
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
        int index = indexOf(ticketId);
        return index < 0 ? null : shown(held.get(index));
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
            Queue other = Queues.get(ticket.getQueueId());
            if (other != null && !other.getId().equals(queue.getId())
                    && TicketRules.hoursOverlap(other, queue, Format.today())) {
                return ticket;
            }
        }
        return null;
    }

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

    // ---- Listening ----------------------------------------------------------

    /** Told (on the main thread) whenever the copy changes. Remove it when the screen goes. */
    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static void changed() {
        // A copy of the list, so a listener that removes itself doesn't break the loop
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    // ---- Keeping up with the server ------------------------------------------

    /**
     * Brings the copy up to date: the live tickets, the final state of any that stopped being
     * live since (served, released, closed), each ticket's queue (for its hours and now
     * serving), and the cooldown. Listeners hear once it's all in.
     */
    public static void sync(final Context context) {
        if (syncing || !new Session(context).isLoggedIn()) {
            return;
        }
        syncing = true;
        ApiClient.api(context).myTickets(true).enqueue(new ApiCallback<List<Ticket>>(context) {
            @Override
            protected void onSuccess(@Nullable List<Ticket> live) {
                mergeLive(context, live);
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                // Offline or the server is down: keep the copy as it is and try next round
                syncing = false;
            }
        });
    }

    /**
     * The live list replaces the live tickets. Any that were live and aren't any more are asked
     * for one by one, so their outcome can be shown; one that's gone entirely (left from another
     * phone) is dropped.
     */
    private static void mergeLive(final Context context, List<Ticket> live) {
        Map<String, Ticket> fresh = new HashMap<>();
        for (Ticket ticket : live) {
            if (!dismissed.contains(ticket.getId())) {
                fresh.put(ticket.getId(), ticket);
            }
        }

        final List<String> finishedSince = new ArrayList<>();
        List<Ticket> merged = new ArrayList<>();
        for (Ticket old : held) {
            Ticket now = fresh.remove(old.getId());
            if (now != null) {
                notifyIfCalled(context, old, now);
                merged.add(now);
            } else {
                merged.add(old);
                if (old.isLive()) {
                    finishedSince.add(old.getId());
                }
            }
        }
        // Joined from another phone, or the first sync after starting the app
        for (Ticket ticket : fresh.values()) {
            notifyIfCalled(context, null, ticket);
            merged.add(ticket);
        }
        held.clear();
        held.addAll(merged);

        // Everything else this sync needs, counted down to one "changed"
        Set<String> queueIds = new HashSet<>();
        for (Ticket ticket : held) {
            if (ticket.isLive() || Queues.get(ticket.getQueueId()) == null) {
                queueIds.add(ticket.getQueueId());
            }
        }
        final int[] pending = {finishedSince.size() + queueIds.size() + 1};
        final Runnable oneDone = new Runnable() {
            @Override
            public void run() {
                if (--pending[0] == 0) {
                    syncing = false;
                    changed();
                }
            }
        };

        for (final String ticketId : finishedSince) {
            ApiClient.api(context).ticket(ticketId).enqueue(new ApiCallback<Ticket>(context) {
                @Override
                protected void onSuccess(@Nullable Ticket ticket) {
                    CalledNotifier.cancel(context, ticketId);
                    replace(ticket);
                    oneDone.run();
                }

                @Override
                protected void onError(@NonNull ApiError error) {
                    if (error.is("TICKET_NOT_FOUND")) {
                        CalledNotifier.cancel(context, ticketId);
                        remove(ticketId);
                    }
                    oneDone.run();
                }
            });
        }
        for (String queueId : queueIds) {
            Queues.fetch(context, queueId, new Queues.Loaded() {
                @Override
                public void onLoaded(Queue queue) {
                    oneDone.run();
                }

                @Override
                public void onFailed(ApiError error) {
                    oneDone.run();
                }
            });
        }
        ApiClient.api(context).cooldown().enqueue(new ApiCallback<QuappApi.CooldownState>(context) {
            @Override
            protected void onSuccess(@Nullable QuappApi.CooldownState state) {
                Cooldown.update(state);
                oneDone.run();
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                oneDone.run();
            }
        });
    }

    /**
     * Shows the "You're being called" notification the moment a ticket turns CALLED, and clears
     * it once the queuer has answered or the call is over.
     */
    private static void notifyIfCalled(Context context, @Nullable Ticket before, Ticket now) {
        boolean calledNow = now.getStatus() == Ticket.Status.CALLED && now.getHereAt() == null;
        boolean calledBefore = before != null && before.getStatus() == Ticket.Status.CALLED
                && before.getHereAt() == null;
        if (calledNow && !calledBefore && graceRemainingMs(now) > 0) {
            CalledNotifier.show(context, now);
        } else if (!calledNow && calledBefore) {
            CalledNotifier.cancel(context, now.getId());
        }
    }

    // ---- Changing (server first, then the copy) --------------------------------

    /** What a change hands back: the result, or the server's reason it didn't happen. */
    public interface Done<T> {
        void onDone(@Nullable T result);

        void onFailed(@NonNull ApiError error);
    }

    public static void join(Context context, String queueId, @Nullable Double latitude,
                            @Nullable Double longitude, String holderName, String holderPhone,
                            final Done<Ticket> done) {
        ApiClient.api(context).join(queueId,
                        new QuappApi.JoinBody(latitude, longitude, holderName, holderPhone))
                .enqueue(relay(context, new Done<Ticket>() {
                    @Override
                    public void onDone(@Nullable Ticket ticket) {
                        held.add(ticket);
                        changed();
                        done.onDone(ticket);
                    }

                    @Override
                    public void onFailed(@NonNull ApiError error) {
                        done.onFailed(error);
                    }
                }));
    }

    /** "I'm here": the console shows the person confirmed; the app shows the ticket as done. */
    public static void here(final Context context, final String ticketId, final Done<Ticket> done) {
        CalledNotifier.cancel(context, ticketId);
        ApiClient.api(context).here(ticketId).enqueue(relay(context, updating(done)));
    }

    /**
     * "I need more time". With dryRun, the server answers where the ticket would land and
     * nothing moves (the sheet's preview); the copy isn't touched.
     */
    public static void moveBack(Context context, String ticketId, int minutesNeeded,
                                boolean dryRun, Done<Ticket> done) {
        if (!dryRun) {
            CalledNotifier.cancel(context, ticketId);
        }
        ApiClient.api(context).moveBack(ticketId, dryRun, new QuappApi.MoveBackBody(minutesNeeded))
                .enqueue(relay(context, dryRun ? done : updating(done)));
    }

    /** Leave queue: gives the place up for good. Not filed in history. */
    public static void leave(Context context, final String ticketId, final Done<Void> done) {
        ApiClient.api(context).leave(ticketId).enqueue(relay(context, new Done<Void>() {
            @Override
            public void onDone(@Nullable Void nothing) {
                remove(ticketId);
                changed();
                done.onDone(null);
            }

            @Override
            public void onFailed(@NonNull ApiError error) {
                done.onFailed(error);
            }
        }));
    }

    /** "Done" on a finished ticket: stop showing it. History has it on the server. */
    public static void finishTicket(String ticketId) {
        dismissed.add(ticketId);
        remove(ticketId);
        changed();
    }

    /** Signing out forgets this account's tickets on this phone (they stay on the server). */
    public static void clear() {
        held.clear();
        dismissed.clear();
        Cooldown.clear();
        Queues.clear();
        changed();
    }

    // ---- Internals ----------------------------------------------------------

    /** The ticket as the queuer should see it (see the class comment). */
    private static Ticket shown(Ticket ticket) {
        if (ticket.getStatus() != Ticket.Status.CALLED) {
            return ticket;
        }
        if (ticket.getHereAt() != null) {
            return ticket.toBuilder().setStatus(Ticket.Status.SERVED).build();
        }
        Queue queue = Queues.get(ticket.getQueueId());
        boolean grace = queue == null || queue.isGracePeriodEnabled();
        if (grace && graceRemainingMs(ticket) == 0) {
            return ticket.toBuilder().setStatus(Ticket.Status.NO_SHOW).build();
        }
        return ticket;
    }

    /** A change that answers with the ticket: keep the server's new copy, then pass it on. */
    private static Done<Ticket> updating(final Done<Ticket> done) {
        return new Done<Ticket>() {
            @Override
            public void onDone(@Nullable Ticket ticket) {
                replace(ticket);
                changed();
                done.onDone(shown(ticket));
            }

            @Override
            public void onFailed(@NonNull ApiError error) {
                done.onFailed(error);
            }
        };
    }

    /** An ApiCallback that hands the answer to a Done. */
    private static <T> ApiCallback<T> relay(Context context, final Done<T> done) {
        return new ApiCallback<T>(context) {
            @Override
            protected void onSuccess(@Nullable T body) {
                done.onDone(body);
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                done.onFailed(error);
            }
        };
    }

    private static void replace(Ticket ticket) {
        if (dismissed.contains(ticket.getId())) {
            return;  // an answer that arrived after Done
        }
        int index = indexOf(ticket.getId());
        if (index >= 0) {
            held.set(index, ticket);
        } else {
            held.add(ticket);
        }
    }

    private static void remove(String ticketId) {
        int index = indexOf(ticketId);
        if (index >= 0) {
            held.remove(index);
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
        Queue queue = Queues.get(ticket.getQueueId());
        if (queue == null) {
            return ticket.getJoinedAt();
        }
        return queue.getStartDate().atTime(queue.getOpensAt()).atZone(Format.MANILA).toInstant();
    }
}

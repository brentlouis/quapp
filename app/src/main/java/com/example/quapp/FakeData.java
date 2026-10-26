package com.example.quapp;

import android.os.SystemClock;

import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory stand-in for the backend. Holds queues, each queue's waiting list,
 * served/no-show counts and the queuer's ticket history for as long as the app
 * process lives. Every method here maps onto a future API endpoint, so swapping
 * this for Retrofit calls shouldn't touch the layouts.
 */
public final class FakeData {

    /** How many recent service times the wait forecast averages over. */
    public static final int SERVICE_TIME_WINDOW = 5;
    private static final long DEFAULT_SERVICE_MS = 5 * 60_000L;
    /** Seeded open queues get a new arrival this often, so someone is always joining behind you. */
    private static final long ARRIVAL_EVERY_MS = 60_000L;
    private static final int MAX_ARRIVALS = 20;

    /** The organizer id FakeData uses for "the logged-in owner" until there are real accounts. */
    public static final String MY_ORGANIZER_ID = "me";
    /** The logged-in owner's organization. Whether it's verified is myVerification(). */
    public static final String MY_ORGANIZER_NAME = "Brgy. Poblacion Council";

    // Default coordinates (Tagbilaran City) for new queues until there's a map picker.
    private static final double DEFAULT_LATITUDE = 9.6496;
    private static final double DEFAULT_LONGITUDE = 123.8547;

    private static final String[] FIRST_NAMES = {
            "Maria", "Jose", "Ana", "Pedro", "Rosa", "Carlos", "Elena", "Miguel"
    };
    private static final String[] LAST_NAMES = {
            "Santos", "Rivera", "Lopez", "Cruz", "Mendoza", "Bautista", "Torres", "Reyes"
    };

    /** The mutable live side of one queue — what the owner changes from the Live Console. */
    private static final class LiveQueue {
        final List<Ticket> waiting;
        final RollingAverage serviceTimes;
        final Map<String, LiveQueue> counters = new LinkedHashMap<>();
        final Map<String, String> counterNames = new LinkedHashMap<>();
        int nextCounterNumber = 2;
        String announcement;
        LiveQueue() { this.waiting = new ArrayList<>(); this.serviceTimes = new RollingAverage(SERVICE_TIME_WINDOW); }
        LiveQueue(LiveQueue line) { this.waiting = line.waiting; this.serviceTimes = line.serviceTimes; }
        Ticket nowServing;
        /** The person being served didn't confirm in time; already counted as a no-show. */
        boolean nowServingTimedOut;
        /** When the person being served tapped "I'm here"; null until they do. */
        Instant nowServingConfirmedAt;
        int served;
        int noShows;
        int nextTicketNumber = 1;
        long lastServedAt; // SystemClock.elapsedRealtime(); 0 = nobody served this session
        /** Seeded queues simulate people joining; 0 = no simulated arrivals (new queues). */
        long arrivalsSince;
        int arrivals;
    }

    private static final List<Queue> queues = new ArrayList<>();
    private static final List<String> ownedIds = new ArrayList<>();
    private static final Map<String, LiveQueue> live = new HashMap<>();
    private static final List<Ticket> history = new ArrayList<>();
    /** How each ticket that left a line ended, by ticket id. The queuer's app reads it back. */
    private static final Map<String, Ticket> finished = new HashMap<>();
    private static int nextQueueNumber = 1;
    private static boolean seeded;

    /**
     * What a push message from the server would tell the queuer's phone. QuappApplication
     * listens, and posts or clears the "You're being called" notification.
     */
    public interface CounterListener {
        /** A ticket was just called to the counter. */
        void onCalled(Ticket ticket);

        /** A ticket stopped waiting at the counter: confirmed, served, released or moved. */
        void onLeftCounter(String ticketId);
    }

    @Nullable
    private static CounterListener counterListener;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable error);
    }

    private static boolean debugMode = false;
    private static long debugDelayMs = 1500L;
    private static boolean debugFailOnce = false;
    private static boolean debugEmptyOnce = false;
    private static final android.os.Handler asyncHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void setDebugDelayMs(long delayMs) {
        debugDelayMs = delayMs;
    }

    public static void setDebugFailOnce(boolean fail) {
        debugFailOnce = fail;
    }

    public static void setDebugEmptyOnce(boolean empty) {
        debugEmptyOnce = empty;
    }

    private interface Supplier<T> {
        T get() throws Exception;
    }

    @SuppressWarnings("unchecked")
    private static <T> void dispatchAsync(final Supplier<T> supplier, final Callback<T> callback) {
        long delay = debugMode ? debugDelayMs : 0L;
        asyncHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (debugFailOnce) {
                    debugFailOnce = false;
                    callback.onError(new java.io.IOException("Could not connect to the server. Please check your connection and try again."));
                    return;
                }
                try {
                    T result = supplier.get();
                    if (debugEmptyOnce && result instanceof List) {
                        debugEmptyOnce = false;
                        callback.onSuccess((T) java.util.Collections.emptyList());
                    } else {
                        callback.onSuccess(result);
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        }, delay);
    }

    public static void joinAsync(final String queueId, final String holderName, final String holderPhone, final Callback<Ticket> callback) {
        dispatchAsync(new Supplier<Ticket>() {
            @Override
            public Ticket get() {
                return join(queueId, holderName, holderPhone);
            }
        }, callback);
    }

    public static void reportAsync(final android.content.Context context, final String queueId, final Report.Reason reason, final String details, final Callback<Void> callback) {
        final String reporter = new Session(context).isLoggedIn() ? new Session(context).getPhone() : null;
        dispatchAsync(new Supplier<Void>() {
            @Override
            public Void get() {
                Report report = new Report("r" + (reports.size() + 1), queueId, reason, details, Instant.now());
                report.setReporterPhone(reporter);
                reports.add(report);
                return null;
            }
        }, callback);
    }

    // Trust and safety: the logged-in organizer's badge, reports sent, suspended accounts.
    // Verified to start with, so the seeded queues have their badges; see setMyVerification.
    private static VerificationStatus myVerification = VerificationStatus.VERIFIED;
    @Nullable
    private static VerificationRequest myRequest;
    private static final List<Report> reports = new ArrayList<>();

    private FakeData() {
        // Utility class.
    }

    // ---- Queues -------------------------------------------------------------

    public static synchronized List<Queue> queues() {
        ensureSeeded();
        List<Queue> result = new ArrayList<>();
        for (Queue queue : queues) {
            result.add(withLiveNumbers(queue));
        }
        return result;
    }

    public static synchronized Queue queueById(String queueId) {
        ensureSeeded();
        for (Queue queue : queues) {
            if (queue.getId().equals(queueId)) {
                return withLiveNumbers(queue);
            }
        }
        return null;
    }

    public static synchronized List<Queue> ownedQueues() {
        ensureSeeded();
        List<Queue> result = new ArrayList<>();
        for (String id : ownedIds) {
            result.add(queueById(id));
        }
        return result;
    }

    /** Adds a new queue (owned by the current user) or replaces one with the same id. */
    public static synchronized void saveQueue(Queue queue) {
        ensureSeeded();
        for (int i = 0; i < queues.size(); i++) {
            if (queues.get(i).getId().equals(queue.getId())) {
                queues.set(i, queue);
                return;
            }
        }
        // Newest first, on Browse and the Dashboard alike.
        queues.add(0, queue);
        ownedIds.add(0, queue.getId());
        live.put(queue.getId(), new LiveQueue());
    }

    /**
     * Pausing keeps everyone in line so the owner can finish. Closing ends every ticket still in
     * line as QUEUE_CLOSED, which never counts as a no-show (DECISIONS.md "A fifth ticket status").
     */
    public static synchronized void setQueueStatus(String queueId, Queue.Status status) {
        Queue queue = queueById(queueId);
        if (queue == null) {
            return;
        }
        Queue.Builder changed = queue.toBuilder().setStatus(status);
        if (status == Queue.Status.PAUSED) {
            changed.setPausedAt(Instant.now());
        } else if (status == Queue.Status.CLOSED) {
            changed.setClosedAt(Instant.now());
        } else {
            changed.setPausedAt(null).setClosedAt(null);
        }
        saveQueue(changed.build());

        if (status == Queue.Status.CLOSED) {
            LiveQueue state = liveState(queueId);
            for (Ticket ticket : state.waiting) {
                finish(ticket, Ticket.Status.QUEUE_CLOSED);
            }
            // A timed-out slot is already a no-show; anyone else at the counter is closed out.
            for (LiveQueue counter : servingSlots(state)) {
                if (counter.nowServing != null && !counter.nowServingTimedOut) {
                    finish(counter.nowServing, Ticket.Status.QUEUE_CLOSED);
                }
                counter.nowServing = null;
                counter.nowServingConfirmedAt = null;
                counter.nowServingTimedOut = false;
            }
            state.waiting.clear();
            state.nowServing = null;
            state.nowServingConfirmedAt = null;
        }
    }

    // ---- Live console -------------------------------------------------------

    public static final String MAIN_COUNTER_ID = "main";

    /** Local demo ownership check; the future server must enforce the authenticated owner. */
    public static synchronized boolean ownsQueue(String queueId) {
        ensureSeeded();
        for (Queue queue : queues) if (queue.getId().equals(queueId)) return MY_ORGANIZER_ID.equals(queue.getOrganizerId()) && ownedIds.contains(queueId);
        return false;
    }

    private static LiveQueue ownedLine(String queueId) {
        if (!ownsQueue(queueId)) throw new SecurityException("Queue owner required");
        return liveState(queueId);
    }

    public static synchronized List<QueueCounter> counters(String queueId) {
        LiveQueue line = ownedLine(queueId);
        List<QueueCounter> result = new ArrayList<>();
        result.add(new QueueCounter(MAIN_COUNTER_ID, queueId, line.counterNames.getOrDefault(MAIN_COUNTER_ID, "")));
        for (String id : line.counters.keySet()) result.add(new QueueCounter(id, queueId, line.counterNames.get(id)));
        return result;
    }

    public static synchronized QueueCounter createCounter(String queueId, String name) {
        LiveQueue line = ownedLine(queueId);
        String clean = counterName(name);
        String id = queueId + "-counter-" + line.nextCounterNumber++;
        line.counters.put(id, new LiveQueue(line));
        line.counterNames.put(id, clean);
        return new QueueCounter(id, queueId, clean);
    }

    /** Public message for this queue's displays; process-local until the server exists. */
    public static synchronized String announcement(String queueId) {
        return queueById(queueId) == null ? null : liveState(queueId).announcement;
    }

    public static synchronized void publishAnnouncement(String queueId, String message) {
        LiveQueue line = ownedLine(queueId);
        if (Validation.isBlank(message)) throw new IllegalArgumentException("Announcement required");
        String clean = message.trim();
        if (clean.codePointCount(0, clean.length()) > 500) throw new IllegalArgumentException("Announcement too long");
        line.announcement = clean;
    }

    public static synchronized void clearAnnouncement(String queueId) { ownedLine(queueId).announcement = null; }

    /** Exact, owner-only lookup; does not advance simulation, grace timers or ticket state. */
    public static synchronized Ticket findTicketByNumber(String queueId, int number) {
        if (!ownsQueue(queueId)) throw new SecurityException("Queue owner required");
        if (number < 0) throw new IllegalArgumentException("Nonnegative ticket number required");
        LiveQueue line = live.get(queueId);
        if (line == null) return null;
        for (int i = 0; i < line.waiting.size(); i++) if (line.waiting.get(i).getTicketNumber() == number) return placed(line, i);
        for (LiveQueue slot : servingSlots(line)) if (slot.nowServing != null && slot.nowServing.getTicketNumber() == number) return slot.nowServingTimedOut ? finished.get(slot.nowServing.getId()) : slot.nowServing;
        for (Ticket ticket : finished.values()) if (queueId.equals(ticket.getQueueId()) && ticket.getTicketNumber() == number) return ticket;
        return null;
    }

    public static synchronized void renameCounter(String queueId, String counterId, String name) {
        LiveQueue line = ownedLine(queueId);
        counterSlot(line, counterId);
        line.counterNames.put(counterId, counterName(name));
    }

    private static String counterName(String name) {
        if (Validation.isBlank(name)) throw new IllegalArgumentException("Counter name required");
        return name.trim();
    }

    private static LiveQueue counterSlot(LiveQueue line, String counterId) {
        if (MAIN_COUNTER_ID.equals(counterId)) return line;
        LiveQueue slot = line.counters.get(counterId);
        if (slot == null) throw new IllegalArgumentException("Counter not found");
        return slot;
    }

    public static synchronized Ticket counterServing(String queueId, String counterId) {
        LiveQueue slot = counterSlot(ownedLine(queueId), counterId);
        return slot.nowServingTimedOut ? null : slot.nowServing;
    }

    public static synchronized Ticket callNextAtCounter(String queueId, String counterId) {
        LiveQueue line = ownedLine(queueId);
        Queue.Status status = queueById(queueId).getStatus();
        if (status != Queue.Status.OPEN && status != Queue.Status.PAUSED) throw new IllegalStateException("Queue is not running");
        LiveQueue slot = counterSlot(line, counterId);
        expireSlot(queueById(queueId), slot);
        if (slot.nowServing != null && !slot.nowServingTimedOut) throw new IllegalStateException("Finish current ticket first");
        callFront(slot);
        return slot.nowServing;
    }

    public static synchronized void finishCounter(String queueId, String counterId, boolean noShow) {
        LiveQueue slot = counterSlot(ownedLine(queueId), counterId);
        expireSlot(queueById(queueId), slot);
        if (slot.nowServing == null || slot.nowServingTimedOut) return;
        if (noShow) slot.noShows++; else recordServed(slot);
        finish(slot.nowServing, noShow ? Ticket.Status.NO_SHOW : Ticket.Status.SERVED);
        slot.nowServing = null;
        slot.nowServingConfirmedAt = null;
    }

    private static List<LiveQueue> servingSlots(LiveQueue line) {
        List<LiveQueue> slots = new ArrayList<>(); slots.add(line); slots.addAll(line.counters.values()); return slots;
    }

    private static LiveQueue ticketSlot(LiveQueue line, String ticketId) {
        for (LiveQueue slot : servingSlots(line)) if (slot.nowServing != null && slot.nowServing.getId().equals(ticketId)) return slot;
        return line;
    }

    /** The line in call order, each ticket stamped with its current position and wait. */
    public static synchronized List<Ticket> waitingTickets(String queueId) {
        LiveQueue state = liveState(queueId);
        List<Ticket> result = new ArrayList<>();
        for (int i = 0; i < state.waiting.size(); i++) {
            result.add(placed(state, i));
        }
        return result;
    }

    public static synchronized Ticket nowServing(String queueId) {
        return liveState(queueId).nowServing;
    }

    /**
     * The grace period, seen from the counter: if the person being served was called more than
     * 3 minutes ago and never confirmed, their slot is released and counted as a no-show, once.
     * The server does this on its own; FakeData does it whenever the console asks.
     *
     * @return true when the person being served has timed out
     */
    public static synchronized boolean checkGraceExpired(String queueId) {
        LiveQueue state = liveState(queueId);
        Queue queue = queueById(queueId);
        for (LiveQueue slot : servingSlots(state)) expireSlot(queue, slot);
        return state.nowServingTimedOut;
    }

    private static void expireSlot(Queue queue, LiveQueue state) {
        Ticket serving = state.nowServing;
        if (serving == null || state.nowServingTimedOut || queue == null
                || !queue.isGracePeriodEnabled() || serving.getCalledAt() == null) {
            return;
        }
        if (state.nowServingConfirmedAt == null && Instant.now().isAfter(graceDeadline(serving))) {
            timeOut(state);
        }
    }

    /** Serving tickets for number-only public presentation; counter metadata remains owner-only. */
    public static synchronized List<Ticket> servingTickets(String queueId) {
        checkGraceExpired(queueId);
        List<Ticket> result = new ArrayList<>();
        for (LiveQueue slot : servingSlots(liveState(queueId))) if (slot.nowServing != null && !slot.nowServingTimedOut) result.add(slot.nowServing);
        result.sort((a, b) -> Integer.compare(a.getTicketNumber(), b.getTicketNumber()));
        return result;
    }

    public static synchronized boolean arrivalConfirmed(String queueId, String ticketId) {
        LiveQueue slot = ticketSlot(liveState(queueId), ticketId);
        return isAtCounter(slot, ticketId) && slot.nowServingConfirmedAt != null;
    }

    /** The person currently being served is done; the front of the line is called. */
    public static synchronized Ticket callNext(String queueId) {
        LiveQueue state = liveState(queueId);

        // A timed-out ticket was already counted as a no-show, so it isn't also "served".
        if (state.nowServing != null && !state.nowServingTimedOut) {
            recordServed(state);
            finish(state.nowServing, Ticket.Status.SERVED);
        }

        callFront(state);
        return state.nowServing;
    }

    public static synchronized void markServed(String queueId, String ticketId) {
        LiveQueue line = ownedLine(queueId);
        for (QueueCounter counter : counters(queueId)) {
            LiveQueue slot = counterSlot(line, counter.getId());
            if (isAtCounter(slot, ticketId)) {
                finishCounter(queueId, counter.getId(), false);
                return;
            }
        }
    }

    public static synchronized void markNoShow(String queueId, String ticketId) {
        LiveQueue line = ownedLine(queueId);
        for (QueueCounter counter : counters(queueId)) {
            LiveQueue slot = counterSlot(line, counter.getId());
            if (isAtCounter(slot, ticketId)) {
                finishCounter(queueId, counter.getId(), true);
                return;
            }
        }
    }

    /** Someone without a phone joins at the counter. */
    public static synchronized Ticket addWalkIn(String queueId, String holderName) {
        Queue queue = queueById(queueId);
        if (queue == null) throw new IllegalArgumentException("Queue not found");
        if (!ownsQueue(queueId)) throw new SecurityException("Queue owner required");
        if (queue.getStatus() == Queue.Status.CLOSED)
            throw new IllegalStateException("Queue is closed to walk-ins");
        LiveQueue state = liveState(queueId);
        int number = state.nextTicketNumber++;

        Ticket ticket = new Ticket.Builder()
                .setId(queueId + "-t" + number)
                .setQueue(queueId, queue == null ? "" : queue.getName(),
                        queue == null ? "" : queue.getVenue())
                .setHolder(holderName, null)
                .setWalkIn(true)
                .setTicketNumber(number)
                .setPosition(state.waiting.size() + 1)
                .setEstimatedWaitMinutes(forecastMinutes(state, state.waiting.size()))
                .build();

        state.waiting.add(ticket);
        return ticket;
    }

    // ---- The queuer's side of a line -------------------------------------------

    /** The number the next person to join will be handed. */
    public static int nextTicketNumber(String queueId) {
        return liveState(queueId).nextTicketNumber;
    }

    /** Joining puts a new ticket at the back of the line. */
    public static synchronized Ticket join(String queueId, String holderName, String holderPhone) {
        Queue queue = queueById(queueId);
        if (queue == null) throw new IllegalArgumentException("Queue not found");
        Ticket existing = ActiveTicketStore.ticketForQueue(queueId);
        if (existing != null) return existing;
        if (!queue.acceptsJoins())
            throw new IllegalStateException("Queue is unavailable for new tickets");
        if (ActiveTicketStore.overlapping(queue) != null)
            throw new IllegalStateException("Queue hours overlap an active ticket");
        if (queue.isNoShowCooldownEnabled() && Cooldown.isActive())
            throw new IllegalStateException("No-show cooldown is active");
        if (Validation.isBlank(holderName) || !Validation.isValidPhone(holderPhone))
            throw new IllegalArgumentException("A name and valid phone are required");
        LiveQueue state = liveState(queueId);
        int number = state.nextTicketNumber++;
        Ticket ticket = new Ticket.Builder()
                .setId(queueId + "-t" + number)
                .setQueue(queueId, queue == null ? "" : queue.getName(),
                        queue == null ? "" : queue.getVenue())
                .setHolder(holderName, holderPhone)
                .setTicketNumber(number)
                .build();
        state.waiting.add(ticket);
        Ticket placed = placed(state, state.waiting.size() - 1);
        return placed;
    }

    /**
     * Where a ticket stands now: in line (with its position and wait), at the counter (CALLED),
     * or how it ended. Null if this queue never had it.
     */
    public static synchronized Ticket ticket(String queueId, String ticketId) {
        LiveQueue state = liveState(queueId);
        LiveQueue slot = ticketSlot(state, ticketId);
        if (slot.nowServing != null && slot.nowServing.getId().equals(ticketId)) {
            return slot.nowServingTimedOut ? finished.get(ticketId) : slot.nowServing;
        }
        int index = indexOf(state.waiting, ticketId);
        if (index >= 0) {
            return placed(state, index);
        }
        return finished.get(ticketId);
    }

    /**
     * Demo hook until the console and the queuer are on different phones: the counter calls
     * this ticket now. Whoever was at the counter is finished first, as with Call next.
     */
    public static synchronized void callTicket(String queueId, String ticketId) {
        LiveQueue state = liveState(queueId);
        int index = indexOf(state.waiting, ticketId);
        if (index < 0) {
            return;
        }
        if (state.nowServing != null && !state.nowServingTimedOut) {
            recordServed(state);
            finish(state.nowServing, Ticket.Status.SERVED);
        }
        state.waiting.add(0, state.waiting.remove(index));
        callFront(state);
    }

    /** "I'm here": the console shows the person as confirmed. */
    public static synchronized void confirmArrival(String queueId, String ticketId) {
        LiveQueue state = ticketSlot(liveState(queueId), ticketId);
        if (isAtCounter(state, ticketId)) {
            state.nowServingConfirmedAt = Instant.now();
            leftCounter(ticketId);
        }
    }

    /** The queuer's grace period ran out (or the demo skipped it): a no-show, counted once. */
    public static synchronized void releaseCalled(String queueId, String ticketId) {
        LiveQueue state = ticketSlot(liveState(queueId), ticketId);
        if (isAtCounter(state, ticketId)) {
            timeOut(state);
        }
    }

    /** Leave queue: the ticket is simply gone. Not a no-show, and it doesn't go to history. */
    public static synchronized void leave(String queueId, String ticketId) {
        LiveQueue state = ticketSlot(liveState(queueId), ticketId);
        if (isAtCounter(state, ticketId)) {
            state.nowServing = null;
            state.nowServingConfirmedAt = null;
            leftCounter(ticketId);
        } else {
            removeById(state.waiting, ticketId);
        }
    }

    /**
     * "I need more time": the ticket moves back {@code places} places and keeps its number.
     * Called to the counter already, it hands the slot back and rejoins with {@code places}
     * people ahead. Never a no-show. Once per ticket; the caller checks.
     */
    public static synchronized Ticket moveBack(String queueId, String ticketId, int places) {
        LiveQueue line = liveState(queueId);
        LiveQueue state = ticketSlot(line, ticketId);
        Ticket ticket;
        int index;
        if (isAtCounter(state, ticketId)) {
            ticket = state.nowServing;
            state.nowServing = null;
            state.nowServingConfirmedAt = null;
            index = Math.min(places, state.waiting.size());
            leftCounter(ticketId);
        } else {
            int from = indexOf(state.waiting, ticketId);
            if (from < 0) {
                return null;
            }
            ticket = state.waiting.remove(from);
            index = Math.min(from + places, state.waiting.size());
        }
        state.waiting.add(index, ticket.toBuilder()
                .setStatus(Ticket.Status.WAITING)
                .setCalledAt(null)
                .setMovedBack(Instant.now())
                .build());
        return placed(line, index);
    }

    public static synchronized QueueStats stats(String queueId) {
        LiveQueue state = liveState(queueId);
        double averageMs = averageServiceMs(state);
        int served = 0, noShows = 0;
        for (LiveQueue slot : servingSlots(state)) { served += slot.served; noShows += slot.noShows; }

        return new QueueStats(
                served,
                noShows,
                state.waiting.size(),
                averageMs / 60_000d,
                state.serviceTimes.size(),
                forecastMinutes(state, state.waiting.size()),
                // FakeData only has the rolling average; the learning model lives on the server.
                QueueStats.EstimateSource.ROLLING_AVERAGE,
                0);
    }

    // ---- Trust and safety (canvas 50-58) ---------------------------------------

    public static List<Report> reports() {
        return new ArrayList<>(reports);
    }

    // ---- Queuer history -----------------------------------------------------

    public static List<Ticket> history() {
        ensureSeeded();
        return new ArrayList<>(history);
    }

    // ---- Internals ----------------------------------------------------------

    private static LiveQueue liveState(String queueId) {
        ensureSeeded();
        LiveQueue state = live.get(queueId);
        if (state == null) {
            state = new LiveQueue();
            live.put(queueId, state);
        }
        simulateArrivals(queueId, state);
        return state;
    }

    /**
     * Stand-in for other people's phones: a seeded open queue gains one person a minute (up to
     * 20 per session), so the line keeps growing behind the queuer the way a real one does.
     */
    private static void simulateArrivals(String queueId, LiveQueue state) {
        if (state.arrivalsSince == 0 || state.arrivals >= MAX_ARRIVALS) {
            return;
        }
        Queue queue = null;
        for (Queue candidate : queues) {
            if (candidate.getId().equals(queueId)) {
                queue = candidate;
            }
        }
        if (queue == null || queue.getStatus() != Queue.Status.OPEN) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        while (now - state.arrivalsSince >= ARRIVAL_EVERY_MS && state.arrivals < MAX_ARRIVALS) {
            state.arrivalsSince += ARRIVAL_EVERY_MS;
            int i = state.waiting.size() + state.arrivals;
            int number = state.nextTicketNumber++;
            state.waiting.add(new Ticket.Builder()
                    .setId(queueId + "-t" + number)
                    .setQueue(queueId, queue.getName(), queue.getVenue())
                    .setHolder(FIRST_NAMES[i % FIRST_NAMES.length] + " "
                            + LAST_NAMES[(i * 5) % LAST_NAMES.length], "0917" + (1000000 + number))
                    .setTicketNumber(number)
                    .build());
            state.arrivals++;
        }
    }

    /** The stored Queue keeps its config; waiting count and ETA always come from live state. */
    private static Queue withLiveNumbers(Queue queue) {
        LiveQueue state = liveState(queue.getId());
        int waiting = state.waiting.size();
        int eta = queue.getStatus() == Queue.Status.CLOSED ? 0 : forecastMinutes(state, waiting);

        Queue.Builder builder = queue.toBuilder();
        // The badge follows the organizer, not the queue: a revoke shows everywhere at once.
        if (MY_ORGANIZER_ID.equals(queue.getOrganizerId())) {
            builder.setOrganizer(MY_ORGANIZER_ID, MY_ORGANIZER_NAME, isMyOrganizerVerified());
        }
        Ticket representative = null;
        for (LiveQueue slot : servingSlots(state)) {
            if (slot.nowServing != null && !slot.nowServingTimedOut
                    && (representative == null || slot.nowServing.getTicketNumber() < representative.getTicketNumber())) representative = slot.nowServing;
        }
        return builder
                .setPeopleWaiting(waiting)
                .setNowServing(representative == null ? null : representative.getTicketNumber())
                .setEstimatedWaitMinutes(eta)
                .build();
    }

    /** The ticket at this index in line, with its position and wait worked out. */
    private static Ticket placed(LiveQueue state, int index) {
        return state.waiting.get(index).toBuilder()
                .setPosition(index + 1)
                .setEstimatedWaitMinutes(forecastMinutes(state, index))
                .build();
    }

    /** Still at the counter and not timed out. */
    private static boolean isAtCounter(LiveQueue state, String ticketId) {
        return state.nowServing != null && !state.nowServingTimedOut
                && state.nowServing.getId().equals(ticketId);
    }

    /** Records how a ticket that left the line ended, for the queuer's app to read back. */
    private static void finish(Ticket ticket, Ticket.Status status) {
        finished.put(ticket.getId(), ticket.withStatus(status));
        leftCounter(ticket.getId());
    }

    private static void leftCounter(String ticketId) {
        if (counterListener != null) {
            counterListener.onLeftCounter(ticketId);
        }
    }

    private static void timeOut(LiveQueue state) {
        state.nowServingTimedOut = true;
        state.noShows++;
        finish(state.nowServing, Ticket.Status.NO_SHOW);
    }

    /** The front of the line becomes the one being served, stamped with when they were called. */
    private static void callFront(LiveQueue state) {
        state.nowServingTimedOut = false;
        state.nowServingConfirmedAt = null;
        state.nowServing = state.waiting.isEmpty() ? null
                : state.waiting.remove(0).withStatus(Ticket.Status.CALLED);
        if (state.nowServing != null && counterListener != null) {
            counterListener.onCalled(state.nowServing);
        }
    }

    /**
     * The rolling-average forecast: people ahead × average time per person.
     * Before anyone has been served there's no data, so a 5-minute default stands in.
     */
    private static int forecastMinutes(LiveQueue state, int peopleAhead) {
        return (int) Math.round(peopleAhead * averageServiceMs(state) / 60_000d);
    }

    private static double averageServiceMs(LiveQueue state) {
        return state.serviceTimes.isEmpty() ? DEFAULT_SERVICE_MS : state.serviceTimes.average();
    }

    /**
     * A service time is the gap between two consecutive completions. The first
     * completion of a session has no gap before it, so it only starts the clock.
     */
    private static void recordServed(LiveQueue state) {
        long now = SystemClock.elapsedRealtime();
        if (state.lastServedAt > 0) {
            state.serviceTimes.add(now - state.lastServedAt);
        }
        state.lastServedAt = now;
        state.served++;
    }

    private static int indexOf(List<Ticket> tickets, String ticketId) {
        for (int i = 0; i < tickets.size(); i++) {
            if (tickets.get(i).getId().equals(ticketId)) {
                return i;
            }
        }
        return -1;
    }

    /** @return the removed ticket, or null if it wasn't there */
    private static Ticket removeById(List<Ticket> tickets, String ticketId) {
        int index = indexOf(tickets, ticketId);
        return index < 0 ? null : tickets.remove(index);
    }

    // ---- Seed data ----------------------------------------------------------

    private static final String CITY_HEALTH = "City Health Office";

    private static void ensureSeeded() {
        if (seeded) {
            return;
        }
        seeded = true;

        LocalDate today = Format.today();
        // Open queues are scheduled around the current time, so the demo makes sense whenever
        // it runs: opened a couple of hours ago, closing a few hours from now.
        LocalTime opened = earlier(hoursFromNow(-2), LocalTime.of(8, 0));
        LocalTime closes = later(hoursFromNow(5), LocalTime.of(17, 0));

        seed(new Queue.Builder()
                        .setId("q1")
                        .setOrganizer(MY_ORGANIZER_ID, MY_ORGANIZER_NAME, true)
                        .setName("Barangay Relief Distribution")
                        .setCategory(Category.RELIEF)
                        .setShortDescription("Family food packs for registered households")
                        .setDetails("Distribution of family food packs for registered households. One pack per household.")
                        .setBring("Barangay ID or proof of residency · claim stub")
                        .setVenue("Brgy. Poblacion Hall")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6496, 123.8547)
                        .setSchedule(today, today.plusDays(1), opened, closes)
                        .setGracePeriodEnabled(true)
                        .setNoShowCooldownEnabled(true)
                        .setProximity(true, 1000)
                        .build(),
                42, 55, 18, 3, true);

        seed(new Queue.Builder()
                        .setId("q2")
                        .setOrganizer("o2", CITY_HEALTH, true)
                        .setName("Free Medical Mission")
                        .setCategory(Category.MEDICAL)
                        .setShortDescription("Free check-ups, BP tests, and medicines")
                        .setDetails("General consultation, blood pressure screening, and free maintenance medicine for seniors.")
                        .setBring("Senior citizen ID if you have one")
                        .setVenue("Tagbilaran City Gym")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6543, 123.8601)
                        .setSchedule(today, today, opened, closes)
                        .setGracePeriodEnabled(true)
                        .build(),
                18, 25, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q3")
                        .setOrganizer(MY_ORGANIZER_ID, MY_ORGANIZER_NAME, true)
                        .setName("Barangay Clearance Processing")
                        .setCategory(Category.GOVERNMENT)
                        .setShortDescription("Clearance for work and business permits")
                        .setDetails("Application and release of barangay clearance for employment and business permits.")
                        .setBring("Valid ID · ₱50 fee")
                        .setVenue("Brgy. Cogon Office")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6612, 123.8578)
                        .setSchedule(today, today, opened, closes)
                        .setGracePeriodEnabled(true)
                        .build(),
                7, 12, 11, 1, true);

        seed(new Queue.Builder()
                        .setId("q4")
                        .setOrganizer("o4", "BISU Registrar", true)
                        .setName("Registrar Enrollment Window 2")
                        .setCategory(Category.EDUCATION)
                        .setShortDescription("2nd semester enrollment, continuing students")
                        .setDetails("Second semester enrollment for continuing students. Have your registration form pre-filled.")
                        .setBring("Pre-filled registration form")
                        .setVenue("BISU Main Campus")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6402, 123.8563)
                        .setSchedule(today, today.plusDays(4), opened, closes)
                        .setGracePeriodEnabled(true)
                        .setNoShowCooldownEnabled(true)
                        .build(),
                63, 90, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q5")
                        .setOrganizer(MY_ORGANIZER_ID, MY_ORGANIZER_NAME, true)
                        .setName("Senior Citizen Pension Payout")
                        .setCategory(Category.GOVERNMENT)
                        .setShortDescription("Quarterly payout for registered senior citizens")
                        .setDetails("Quarterly social pension release. Beneficiaries must claim in person or through an authorized representative.")
                        .setBring("Senior citizen ID · authorization letter for representatives")
                        .setVenue("Brgy. Dao Covered Court")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6689, 123.8695)
                        .setSchedule(today, today, LocalTime.of(7, 0), LocalTime.of(10, 0))
                        .setStatus(Queue.Status.CLOSED)
                        .setClosedAt(Instant.now().minus(Duration.ofHours(1)))
                        .setGracePeriodEnabled(true)
                        .setNoShowCooldownEnabled(true)
                        .setProximity(true, 500)
                        .build(),
                0, 0, 0, 0, true);

        seed(new Queue.Builder()
                        .setId("q6")
                        .setOrganizer("o6", "Dauis Municipal Agriculture Office", false)
                        .setName("Anti-Rabies Vaccination Drive")
                        .setCategory(Category.MEDICAL)
                        .setShortDescription("Free rabies shots for dogs and cats")
                        .setDetails("Free anti-rabies vaccination for dogs and cats. One pet per queue slot.")
                        .setBring("Your pet on a leash or in a carrier")
                        .setVenue("Brgy. Booy Health Center")
                        .setMunicipality("Dauis")
                        .setLocation(9.6236, 123.8478)
                        .setSchedule(today.plusDays(1), today.plusDays(1), LocalTime.of(8, 0), LocalTime.of(14, 0))
                        .setStatus(Queue.Status.UPCOMING)
                        .setGracePeriodEnabled(true)
                        .build(),
                11, 18, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q7")
                        .setOrganizer("o7", "Panglao Tourism Office", true)
                        .setName("Tourist Assistance Desk")
                        .setCategory(Category.OTHER)
                        .setShortDescription("Lost items, transport help, referrals")
                        .setDetails("Walk-in assistance for lost items, transport help, and accommodation referrals.")
                        .setVenue("Alona Beach Info Center")
                        .setMunicipality("Panglao")
                        .setLocation(9.5786, 123.7486)
                        .setSchedule(today, today.plusDays(30), LocalTime.of(9, 0), LocalTime.of(18, 0))
                        .build(),
                5, 8, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q8")
                        .setOrganizer("o8", "Baclayon MSWDO", true)
                        .setName("Cash Aid Payout")
                        .setCategory(Category.RELIEF)
                        .setShortDescription("AICS financial assistance release")
                        .setDetails("AICS financial assistance release. Claimants must present the notice sent by the MSWDO.")
                        .setBring("MSWDO notice · valid ID")
                        .setVenue("Baclayon Municipal Hall")
                        .setMunicipality("Baclayon")
                        .setLocation(9.6244, 123.9128)
                        .setSchedule(today, today.plusDays(2), opened, closes)
                        .setStatus(Queue.Status.PAUSED)
                        .setPausedAt(Instant.now().minus(Duration.ofMinutes(20)))
                        .setGracePeriodEnabled(true)
                        .setNoShowCooldownEnabled(true)
                        .setProximity(true, 2000)
                        .build(),
                87, 120, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q9")
                        .setOrganizer("o9", "Corella MSWDO", true)
                        .setName("Solo Parent ID Application")
                        .setCategory(Category.IDS)
                        .setShortDescription("New and renewal solo parent IDs")
                        .setDetails("New applications and renewals for solo parent identification cards.")
                        .setBring("Birth certificate of child · barangay certificate")
                        .setVenue("Corella Municipal Hall")
                        .setMunicipality("Corella")
                        .setLocation(9.7089, 123.9161)
                        .setSchedule(today.plusDays(2), today.plusDays(2), LocalTime.of(9, 0), LocalTime.of(15, 0))
                        .setStatus(Queue.Status.UPCOMING)
                        .setNoShowCooldownEnabled(true)
                        .build(),
                3, 6, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q10")
                        .setOrganizer("o10", "Purok 3 Youth Volunteers", false)
                        .setName("School Supplies Distribution")
                        .setCategory(Category.EDUCATION)
                        .setShortDescription("Notebooks and school kits, Grades 1–6")
                        .setDetails("Distribution of notebooks and school kits for Grade 1–6 pupils. Parent or guardian must be present.")
                        .setBring("Pupil's school ID or report card")
                        .setVenue("Loon Central Elementary")
                        .setMunicipality("Loon")
                        .setLocation(9.7986, 123.7947)
                        .setSchedule(today, today, opened, closes)
                        .setGracePeriodEnabled(true)
                        .setNoShowCooldownEnabled(true)
                        .setProximity(true, 1000)
                        .build(),
                29, 40, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q11")
                        .setOrganizer("o11", "Bohol Water Utilities", true)
                        .setName("Water District Bill Payment")
                        .setCategory(Category.BILLS)
                        .setShortDescription("Pay your monthly water bill")
                        .setVenue("BWUA Office")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6478, 123.8531)
                        .setSchedule(today, today.plusDays(60), LocalTime.of(8, 0), LocalTime.of(17, 0))
                        .setGracePeriodEnabled(true)
                        .build(),
                14, 20, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q12")
                        .setOrganizer("o12", "PESO Bohol", true)
                        .setName("PESO Job Fair")
                        .setCategory(Category.JOBS)
                        .setShortDescription("Local and overseas hiring, walk-in interviews")
                        .setDetails("Employers from Bohol and Cebu hiring on the spot. Bring several copies of your résumé.")
                        .setBring("Résumé (5 copies) · valid ID")
                        .setVenue("Island City Mall Activity Center")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6617, 123.8703)
                        .setSchedule(today.plusDays(1), today.plusDays(1), LocalTime.of(13, 0), LocalTime.of(17, 0))
                        .setStatus(Queue.Status.UPCOMING)
                        .setGracePeriodEnabled(true)
                        .build(),
                6, 10, 0, 0, false);

        nextQueueNumber = queues.size() + 1;

        // A few past visits so Queue History isn't empty on first open.
        history.add(pastTicket("q2", 12, Ticket.Status.SERVED, 2));
        history.add(pastTicket("q5", 17, Ticket.Status.QUEUE_CLOSED, 9));
        history.add(pastTicket("q6", 5, Ticket.Status.NO_SHOW, 14));
        history.add(pastTicket("q3", 4, Ticket.Status.SERVED, 30));
    }

    private static LocalTime earlier(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalTime later(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    /**
     * Now, rounded down to the half hour, moved by a number of hours and kept within the day
     * (so a demo at 10 PM doesn't produce a queue that closes "at 3 AM").
     */
    private static LocalTime hoursFromNow(int hours) {
        LocalTime now = LocalTime.now(Format.MANILA);
        LocalTime rounded = LocalTime.of(now.getHour(), now.getMinute() < 30 ? 0 : 30);
        int minutes = rounded.getHour() * 60 + rounded.getMinute() + hours * 60;
        minutes = Math.max(0, Math.min(minutes, 23 * 60 + 30));
        return LocalTime.of(minutes / 60, minutes % 60);
    }

    /**
     * @param waiting      how many people start in line
     * @param etaMinutes   the wait to aim for; seeds the rolling average so the
     *                     forecast starts near this value instead of at the default
     * @param served       tickets already served today
     * @param noShows      tickets already marked no-show today
     */
    private static void seed(Queue queue, int waiting, int etaMinutes,
                             int served, int noShows, boolean owned) {
        queues.add(queue);
        if (owned) {
            ownedIds.add(queue.getId());
        }

        LiveQueue state = new LiveQueue();
        state.served = served;
        state.noShows = noShows;
        state.nextTicketNumber = served + noShows + 1;

        if (waiting > 0 && etaMinutes > 0) {
            long perPersonMs = etaMinutes * 60_000L / waiting;
            for (int i = 0; i < SERVICE_TIME_WINDOW; i++) {
                state.serviceTimes.add(perPersonMs);
            }
        }

        Instant now = Instant.now();
        for (int i = 0; i < waiting; i++) {
            int number = state.nextTicketNumber++;
            // Every seventh person joined at the counter; the fourth asked to move back.
            boolean walkIn = i % 7 == 6;
            state.waiting.add(new Ticket.Builder()
                    .setId(queue.getId() + "-t" + number)
                    .setQueue(queue.getId(), queue.getName(), queue.getVenue())
                    .setHolder(FIRST_NAMES[i % FIRST_NAMES.length] + " "
                                    + LAST_NAMES[(i * 3) % LAST_NAMES.length],
                            walkIn ? null : "0917" + (1000000 + number))
                    .setWalkIn(walkIn)
                    .setTicketNumber(number)
                    .setPosition(i + 1)
                    .setEstimatedWaitMinutes(forecastMinutes(state, i))
                    // The first in line joined longest ago.
                    .setJoinedAt(now.minus(Duration.ofMinutes(3L * (waiting - i))))
                    .setMovedBack(i == 3 ? now.minus(Duration.ofMinutes(12)) : null)
                    .build());
        }

        // Waiting lines keep growing; empty seeded queues stay empty.
        if (waiting > 0) {
            state.arrivalsSince = SystemClock.elapsedRealtime();
        }
        live.put(queue.getId(), state);
    }

    private static Ticket pastTicket(String queueId, int number, Ticket.Status status, int daysAgo) {
        Queue queue = null;
        for (Queue candidate : queues) {
            if (candidate.getId().equals(queueId)) {
                queue = candidate;
            }
        }
        Instant finished = Instant.now().minus(Duration.ofDays(daysAgo));
        return new Ticket.Builder()
                .setId(queueId + "-past" + number)
                .setQueue(queueId, queue == null ? "" : queue.getName(),
                        queue == null ? "" : queue.getVenue())
                .setTicketNumber(number)
                .setStatus(status)
                .setJoinedAt(finished.minus(Duration.ofMinutes(40)))
                .setFinishedAt(finished)
                .build();
    }

    /** When the person being served must confirm by: called_at + 3 min. */
    private static Instant graceDeadline(Ticket serving) {
        return serving.getCalledAt().plusMillis(CalledActivity.GRACE_PERIOD_MS);
    }

    /** The local demo organizer's badge, for the local tools' queues. */
    private static boolean isMyOrganizerVerified() {
        return myVerification == VerificationStatus.VERIFIED;
    }
}

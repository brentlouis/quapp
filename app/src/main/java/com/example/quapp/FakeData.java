package com.example.quapp;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.HashMap;
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
        final List<Ticket> waiting = new ArrayList<>();
        final RollingAverage serviceTimes = new RollingAverage(SERVICE_TIME_WINDOW);
        Ticket nowServing;
        int served;
        int noShows;
        int nextTicketNumber = 1;
        long lastServedAt; // SystemClock.elapsedRealtime(); 0 = nobody served this session
    }

    private static final List<Queue> queues = new ArrayList<>();
    private static final List<String> ownedIds = new ArrayList<>();
    private static final Map<String, LiveQueue> live = new HashMap<>();
    private static final List<Ticket> history = new ArrayList<>();
    private static int nextQueueNumber = 1;
    private static boolean seeded;

    private FakeData() {
        // Utility class.
    }

    // ---- Queues -------------------------------------------------------------

    public static List<Queue> queues() {
        ensureSeeded();
        List<Queue> result = new ArrayList<>();
        for (Queue queue : queues) {
            result.add(withLiveNumbers(queue));
        }
        return result;
    }

    public static Queue queueById(String queueId) {
        ensureSeeded();
        for (Queue queue : queues) {
            if (queue.getId().equals(queueId)) {
                return withLiveNumbers(queue);
            }
        }
        return null;
    }

    public static List<Queue> ownedQueues() {
        ensureSeeded();
        List<Queue> result = new ArrayList<>();
        for (String id : ownedIds) {
            result.add(queueById(id));
        }
        return result;
    }

    public static String newQueueId() {
        ensureSeeded();
        return "q" + nextQueueNumber++;
    }

    public static double defaultLatitude() {
        return DEFAULT_LATITUDE;
    }

    public static double defaultLongitude() {
        return DEFAULT_LONGITUDE;
    }

    /** Adds a new queue (owned by the current user) or replaces one with the same id. */
    public static void saveQueue(Queue queue) {
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

    /** Closing releases everyone still waiting; pausing keeps them so the owner can finish. */
    public static void setQueueStatus(String queueId, Queue.Status status) {
        Queue queue = queueById(queueId);
        if (queue == null) {
            return;
        }
        saveQueue(queue.toBuilder().setStatus(status).build());

        if (status == Queue.Status.CLOSED) {
            LiveQueue state = liveState(queueId);
            state.waiting.clear();
            state.nowServing = null;
        }
    }

    // ---- Live console -------------------------------------------------------

    public static List<Ticket> waitingTickets(String queueId) {
        return new ArrayList<>(liveState(queueId).waiting);
    }

    public static Ticket nowServing(String queueId) {
        return liveState(queueId).nowServing;
    }

    /** The person currently being served is done; the front of the line is called. */
    public static Ticket callNext(String queueId) {
        LiveQueue state = liveState(queueId);

        if (state.nowServing != null) {
            recordServed(state);
        }

        state.nowServing = state.waiting.isEmpty() ? null : state.waiting.remove(0);
        return state.nowServing;
    }

    /** The person being served didn't come up: count a no-show and call the next number. */
    public static Ticket noShowAndCallNext(String queueId) {
        LiveQueue state = liveState(queueId);

        if (state.nowServing != null) {
            state.noShows++;
        }

        state.nowServing = state.waiting.isEmpty() ? null : state.waiting.remove(0);
        return state.nowServing;
    }

    public static void markServed(String queueId, String ticketId) {
        LiveQueue state = liveState(queueId);
        if (removeById(state.waiting, ticketId)) {
            recordServed(state);
        }
    }

    public static void markNoShow(String queueId, String ticketId) {
        LiveQueue state = liveState(queueId);
        if (removeById(state.waiting, ticketId)) {
            state.noShows++;
        }
    }

    /** Someone without a phone joins at the counter. Phone stays empty. */
    public static Ticket addWalkIn(String queueId, String holderName) {
        Queue queue = queueById(queueId);
        LiveQueue state = liveState(queueId);
        int number = state.nextTicketNumber++;

        Ticket ticket = new Ticket(
                queueId + "-t" + number,
                queueId,
                queue == null ? "" : queue.getName(),
                queue == null ? "" : queue.getVenue(),
                holderName,
                "",
                number,
                state.waiting.size() + 1,
                forecastMinutes(state, state.waiting.size()),
                Ticket.Status.WAITING);

        state.waiting.add(ticket);
        return ticket;
    }

    public static QueueStats stats(String queueId) {
        LiveQueue state = liveState(queueId);
        double averageMs = averageServiceMs(state);

        return new QueueStats(
                state.served,
                state.noShows,
                state.waiting.size(),
                averageMs / 60_000d,
                state.serviceTimes.size(),
                forecastMinutes(state, state.waiting.size()));
    }

    // ---- Queuer history -----------------------------------------------------

    public static List<Ticket> history() {
        ensureSeeded();
        return new ArrayList<>(history);
    }

    /** Newest first. */
    public static void addToHistory(Ticket ticket) {
        ensureSeeded();
        history.add(0, ticket);
    }

    public static void clearHistory() {
        ensureSeeded();
        history.clear();
    }

    // ---- Internals ----------------------------------------------------------

    private static LiveQueue liveState(String queueId) {
        ensureSeeded();
        LiveQueue state = live.get(queueId);
        if (state == null) {
            state = new LiveQueue();
            live.put(queueId, state);
        }
        return state;
    }

    /** The stored Queue keeps its config; waiting count and ETA always come from live state. */
    private static Queue withLiveNumbers(Queue queue) {
        LiveQueue state = liveState(queue.getId());
        int waiting = state.waiting.size();
        int eta = queue.getStatus() == Queue.Status.CLOSED ? 0 : forecastMinutes(state, waiting);

        return queue.toBuilder()
                .setPeopleWaiting(waiting)
                .setEstimatedWaitMinutes(eta)
                .build();
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

    private static boolean removeById(List<Ticket> tickets, String ticketId) {
        for (int i = 0; i < tickets.size(); i++) {
            if (tickets.get(i).getId().equals(ticketId)) {
                tickets.remove(i);
                return true;
            }
        }
        return false;
    }

    // ---- Seed data ----------------------------------------------------------

    private static void ensureSeeded() {
        if (seeded) {
            return;
        }
        seeded = true;

        seed(new Queue.Builder()
                        .setId("q1")
                        .setName("Barangay Relief Distribution")
                        .setVenue("Brgy. Poblacion Hall")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6496, 123.8547)
                        .setCategory("Relief")
                        .setDescription("Distribution of family food packs for registered households. Bring a valid ID and your barangay certificate.")
                        .setServiceHours("Aug 30–31, 8:00 AM – 4:00 PM")
                        .setSmsOtpEnabled(true)
                        .setGracePeriodEnabled(true)
                        .setNoShowPenaltyEnabled(true)
                        .setProximityCheckEnabled(true)
                        .build(),
                42, 55, 18, 3, true);

        seed(new Queue.Builder()
                        .setId("q2")
                        .setName("Free Medical Mission")
                        .setVenue("Tagbilaran City Gym")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6543, 123.8601)
                        .setCategory("Medical")
                        .setDescription("General consultation, blood pressure screening, and free maintenance medicine for seniors.")
                        .setServiceHours("Saturday, 7:00 AM – 12:00 NN")
                        .setSmsOtpEnabled(true)
                        .setGracePeriodEnabled(true)
                        .build(),
                18, 25, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q3")
                        .setName("Barangay Clearance Processing")
                        .setVenue("Brgy. Cogon Office")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6612, 123.8578)
                        .setCategory("Government")
                        .setDescription("Application and release of barangay clearance for employment and business permits.")
                        .setServiceHours("Mon–Fri, 8:00 AM – 5:00 PM")
                        .setGracePeriodEnabled(true)
                        .build(),
                7, 12, 11, 1, true);

        seed(new Queue.Builder()
                        .setId("q4")
                        .setName("Registrar Enrollment Window 2")
                        .setVenue("BISU Main Campus")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6402, 123.8563)
                        .setCategory("Education")
                        .setDescription("Second semester enrollment for continuing students. Have your registration form pre-filled.")
                        .setServiceHours("Mon–Fri, 8:00 AM – 4:00 PM")
                        .setGracePeriodEnabled(true)
                        .setNoShowPenaltyEnabled(true)
                        .build(),
                63, 90, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q5")
                        .setName("Senior Citizen Pension Payout")
                        .setVenue("Brgy. Dao Covered Court")
                        .setMunicipality("Tagbilaran City")
                        .setLocation(9.6689, 123.8695)
                        .setCategory("Government")
                        .setDescription("Quarterly social pension release. Beneficiaries must claim in person or through an authorized representative.")
                        .setServiceHours("Sept 5, 8:00 AM – 3:00 PM")
                        .setStatus(Queue.Status.CLOSED)
                        .setSmsOtpEnabled(true)
                        .setGracePeriodEnabled(true)
                        .setNoShowPenaltyEnabled(true)
                        .setProximityCheckEnabled(true)
                        .build(),
                0, 0, 0, 0, true);

        seed(new Queue.Builder()
                        .setId("q6")
                        .setName("Anti-Rabies Vaccination Drive")
                        .setVenue("Brgy. Booy Health Center")
                        .setMunicipality("Dauis")
                        .setLocation(9.6236, 123.8478)
                        .setCategory("Medical")
                        .setDescription("Free anti-rabies vaccination for dogs and cats. One pet per queue slot.")
                        .setServiceHours("Sept 3, 8:00 AM – 2:00 PM")
                        .setGracePeriodEnabled(true)
                        .build(),
                11, 18, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q7")
                        .setName("Tourist Assistance Desk")
                        .setVenue("Alona Beach Info Center")
                        .setMunicipality("Panglao")
                        .setLocation(9.5786, 123.7486)
                        .setCategory("Community")
                        .setDescription("Walk-in assistance for lost items, transport help, and accommodation referrals.")
                        .setServiceHours("Daily, 9:00 AM – 6:00 PM")
                        .build(),
                5, 8, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q8")
                        .setName("Cash Aid Payout")
                        .setVenue("Baclayon Municipal Hall")
                        .setMunicipality("Baclayon")
                        .setLocation(9.6244, 123.9128)
                        .setCategory("Relief")
                        .setDescription("AICS financial assistance release. Claimants must present the notice sent by the MSWDO.")
                        .setServiceHours("Sept 2–4, 8:00 AM – 5:00 PM")
                        .setSmsOtpEnabled(true)
                        .setGracePeriodEnabled(true)
                        .setNoShowPenaltyEnabled(true)
                        .setProximityCheckEnabled(true)
                        .build(),
                87, 120, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q9")
                        .setName("Solo Parent ID Application")
                        .setVenue("Corella Municipal Hall")
                        .setMunicipality("Corella")
                        .setLocation(9.7089, 123.9161)
                        .setCategory("Government")
                        .setDescription("New applications and renewals for solo parent identification cards.")
                        .setServiceHours("Tue & Thu, 9:00 AM – 3:00 PM")
                        .setNoShowPenaltyEnabled(true)
                        .build(),
                3, 6, 0, 0, false);

        seed(new Queue.Builder()
                        .setId("q10")
                        .setName("School Supplies Distribution")
                        .setVenue("Loon Central Elementary")
                        .setMunicipality("Loon")
                        .setLocation(9.7986, 123.7947)
                        .setCategory("Education")
                        .setDescription("Distribution of notebooks and school kits for Grade 1–6 pupils. Parent or guardian must be present.")
                        .setServiceHours("Sept 8, 7:00 AM – 11:00 AM")
                        .setGracePeriodEnabled(true)
                        .setNoShowPenaltyEnabled(true)
                        .setProximityCheckEnabled(true)
                        .build(),
                29, 40, 0, 0, false);

        nextQueueNumber = queues.size() + 1;

        // A past visit or two so Queue History isn't empty on first open.
        history.add(pastTicket("q2", 12, Ticket.Status.SERVED));
        history.add(pastTicket("q6", 5, Ticket.Status.NO_SHOW));
        history.add(pastTicket("q3", 4, Ticket.Status.SERVED));
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

        for (int i = 0; i < waiting; i++) {
            int number = state.nextTicketNumber++;
            state.waiting.add(new Ticket(
                    queue.getId() + "-t" + number,
                    queue.getId(),
                    queue.getName(),
                    queue.getVenue(),
                    FIRST_NAMES[i % FIRST_NAMES.length] + " "
                            + LAST_NAMES[(i * 3) % LAST_NAMES.length],
                    "0917" + (1000000 + number),
                    number,
                    i + 1,
                    forecastMinutes(state, i),
                    Ticket.Status.WAITING));
        }

        live.put(queue.getId(), state);
    }

    private static Ticket pastTicket(String queueId, int number, Ticket.Status status) {
        Queue queue = null;
        for (Queue candidate : queues) {
            if (candidate.getId().equals(queueId)) {
                queue = candidate;
            }
        }
        return new Ticket(queueId + "-past" + number, queueId,
                queue == null ? "" : queue.getName(),
                queue == null ? "" : queue.getVenue(),
                "", "", number, 0, 0, status);
    }
}

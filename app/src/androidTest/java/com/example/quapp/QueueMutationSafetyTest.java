package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.time.LocalTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Disposable queues exercise the actual mutation paths, without changing saved accounts. */
@RunWith(AndroidJUnit4.class)
public class QueueMutationSafetyTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private final List<Ticket> held = new ArrayList<>();
    private Queue queue;
    private Activity activity;
    private boolean debug;

    @Before public void setup() throws Exception {
        ins.runOnMainSync(() -> {
            debug = FakeData.isDebugMode();
            FakeData.setDebugMode(false);
            held.addAll(ActiveTicketStore.tickets());
            try { ((List<?>) field(ActiveTicketStore.class, "held").get(null)).clear(); }
            catch (Exception e) { throw new AssertionError(e); }
            queue = FakeData.queues().get(0).toBuilder().setId("parity-safety-" + System.nanoTime())
                    .setName("Safety fixture").setStatus(Queue.Status.OPEN)
                    .setSchedule(Format.today(), Format.today(), LocalTime.MIN, LocalTime.MAX)
                    .setGracePeriodEnabled(false).setNoShowCooldownEnabled(false).setProximity(false, 0).build();
            FakeData.saveQueue(queue);
        });
    }

    @After @SuppressWarnings("unchecked") public void cleanup() {
        ins.runOnMainSync(() -> {
            if (activity != null) activity.finish();
            FakeData.setDebugMode(debug);
            try {
                List<Ticket> current = (List<Ticket>) field(ActiveTicketStore.class, "held").get(null);
                current.clear(); current.addAll(held);
                ((List<Queue>) field(FakeData.class, "queues").get(null)).removeIf(q -> q.getId().equals(queue.getId()));
                ((List<String>) field(FakeData.class, "ownedIds").get(null)).remove(queue.getId());
                ((Map<?, ?>) field(FakeData.class, "live").get(null)).remove(queue.getId());
                ((Map<String, Ticket>) field(FakeData.class, "finished").get(null)).values()
                        .removeIf(t -> t.getQueueId().equals(queue.getId()));
            } catch (Exception e) { throw new AssertionError(e); }
        });
        ins.waitForIdleSync();
    }

    @Test public void unavailableQueueCannotIssue() {
        ins.runOnMainSync(() -> {
            for (Queue.Status status : new Queue.Status[]{Queue.Status.UPCOMING, Queue.Status.PAUSED, Queue.Status.CLOSED}) {
                FakeData.saveQueue(queue.toBuilder().setStatus(status).build());
                try { FakeData.join(queue.getId(), "Test holder", "09171234567"); fail("Unavailable queue issued a ticket: " + status); }
                catch (IllegalStateException expected) { }
                assertTrue(FakeData.waitingTickets(queue.getId()).isEmpty());
                assertEquals(1, FakeData.nextTicketNumber(queue.getId()));
            }
        });
    }

    @Test public void closedOrForeignWalkInCannotIssue() {
        ins.runOnMainSync(() -> {
            FakeData.saveQueue(queue.toBuilder().setStatus(Queue.Status.CLOSED).build());
            try { FakeData.addWalkIn(queue.getId(), "Visitor"); fail("Closed queue issued walk-in"); }
            catch (IllegalStateException expected) { }
            assertEquals(1, FakeData.nextTicketNumber(queue.getId()));
            try { FakeData.addWalkIn("missing-walk-in", "Visitor"); fail("Missing queue issued walk-in"); }
            catch (IllegalArgumentException expected) { }
            Queue foreign = FakeData.queues().stream().filter(q -> !FakeData.ownsQueue(q.getId())).findFirst().get();
            try { FakeData.addWalkIn(foreign.getId(), "Visitor"); fail("Foreign queue issued walk-in"); }
            catch (SecurityException expected) { }
        });
    }

    @Test public void repeatJoinReturnsSameHeldTicket() {
        ins.runOnMainSync(() -> {
            Ticket first = FakeData.join(queue.getId(), "Test holder", "09171234567");
            Ticket second = FakeData.join(queue.getId(), "Test holder", "09171234567");
            assertEquals("Duplicate join issued another place", first.getId(), second.getId());
            assertTrue("Issued ticket must already be held", ActiveTicketStore.holds(first.getId()));
            assertEquals(1, FakeData.waitingTickets(queue.getId()).size());
            assertEquals(2, FakeData.nextTicketNumber(queue.getId()));
        });
    }

    @Test public void openStatusCannotBypassFutureOpeningTime() {
        ins.runOnMainSync(() -> {
            java.time.ZonedDateTime opening = java.time.ZonedDateTime.now(Format.MANILA).plusHours(1);
            Queue scheduled = queue.toBuilder().setSchedule(opening.toLocalDate(), opening.toLocalDate(),
                    opening.toLocalTime(), LocalTime.MAX).build();
            FakeData.saveQueue(scheduled);
            assertFalse(scheduled.acceptsJoins());
            try { FakeData.join(queue.getId(), "Holder", "09171234567"); fail("Joined before opening"); }
            catch (IllegalStateException expected) { }
            assertTrue(FakeData.waitingTickets(queue.getId()).isEmpty());
            assertEquals(1, FakeData.nextTicketNumber(queue.getId()));
        });
    }

    @Test public void destroyedJoinScreenCannotLoseIssuedTicket() {
        activity = ins.startActivitySync(new Intent(ins.getTargetContext(), JoinQueueActivity.class)
                .putExtra(JoinQueueActivity.EXTRA_QUEUE_ID, queue.getId()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.runOnMainSync(() -> {
            ((TextView) activity.findViewById(R.id.join_name_input)).setText("Test holder");
            ((TextView) activity.findViewById(R.id.join_phone_input)).setText("09171234567");
            assertTrue(activity.findViewById(R.id.join_submit_button).performClick());
            activity.finish();
        });
        ins.waitForIdleSync();
        ins.runOnMainSync(() -> {
            List<Ticket> waiting = FakeData.waitingTickets(queue.getId());
            assertEquals("Callback must have issued one ticket", 1, waiting.size());
            assertTrue("Ticket lost after join Activity finished", ActiveTicketStore.holds(waiting.get(0).getId()));
        });
    }

    @Test public void concurrentJoinsReserveOnlyOnePlace() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Ticket> first = new AtomicReference<>(), second = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread a = new Thread(() -> { try { start.await(); first.set(FakeData.join(queue.getId(), "Holder", "09171234567")); } catch (Throwable e) { error.set(e); } });
        Thread b = new Thread(() -> { try { start.await(); second.set(FakeData.join(queue.getId(), "Holder", "09171234567")); } catch (Throwable e) { error.set(e); } });
        a.start(); b.start(); start.countDown(); a.join(3000); b.join(3000);
        assertFalse(a.isAlive()); assertFalse(b.isAlive()); assertNull(error.get());
        assertNotNull(first.get()); assertNotNull(second.get());
        assertEquals(first.get().getId(), second.get().getId());
        assertEquals(1, FakeData.waitingTickets(queue.getId()).size());
        assertEquals(1, ActiveTicketStore.liveTickets().size());
    }

    @Test public void availabilityIsRecheckedWhenAsyncMutationExecutes() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        ins.runOnMainSync(() -> {
            FakeData.joinAsync(queue.getId(), "Holder", "09171234567", new FakeData.Callback<Ticket>() {
                @Override public void onSuccess(Ticket ticket) { error.set(new AssertionError("Stale join issued a ticket")); }
                @Override public void onError(Throwable failure) { error.set(failure); }
            });
            FakeData.saveQueue(queue.toBuilder().setStatus(Queue.Status.PAUSED).build());
        });
        ins.waitForIdleSync();
        assertTrue(error.get() instanceof IllegalStateException);
        assertTrue(FakeData.waitingTickets(queue.getId()).isEmpty());
        assertTrue(ActiveTicketStore.liveTickets().isEmpty());
    }

    @Test public void calledTicketsCanFinishAtTheirOwnCounter() {
        ins.runOnMainSync(() -> {
            Ticket first = FakeData.addWalkIn(queue.getId(), "First");
            Ticket second = FakeData.addWalkIn(queue.getId(), "Second");
            QueueCounter extra = FakeData.createCounter(queue.getId(), "Extra");
            FakeData.callNextAtCounter(queue.getId(), FakeData.MAIN_COUNTER_ID);
            FakeData.callNextAtCounter(queue.getId(), extra.getId());
            FakeData.markServed(queue.getId(), first.getId());
            assertNull(FakeData.counterServing(queue.getId(), FakeData.MAIN_COUNTER_ID));
            assertEquals(second.getId(), FakeData.counterServing(queue.getId(), extra.getId()).getId());
            FakeData.markNoShow(queue.getId(), second.getId());
            assertNull(FakeData.counterServing(queue.getId(), extra.getId()));
            assertEquals(Ticket.Status.SERVED, FakeData.ticket(queue.getId(), first.getId()).getStatus());
            assertEquals(Ticket.Status.NO_SHOW, FakeData.ticket(queue.getId(), second.getId()).getStatus());
            FakeData.markServed(queue.getId(), first.getId());
            FakeData.markNoShow(queue.getId(), second.getId());
            assertEquals(1, FakeData.stats(queue.getId()).getServedToday());
            assertEquals(1, FakeData.stats(queue.getId()).getNoShowsToday());
        });
    }

    @Test public void waitingTicketsCannotBecomeServedOrNoShow() {
        ins.runOnMainSync(() -> {
            Ticket first = FakeData.addWalkIn(queue.getId(), "Not called");
            Ticket second = FakeData.addWalkIn(queue.getId(), "Also waiting");
            FakeData.markServed(queue.getId(), first.getId());
            FakeData.markNoShow(queue.getId(), second.getId());
            assertEquals(Ticket.Status.WAITING, FakeData.ticket(queue.getId(), first.getId()).getStatus());
            assertEquals(Ticket.Status.WAITING, FakeData.ticket(queue.getId(), second.getId()).getStatus());
            assertEquals(0, FakeData.stats(queue.getId()).getServedToday());
            assertEquals(0, FakeData.stats(queue.getId()).getNoShowsToday());
        });
    }

    @Test public void arrivalStaysCalledUntilOrganizerCompletes() {
        ins.runOnMainSync(() -> {
            Ticket ticket = FakeData.join(queue.getId(), "Holder", "09171234567");
            FakeData.callNext(queue.getId());
            ActiveTicketStore.confirmArrival(ticket.getId());
            assertTrue(FakeData.arrivalConfirmed(queue.getId(), ticket.getId()));
            assertEquals(Ticket.Status.CALLED, ActiveTicketStore.ticket(ticket.getId()).getStatus());
            assertEquals(1, ActiveTicketStore.liveTickets().size());
            assertEquals(0, FakeData.stats(queue.getId()).getServedToday());
            FakeData.markServed(queue.getId(), ticket.getId());
            assertEquals(Ticket.Status.SERVED, ActiveTicketStore.ticket(ticket.getId()).getStatus());
            assertTrue(ActiveTicketStore.liveTickets().isEmpty());
        });
    }

    @Test public void confirmedArrivalSurvivesOriginalGraceDeadline() {
        ins.runOnMainSync(() -> {
            FakeData.saveQueue(queue.toBuilder().setGracePeriodEnabled(true).build());
            Ticket ticket = FakeData.join(queue.getId(), "Holder", "09171234567");
            FakeData.callNext(queue.getId());
            ActiveTicketStore.confirmArrival(ticket.getId());
            try {
                Object line = ((Map<?, ?>) field(FakeData.class, "live").get(null)).get(queue.getId());
                Field serving = field(line.getClass(), "nowServing");
                Ticket called = (Ticket) serving.get(line);
                serving.set(line, called.toBuilder().setCalledAt(Instant.now().minusSeconds(300)).build());
            } catch (Exception e) { throw new AssertionError(e); }
            assertEquals(Ticket.Status.CALLED, ActiveTicketStore.ticket(ticket.getId()).getStatus());
            assertEquals(0, FakeData.stats(queue.getId()).getNoShowsToday());
            assertTrue(FakeData.arrivalConfirmed(queue.getId(), ticket.getId()));
        });
    }

    @Test public void calledScreenConfirmsPresenceWithoutReceipt() throws Exception {
        Ticket[] ticket = new Ticket[1];
        ins.runOnMainSync(() -> {
            FakeData.saveQueue(queue.toBuilder().setGracePeriodEnabled(true).build());
            ticket[0] = FakeData.join(queue.getId(), "Holder", "09171234567");
            FakeData.callNext(queue.getId());
        });
        activity = ins.startActivitySync(CalledActivity.intent(ins.getTargetContext(), ticket[0].getId())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.runOnMainSync(() -> {
            assertTrue(activity.findViewById(R.id.called_here_button).performClick());
            assertFalse(activity.isFinishing());
            assertFalse(activity.findViewById(R.id.called_here_button).isEnabled());
            assertEquals(android.view.View.GONE, activity.findViewById(R.id.called_timer).getVisibility());
            assertEquals(activity.getString(R.string.called_waiting_completion),
                    ((TextView) activity.findViewById(R.id.called_instruction)).getText().toString());
            assertEquals(Ticket.Status.CALLED, ActiveTicketStore.ticket(ticket[0].getId()).getStatus());
        });
        SavedServicesTest.capture(ins, "arrival-confirmed");
        SavedServicesTest.revealAndCapture(ins, activity.findViewById(R.id.called_here_button), "arrival-confirmed-end");
        ins.runOnMainSync(() -> {
            android.view.View parent = activity.findViewById(R.id.called_here_button);
            while (!(parent instanceof androidx.core.widget.NestedScrollView)) parent = (android.view.View) parent.getParent();
            ((androidx.core.widget.NestedScrollView) parent).fullScroll(android.view.View.FOCUS_DOWN);
        });
        SavedServicesTest.capture(ins, "arrival-confirmed-full-scroll");
    }

    @Test public void leaveCancelPreservesTicketAndConfirmReleasesIt() {
        Ticket[] ticket = new Ticket[1];
        ins.runOnMainSync(() -> ticket[0] = FakeData.join(queue.getId(), "Holder", "09171234567"));
        activity = ins.startActivitySync(ActiveTicketActivity.intent(ins.getTargetContext(), ticket[0].getId())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.waitForIdleSync();
        ins.runOnMainSync(() -> activity.findViewById(R.id.ticket_leave_button).performClick());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button2))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(androidx.test.espresso.action.ViewActions.click());
        assertTrue(ActiveTicketStore.holds(ticket[0].getId()));
        ins.runOnMainSync(() -> activity.findViewById(R.id.ticket_leave_button).performClick());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button1))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(androidx.test.espresso.action.ViewActions.click());
        ins.waitForIdleSync();
        assertFalse(ActiveTicketStore.holds(ticket[0].getId()));
        assertNull(FakeData.ticket(queue.getId(), ticket[0].getId()));
    }

    @Test public void decliningNotificationPromptPreservesTicket() {
        android.content.SharedPreferences prefs = ins.getTargetContext().getSharedPreferences("quapp_session", 0);
        boolean had = prefs.contains("notifications_asked"); boolean prior = prefs.getBoolean("notifications_asked", false);
        Ticket[] ticket = new Ticket[1];
        try {
            ins.runOnMainSync(() -> ticket[0] = FakeData.join(queue.getId(), "Holder", "09171234567"));
            activity = ins.startActivitySync(NotificationPermissionActivity.intent(ins.getTargetContext(), ticket[0].getId())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            ins.runOnMainSync(() -> activity.findViewById(R.id.notify_not_now).performClick());
            ins.waitForIdleSync();
            assertTrue(ActiveTicketStore.holds(ticket[0].getId()));
            assertEquals(Ticket.Status.WAITING, ActiveTicketStore.ticket(ticket[0].getId()).getStatus());
            assertTrue(new Session(ins.getTargetContext()).wasAskedForNotifications());
        } finally {
            android.content.SharedPreferences.Editor restore = prefs.edit();
            if (had) restore.putBoolean("notifications_asked", prior); else restore.remove("notifications_asked");
            assertTrue(restore.commit());
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name); result.setAccessible(true); return result;
    }
}

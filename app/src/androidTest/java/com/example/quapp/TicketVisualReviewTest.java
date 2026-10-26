package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Non-shipped fixtures: no real queue actions, model APIs or session changes. */
@RunWith(AndroidJUnit4.class)
public class TicketVisualReviewTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final String worst = "👩🏽‍💻 王秀英 — Aleksandra Wiśniewska-Kowalczyk";

    @Test @SuppressWarnings("unchecked") public void reviewTicketScreens() throws Exception {
        Field field = ActiveTicketStore.class.getDeclaredField("held");
        field.setAccessible(true);
        List<Ticket> held = (List<Ticket>) field.get(null);
        List<Ticket> original = new ArrayList<>(held);
        boolean originalDebug = FakeData.isDebugMode();
        Queue queue = FakeData.queues().get(0);
        try {
            for (int screen = 0; screen < 3; screen++) {
                Ticket ticket = new Ticket.Builder().setId("phase2c-visual-only")
                    .setQueue(queue.getId(), queue.getName(), "Demo public entrance")
                    .setHolder("Fictional participant", null).setTicketNumber(64).setPosition(7)
                    .setEstimatedWaitMinutes(18).setStatus(screen == 2 ? Ticket.Status.CALLED : Ticket.Status.WAITING)
                    .setCalledAt(screen == 2 ? Instant.now() : null).build();
                instrumentation.runOnMainSync(() -> { held.clear(); held.add(ticket); });
                FakeData.setDebugMode(false);
                Activity activity = launch(screen, ticket.getId());
                try {
                    capture(activity, screen, "default");
                    if (screen == 1) {
                        NestedScrollView leaveScroll = findScroll(activity.findViewById(android.R.id.content));
                        if (leaveScroll != null) instrumentation.runOnMainSync(() -> leaveScroll.fullScroll(View.FOCUS_DOWN));
                        instrumentation.waitForIdleSync();
                        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.ticket_leave_button))
                                .perform(androidx.test.espresso.action.ViewActions.click());
                        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.ticket_leave_cancel))
                                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                                .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
                        instrumentation.getUiAutomation().waitForIdle(500, 5000);
                        android.os.SystemClock.sleep(1000);
                        capture(activity, screen, "leave-confirmation");
                        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.ticket_leave_cancel))
                                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                                .perform(androidx.test.espresso.action.ViewActions.click());
                        if (leaveScroll != null) instrumentation.runOnMainSync(() -> leaveScroll.fullScroll(View.FOCUS_UP));
                        instrumentation.waitForIdleSync();
                    }
                    int title = screen == 0 ? R.id.my_tickets_queue : screen == 1 ? R.id.ticket_queue_name : R.id.called_queue_name;
                    instrumentation.runOnMainSync(() -> ((TextView) activity.findViewById(title)).setText(worst));
                    capture(activity, screen, "unicode-long");
                    NestedScrollView scroll = findScroll(activity.findViewById(android.R.id.content));
                    if (scroll != null) {
                        instrumentation.runOnMainSync(() -> scroll.fullScroll(View.FOCUS_DOWN));
                        instrumentation.waitForIdleSync();
                        capture(activity, screen, "bottom");
                    }
                    int missing = screen == 0 ? R.id.my_tickets_detail : screen == 1 ? R.id.ticket_venue : R.id.called_venue;
                    instrumentation.runOnMainSync(() -> ((TextView) activity.findViewById(missing)).setText(""));
                    capture(activity, screen, "missing-secondary");
                } finally { instrumentation.runOnMainSync(activity::finish); }
                if (screen == 1) {
                    instrumentation.runOnMainSync(() -> {
                        held.clear(); held.add(ticket.toBuilder().setStatus(Ticket.Status.NO_SHOW).build());
                    });
                    Activity recovery = launch(screen, ticket.getId());
                    try {
                        instrumentation.waitForIdleSync();
                        capture(recovery, screen, "released-recovery");
                        NestedScrollView recoveryScroll = findScroll(recovery.findViewById(android.R.id.content));
                        if (recoveryScroll != null) instrumentation.runOnMainSync(() -> recoveryScroll.fullScroll(View.FOCUS_DOWN));
                        instrumentation.waitForIdleSync();
                        capture(recovery, screen, "released-recovery-bottom");
                    } finally { instrumentation.runOnMainSync(recovery::finish); }
                    instrumentation.runOnMainSync(() -> { held.clear(); held.add(ticket); });
                }
                if (screen == 0) {
                    instrumentation.runOnMainSync(held::clear);
                    Activity empty = launch(screen, ticket.getId());
                    try {
                        assertTrue(empty.findViewById(R.id.my_tickets_empty).isShown());
                        capture(empty, screen, "empty");
                    } finally { instrumentation.runOnMainSync(empty::finish); }
                    instrumentation.runOnMainSync(() -> held.add(ticket));
                }
                if (screen < 2) {
                    FakeData.setDebugMode(true); FakeData.setDebugDelayMs(5000); FakeData.setDebugFailOnce(true);
                    Activity error = launch(screen, ticket.getId());
                    try {
                        assertTrue(error.findViewById(screen == 0 ? R.id.my_tickets_loading : R.id.ticket_loading).isShown());
                        capture(error, screen, "loading");
                        long deadline = android.os.SystemClock.uptimeMillis() + 10000;
                        while (error.findViewById(screen == 0 ? R.id.my_tickets_loading : R.id.ticket_loading).isShown()
                                && android.os.SystemClock.uptimeMillis() < deadline) {
                            Thread.sleep(50); instrumentation.waitForIdleSync();
                        }
                        View state = error.findViewById(screen == 0 ? R.id.my_tickets_error : R.id.ticket_error);
                        if (screen == 0) assertTrue(state.isShown());
                        capture(error, screen, state.isShown() ? "error" : "settled-after-fail-once");
                        NestedScrollView scroll = findScroll(state);
                        if (scroll != null && state.isShown()) {
                            instrumentation.runOnMainSync(() -> scroll.fullScroll(View.FOCUS_DOWN));
                            NestedScrollView outer = findScroll(error.findViewById(android.R.id.content));
                            if (outer != null) instrumentation.runOnMainSync(() -> outer.fullScroll(View.FOCUS_DOWN));
                            instrumentation.waitForIdleSync();
                            capture(error, screen, "error-bottom");
                        }
                    } finally { instrumentation.runOnMainSync(error::finish); }
                }
            }
        } finally {
            instrumentation.runOnMainSync(() -> { held.clear(); held.addAll(original); });
            assertEquals(original, held);
            FakeData.setDebugMode(originalDebug); FakeData.setDebugDelayMs(1500);
            FakeData.setDebugFailOnce(false); FakeData.setDebugEmptyOnce(false);
        }
    }
    private Activity launch(int screen, String ticketId) {
        Intent intent = screen == 0 ? QueuerHomeActivity.intent(instrumentation.getTargetContext(), QueuerHomeActivity.TAB_TICKETS)
            : screen == 1 ? ActiveTicketActivity.intent(instrumentation.getTargetContext(), ticketId)
            : CalledActivity.intent(instrumentation.getTargetContext(), ticketId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return instrumentation.startActivitySync(intent);
    }
    private NestedScrollView findScroll(View view) {
        if (view instanceof NestedScrollView) return (NestedScrollView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            NestedScrollView found = findScroll(((ViewGroup) view).getChildAt(i)); if (found != null) return found;
        }
        return null;
    }
    private void capture(Activity activity, int screen, String state) throws Exception {
        Thread.sleep(160);
        File folder = new File(activity.getExternalFilesDir(null), "phase2c-review");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        String name = new String[]{"ticket-list", "ticket-waiting", "ticket-called"}[screen];
        android.content.res.Configuration config = activity.getResources().getConfiguration();
        try (FileOutputStream out = new FileOutputStream(new File(folder, name + "-" + state + "-" + config.screenWidthDp + "x" + config.screenHeightDp + "-" + config.fontScale + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }
}

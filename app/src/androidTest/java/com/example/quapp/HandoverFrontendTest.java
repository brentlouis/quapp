package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.View;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HandoverFrontendTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();

    @Test @SuppressWarnings("unchecked") public void unavailableBoundaryNeverTransfersOwnership() throws Exception {
        Queue seed = FakeData.ownedQueues().get(0);
        Field field = FakeData.class.getDeclaredField("queues");
        field.setAccessible(true);
        List<Queue> queues = (List<Queue>) field.get(null);
        List<Queue> saved = new ArrayList<>(queues);
        Field ownedField = FakeData.class.getDeclaredField("ownedIds"); ownedField.setAccessible(true);
        List<String> owned = (List<String>) ownedField.get(null);
        List<String> savedOwned = new ArrayList<>(owned);
        Queue original = seed.toBuilder().setId("handover-isolated-fixture").setGracePeriodEnabled(false).build();
        ins.runOnMainSync(() -> { FakeData.saveQueue(original); FakeData.addWalkIn(original.getId(), "Handover fixture"); });
        int matched = -1;
        for (int i = 0; i < queues.size(); i++) if (original.getId().equals(queues.get(i).getId())) matched = i;
        assertTrue("Owned queue fixture exists", matched >= 0);
        final int position = matched;
        int waiting = FakeData.waitingTickets(original.getId()).size();
        Activity console = null;
        try {
            console = ins.startActivitySync(new Intent(ins.getTargetContext(), LiveConsoleActivity.class)
                    .putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID, original.getId()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity caller = console;
            ins.runOnMainSync(() -> caller.findViewById(R.id.console_menu).performClick());
            onView(withText(R.string.handover_title)).inRoot(isDialog()).perform(new androidx.test.espresso.ViewAction() {
                public org.hamcrest.Matcher<View> getConstraints() { return isAssignableFrom(View.class); }
                public String getDescription() { return "Activate actual Handover menu callback"; }
                public void perform(androidx.test.espresso.UiController controller, View view) {
                    View row = view;
                    while (!row.isClickable() && row.getParent() instanceof View) row = (View) row.getParent();
                    assertTrue(row.performClick());
                }
            });
            OrganizerHandoverActivity routed = resumed();
            assertEquals(original.getId(), routed.getIntent().getStringExtra(OrganizerHandoverActivity.EXTRA_QUEUE_ID));
            ins.runOnMainSync(() -> routed.findViewById(R.id.handover_back).performClick());
            ins.waitForIdleSync();
            assertFalse(caller.isFinishing());
            ins.runOnMainSync(caller::finish);
            console = null;

            String[] names = {original.getName(), "Community assistance 公共服务 👩🏽‍💼 " + "VeryLongQueueNameWithoutBreaks".repeat(6), ""};
            String[] labels = {"normal", "unicode-long", "missing"};
            for (int i = 0; i < names.length; i++) {
                Queue fixture = original.toBuilder().setName(names[i]).build();
                ins.runOnMainSync(() -> queues.set(position, fixture));
                try (ActivityScenario<OrganizerHandoverActivity> scenario = ActivityScenario.launch(
                        OrganizerHandoverActivity.intent(ins.getTargetContext(), fixture.getId()))) {
                    scenario.onActivity(screen -> {
                        assertEquals(Validation.isBlank(fixture.getName()) ? screen.getString(R.string.handover_queue_unnamed) : fixture.getName(),
                                ((TextView) screen.findViewById(R.id.handover_queue)).getText().toString());
                        assertFalse(screen.findViewById(R.id.handover_transfer).isEnabled());
                        assertFalse(screen.findViewById(R.id.handover_transfer).performClick());
                        screen.findViewById(R.id.handover_retry).performClick();
                        assertEquals(screen.getString(R.string.handover_still_unavailable), ((TextView) screen.findViewById(R.id.handover_message)).getText().toString());
                    });
                    for (int actionId : new int[]{R.id.handover_retry, R.id.handover_transfer}) reveal(scenario, actionId);
                    scenario.onActivity(screen -> {
                        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, screen.findViewById(R.id.handover_message).getAccessibilityLiveRegion());
                    });
                    capture(labels[i]);
                    if (i == 1) {
                        scenario.recreate();
                        scenario.onActivity(screen -> assertFalse(screen.findViewById(R.id.handover_transfer).isEnabled()));
                    }
                }
                assertEquals(original.getOrganizerId(), FakeData.queueById(original.getId()).getOrganizerId());
                assertEquals(waiting, FakeData.waitingTickets(original.getId()).size());
            }
            Queue foreign = original.toBuilder().setId("handover-foreign-fixture").setOrganizer("other", "Other", true).build();
            ins.runOnMainSync(() -> queues.add(foreign));
            for (String denied : new String[]{foreign.getId(), "missing-handover-queue"}) {
                try (ActivityScenario<OrganizerHandoverActivity> scenario = ActivityScenario.launch(OrganizerHandoverActivity.intent(ins.getTargetContext(), denied))) {
                    assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
                }
            }
        } finally {
            Activity remaining = console;
            ins.runOnMainSync(() -> {
                if (remaining != null) remaining.finish(); queues.clear(); queues.addAll(saved);
                owned.clear(); owned.addAll(savedOwned);
                try {
                    Field liveField = FakeData.class.getDeclaredField("live"); liveField.setAccessible(true);
                    ((java.util.Map<?, ?>) liveField.get(null)).remove(original.getId());
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
            ins.waitForIdleSync();
        }
    }

    private OrganizerHandoverActivity resumed() {
        final OrganizerHandoverActivity[] found = {null};
        long deadline = android.os.SystemClock.uptimeMillis() + 5000;
        do {
            ins.waitForIdleSync();
            ins.runOnMainSync(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                    if (activity instanceof OrganizerHandoverActivity) found[0] = (OrganizerHandoverActivity) activity;
            });
            if (found[0] != null) return found[0];
            android.os.SystemClock.sleep(16);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Console menu did not open Handover");
    }

    private void reveal(ActivityScenario<OrganizerHandoverActivity> scenario, int id) {
        final boolean[] visible = {false};
        long deadline = android.os.SystemClock.uptimeMillis() + 5000;
        do {
            ins.waitForIdleSync();
            scenario.onActivity(screen -> {
                View action = screen.findViewById(id);
                action.requestRectangleOnScreen(new Rect(0, 0, action.getWidth(), action.getHeight()), true);
                Rect bounds = new Rect();
                visible[0] = !action.isLayoutRequested() && action.getGlobalVisibleRect(bounds)
                        && bounds.height() == action.getHeight();
                assertTrue(action.getHeight() >= screen.getResources().getDimensionPixelSize(R.dimen.touch_target_min));
            });
            if (visible[0]) return;
            android.os.SystemClock.sleep(16);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        fail("Action cannot be revealed: " + id);
    }

    private void capture(String fixture) throws Exception {
        Configuration config = ins.getTargetContext().getResources().getConfiguration();
        String label = config.fontScale > 1f ? "large" : config.orientation == Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "standard";
        try (java.io.InputStream stream = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                ins.getUiAutomation().executeShellCommand("screencap -p /sdcard/Download/quapp-handover-" + label + "-" + fixture + ".png"))) {
            byte[] buffer = new byte[4096];
            while (stream.read(buffer) != -1) { }
        }
    }
}

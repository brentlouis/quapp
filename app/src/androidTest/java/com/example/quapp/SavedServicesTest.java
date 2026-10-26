package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.google.android.material.button.MaterialButton;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SavedServicesTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private final ArrayList<Activity> opened = new ArrayList<>();

    @Test @SuppressWarnings("unchecked") public void saveRemoveRecreateAndClearStayLocal() throws Exception {
        SharedPreferences prefs = ins.getTargetContext().getSharedPreferences("quapp_saved_services", 0);
        Map<String, ?> prior = new HashMap<>(prefs.getAll());
        boolean debug = FakeData.isDebugMode();
        try {
            ins.runOnMainSync(() -> { FakeData.setDebugMode(false); new SavedServices(ins.getTargetContext()).clear(); });
            Activity saved = launch(SavedServicesActivity.class, null);
            ins.waitForIdleSync();
            assertEquals(View.VISIBLE, saved.findViewById(R.id.saved_empty).getVisibility());
            capture(ins, "saved-empty");
            revealAndCapture(ins, saved.findViewById(R.id.saved_browse), "saved-empty-end");
            String id = FakeData.queues().get(0).getId();
            final Activity bindingHost = saved;
            ins.runOnMainSync(() -> {
                RecyclerView list = bindingHost.findViewById(R.id.saved_list);
                OpenNowAdapter browseRows = new OpenNowAdapter(queue -> { });
                browseRows.submitQueues(java.util.Collections.singletonList(FakeData.queueById(id)));
                OpenNowAdapter.OpenNowViewHolder browseRow = browseRows.onCreateViewHolder(list, 0);
                browseRows.onBindViewHolder(browseRow, 0);
                assertEquals(View.GONE, browseRow.itemView.findViewById(R.id.open_now_save).getVisibility());
                assertEquals(View.VISIBLE, browseRow.itemView.findViewById(R.id.open_now_chevron).getVisibility());
                OpenNowAdapter savedRows = new OpenNowAdapter(queue -> { }, () -> { });
                savedRows.submitQueues(java.util.Collections.singletonList(FakeData.queueById(id).toBuilder().setStatus(Queue.Status.CLOSED).build()));
                OpenNowAdapter.OpenNowViewHolder closed = savedRows.onCreateViewHolder(list, 0);
                savedRows.onBindViewHolder(closed, 0);
                assertTrue(((android.widget.TextView) closed.itemView.findViewById(R.id.open_now_detail)).getText().toString()
                        .contains(bindingHost.getString(R.string.detail_status_closed)));
                assertEquals(View.VISIBLE, closed.itemView.findViewById(R.id.open_now_save).getVisibility());
                OpenNowAdapter directRows = new OpenNowAdapter(queue -> fail("Bookmark opened detail"), () -> { });
                directRows.submitQueues(java.util.Collections.singletonList(FakeData.queueById(id)));
                OpenNowAdapter.OpenNowViewHolder direct = directRows.onCreateViewHolder(list, 0);
                directRows.onBindViewHolder(direct, 0);
                View rowSave = direct.itemView.findViewById(R.id.open_now_save);
                assertTrue(rowSave.performClick());
                assertTrue(new SavedServices(bindingHost).contains(id));
                directRows.onBindViewHolder(direct, 0);
                assertTrue(rowSave.isSelected());
                assertTrue(rowSave.performClick());
                assertFalse(new SavedServices(bindingHost).contains(id));
                QueueAdapter cards = new QueueAdapter(queue -> fail("Bookmark opened detail"), true);
                cards.submitQueues(java.util.Collections.singletonList(FakeData.queueById(id)));
                QueueAdapter.QueueViewHolder card = cards.onCreateViewHolder(list, 0);
                cards.onBindViewHolder(card, 0);
                View cardSave = card.itemView.findViewById(R.id.card_save);
                assertEquals(View.VISIBLE, cardSave.getVisibility());
                assertTrue(cardSave.performClick());
                assertTrue(new SavedServices(bindingHost).contains(id));
                cards.onBindViewHolder(card, 0);
                assertTrue(cardSave.isSelected());
                cardSave.performClick();
                assertFalse(new SavedServices(bindingHost).contains(id));
                QueueAdapter ownerCards = new QueueAdapter(queue -> { });
                ownerCards.submitQueues(java.util.Collections.singletonList(FakeData.queueById(id)));
                ownerCards.onBindViewHolder(card, 0);
                assertEquals(View.GONE, cardSave.getVisibility());
            });
            Activity detail = launch(QueueDetailActivity.class, id);
            ins.waitForIdleSync();
            ins.runOnMainSync(() -> {
                MaterialButton save = detail.findViewById(R.id.detail_save);
                assertFalse(save.isChecked()); assertTrue(save.performClick()); assertTrue(save.isChecked());
                assertTrue(new SavedServices(detail).contains(id));
            });
            capture(ins, "saved-detail");
            ins.runOnMainSync(detail::finish); ins.waitForIdleSync();
            final Activity previous = saved;
            ins.runOnMainSync(previous::recreate);
            saved = awaitRecreated(previous);
            ins.waitForIdleSync();
            final Activity current = saved;
            ins.runOnMainSync(() -> {
                RecyclerView list = current.findViewById(R.id.saved_list);
                assertEquals(1, list.getAdapter().getItemCount());
                assertTrue(new SavedServices(current).contains(id));
                assertTrue(list.findViewHolderForAdapterPosition(0).itemView.performClick());
            });
            ins.waitForIdleSync();
            Activity reopened = resumed(QueueDetailActivity.class); opened.add(reopened);
            assertEquals(id, reopened.getIntent().getStringExtra(QueueDetailActivity.EXTRA_QUEUE_ID));
            ins.runOnMainSync(() -> {
                MaterialButton save = reopened.findViewById(R.id.detail_save);
                assertTrue(save.isChecked()); save.performClick();
                assertFalse(new SavedServices(reopened).contains(id));
                reopened.finish();
            });
            ins.waitForIdleSync();
            assertEquals(View.VISIBLE, current.findViewById(R.id.saved_empty).getVisibility());
            ins.runOnMainSync(() -> { new SavedServices(current).toggle(id); });
            ins.runOnMainSync(current::recreate);
            Activity removeFixture = awaitRecreated(current); ins.waitForIdleSync();
            capture(ins, "saved-populated");
            ins.runOnMainSync(() -> {
                RecyclerView list = removeFixture.findViewById(R.id.saved_list);
                View remove = list.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.open_now_save);
                assertTrue(remove.performClick());
                assertFalse(new SavedServices(removeFixture).contains(id));
                assertEquals(View.VISIBLE, removeFixture.findViewById(R.id.saved_empty).getVisibility());
                new SavedServices(removeFixture).toggle(id);
                removeFixture.recreate();
            });
            Activity populated = awaitRecreated(removeFixture); ins.waitForIdleSync();
            capture(ins, "saved-populated");
            ins.runOnMainSync(() -> {
                Set<String> copy = new SavedServices(populated).ids(); copy.clear();
                assertTrue(new SavedServices(populated).contains(id));
            });
            ins.runOnMainSync(() -> populated.findViewById(R.id.saved_clear).performClick());
            ins.waitForIdleSync();
            onDialog(android.R.id.button2); // Cancel preserves bookmarks.
            assertTrue(new SavedServices(populated).contains(id));
            ins.runOnMainSync(() -> populated.findViewById(R.id.saved_clear).performClick());
            ins.waitForIdleSync(); onDialog(android.R.id.button1);
            assertTrue(new SavedServices(populated).ids().isEmpty());
            assertEquals(View.VISIBLE, populated.findViewById(R.id.saved_empty).getVisibility());
            ins.runOnMainSync(() -> {
                new SavedServices(populated).toggle("unavailable-saved-fixture");
                populated.recreate();
            });
            Activity unavailable = awaitRecreated(populated); ins.waitForIdleSync();
            assertTrue(new SavedServices(unavailable).contains("unavailable-saved-fixture"));
            assertEquals(View.VISIBLE, unavailable.findViewById(R.id.saved_empty).getVisibility());
            assertEquals(unavailable.getString(R.string.saved_unavailable_title),
                    ((android.widget.TextView) unavailable.findViewById(R.id.saved_empty_title)).getText().toString());
        } finally {
            ins.runOnMainSync(() -> { for (int i = opened.size() - 1; i >= 0; i--) opened.get(i).finish(); FakeData.setDebugMode(debug); });
            ins.waitForIdleSync();
            SharedPreferences.Editor restore = prefs.edit().clear();
            for (Map.Entry<String, ?> entry : prior.entrySet()) restore.putStringSet(entry.getKey(), (Set<String>) entry.getValue());
            assertTrue(restore.commit()); assertEquals(prior, prefs.getAll());
        }
    }

    private Activity launch(Class<?> type, String id) {
        Intent intent = new Intent(ins.getTargetContext(), type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (id != null) intent.putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, id);
        Activity activity = ins.startActivitySync(intent); opened.add(activity); return activity;
    }
    private Activity resumed(Class<?> type) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        do {
            ins.waitForIdleSync();
            Activity[] result = new Activity[1];
            ins.runOnMainSync(() -> { for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                if (type.isInstance(activity)) result[0] = activity; });
            if (result[0] != null) return result[0];
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Activity did not resume: " + type.getSimpleName());
    }
    private Activity awaitRecreated(Activity previous) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            ins.waitForIdleSync();
            Activity[] found = new Activity[1];
            ins.runOnMainSync(() -> { for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                if (activity instanceof SavedServicesActivity && activity != previous) found[0] = activity; });
            if (found[0] != null) { opened.add(found[0]); return found[0]; }
        }
        throw new AssertionError("Saved-services Activity did not recreate");
    }
    private void onDialog(int id) throws Exception {
        capture(ins, "clear-dialog-" + id);
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(id))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).check((view, error) -> {
                    if (error != null) throw error;
                    android.graphics.Rect visible = new android.graphics.Rect();
                    assertTrue(view.getGlobalVisibleRect(visible));
                    assertEquals("Dialog action fully visible", view.getHeight(), visible.height());
                    assertTrue(view.getHeight() >= view.getResources().getDimensionPixelSize(R.dimen.touch_target_min));
                }).perform(androidx.test.espresso.action.ViewActions.click());
        ins.waitForIdleSync();
    }
    static android.graphics.Point capture(Instrumentation ins, String name) throws Exception {
        ins.waitForIdleSync();
        ins.getUiAutomation().waitForIdle(750, 5000);
        Bitmap screenshot = ins.getUiAutomation().takeScreenshot(); assertNotNull(screenshot);
        File directory = new File(ins.getTargetContext().getExternalFilesDir(null), "full-parity-20261021");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        String config = InstrumentationRegistry.getArguments().getString("parityConfig", "normal");
        try (FileOutputStream out = new FileOutputStream(new File(directory, config + "-" + name + ".png"))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, out));
            return new android.graphics.Point(screenshot.getWidth(), screenshot.getHeight());
        } finally { screenshot.recycle(); }
    }

    static void revealAndCapture(Instrumentation ins, View target, String name) throws Exception {
        ins.runOnMainSync(() -> target.requestRectangleOnScreen(
                new android.graphics.Rect(0, 0, target.getWidth(), target.getHeight()), true));
        android.graphics.Point pixels = capture(ins, name);
        ins.runOnMainSync(() -> {
            android.graphics.Rect visible = new android.graphics.Rect();
            assertTrue(target.getGlobalVisibleRect(visible));
            assertEquals("Scrolled action fully visible", target.getHeight(), visible.height());
            assertTrue(target.getHeight() >= target.getResources().getDimensionPixelSize(R.dimen.touch_target_min));
            int[] position = new int[2]; target.getLocationOnScreen(position);
            assertTrue("Action extends below screenshot: y=" + position[1] + " h=" + target.getHeight() + " screen=" + pixels.y,
                    position[1] + target.getHeight() <= pixels.y);
        });
    }
}

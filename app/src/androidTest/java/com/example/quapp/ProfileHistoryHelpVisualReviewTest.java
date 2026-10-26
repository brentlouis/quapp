package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.KeyEvent;
import android.view.inspector.WindowInspector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/** Non-shipped fixtures; restore exact history/debug state, never invoke support email. */
@RunWith(AndroidJUnit4.class)
public class ProfileHistoryHelpVisualReviewTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final String worst = "👩🏽‍💻 王秀英 — Aleksandra Wiśniewska-Kowalczyk";

    @Test @SuppressWarnings("unchecked") public void reviewScreens() throws Exception {
        Field historyField = FakeData.class.getDeclaredField("history"); historyField.setAccessible(true);
        List<Ticket> history = (List<Ticket>) historyField.get(null);
        List<Ticket> original = new ArrayList<>(history);
        Map<Field, Object> debug = new LinkedHashMap<>();
        for (String name : new String[]{"debugMode", "debugDelayMs", "debugFailOnce", "debugEmptyOnce"}) {
            Field field = FakeData.class.getDeclaredField(name); field.setAccessible(true); debug.put(field, field.get(null));
        }
        Queue queue = FakeData.queues().get(0);
        Ticket ticket = new Ticket.Builder().setId("phase2d-visual-only").setQueue(queue.getId(), "Demo past visit", "Demo public entrance")
            .setHolder("Fictional participant", null).setStatus(Ticket.Status.SERVED).setTicketNumber(64).setFinishedAt(Instant.now()).build();
        try {
            instrumentation.runOnMainSync(() -> { history.clear(); history.add(ticket); });
            FakeData.setDebugMode(false); FakeData.setDebugFailOnce(false); FakeData.setDebugEmptyOnce(false);
            for (int screen = 0; screen < 3; screen++) {
                Activity activity = launch(screen);
                try {
                    capture(activity, screen, "default");
                    if (screen == 0) {
                        TextView title = activity.findViewById(R.id.history_title);
                        instrumentation.runOnMainSync(() -> title.setText(worst));
                    } else if (screen == 1) {
                        instrumentation.runOnMainSync(() -> {
                            ((TextView) activity.findViewById(R.id.profile_name)).setText(worst);
                            ((TextView) activity.findViewById(R.id.profile_summary_status)).setText("Demo account");
                        });
                    } else {
                        instrumentation.runOnMainSync(() -> ((TextView) activity.findViewById(R.id.help_queues_row).findViewById(R.id.row_title)).setText(worst));
                    }
                    capture(activity, screen, "unicode-long");
                    scrollBottom(activity, screen, "bottom");
                    if (screen == 0) {
                        instrumentation.runOnMainSync(() -> ((TextView) activity.findViewById(R.id.history_details)).setText(""));
                        scrollBottom(activity, screen, "missing-secondary");
                    }
                    if (screen == 1) {
                        instrumentation.runOnMainSync(() -> {
                            ((TextView) activity.findViewById(R.id.profile_summary_status)).setText("");
                            findScroll(activity.findViewById(android.R.id.content)).fullScroll(View.FOCUS_UP);
                        });
                        capture(activity, screen, "missing-secondary");
                    }
                    if (screen == 2) {
                        instrumentation.runOnMainSync(() -> ((TextView) activity.findViewById(R.id.help_email_row).findViewById(R.id.row_subtitle)).setText(""));
                        scrollBottom(activity, screen, "missing-secondary");
                        instrumentation.runOnMainSync(() -> activity.findViewById(R.id.help_queues_row).performClick());
                        Thread.sleep(600);
                        View[] topic = new View[1];
                        instrumentation.runOnMainSync(() -> {
                            for (View window : WindowInspector.getGlobalWindowViews()) {
                                View body = window.findViewById(R.id.help_sheet_body);
                                if (body != null && body.isShown()) topic[0] = window;
                            }
                        });
                        assertNotNull("Actual topic sheet body visible", topic[0]);
                        instrumentation.runOnMainSync(() -> BottomSheetBehavior.from(topic[0].findViewById(com.google.android.material.R.id.design_bottom_sheet)).setState(BottomSheetBehavior.STATE_EXPANDED));
                        Thread.sleep(400);
                        capture(activity, screen, "topic-sheet");
                        NestedScrollView topicScroll = findScroll(topic[0]);
                        assertNotNull(topicScroll);
                        instrumentation.runOnMainSync(() -> topicScroll.scrollTo(0, topicScroll.getChildAt(0).getHeight()));
                        capture(activity, screen, "topic-bottom");
                        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                        BottomSheetDialog[] dialog = new BottomSheetDialog[1];
                        instrumentation.runOnMainSync(() -> {
                            dialog[0] = new BottomSheetDialog(activity);
                            View content = activity.getLayoutInflater().inflate(R.layout.sheet_help, null);
                            ((TextView) content.findViewById(R.id.help_sheet_title)).setText(worst);
                            ((TextView) content.findViewById(R.id.help_sheet_body)).setText(worst + "\n\n" + activity.getString(R.string.help_queues_body));
                            dialog[0].setContentView(content); dialog[0].show();
                        });
                        Thread.sleep(600);
                        capture(activity, screen, "sheet-unicode-fixture");
                        instrumentation.runOnMainSync(dialog[0]::dismiss);
                    }
                } finally { instrumentation.runOnMainSync(activity::finish); }
            }
            instrumentation.runOnMainSync(history::clear);
            Activity empty = launch(0);
            try { assertTrue(empty.findViewById(R.id.history_empty).isShown()); capture(empty, 0, "empty"); scrollBottom(empty, 0, "empty-bottom"); }
            finally { instrumentation.runOnMainSync(empty::finish); }
            instrumentation.runOnMainSync(() -> history.add(ticket));
            FakeData.setDebugMode(true); FakeData.setDebugDelayMs(1200); FakeData.setDebugFailOnce(true);
            Activity error = launch(0);
            try {
                assertTrue(error.findViewById(R.id.history_loading).isShown()); capture(error, 0, "loading");
                Thread.sleep(1400); instrumentation.waitForIdleSync();
                assertTrue(error.findViewById(R.id.history_error).isShown()); capture(error, 0, "error"); scrollBottom(error, 0, "error-bottom");
            } finally { instrumentation.runOnMainSync(error::finish); }
        } finally {
            instrumentation.runOnMainSync(() -> { history.clear(); history.addAll(original); });
            for (Map.Entry<Field, Object> entry : debug.entrySet()) entry.getKey().set(null, entry.getValue());
            assertEquals(original, history);
            for (Map.Entry<Field, Object> entry : debug.entrySet()) assertEquals(entry.getValue(), entry.getKey().get(null));
        }
    }
    private Activity launch(int screen) {
        Intent intent = screen == 1 ? QueuerHomeActivity.intent(instrumentation.getTargetContext(), QueuerHomeActivity.TAB_PROFILE)
            : new Intent(instrumentation.getTargetContext(), screen == 0 ? HistoryActivity.class : HelpActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); return instrumentation.startActivitySync(intent);
    }
    private NestedScrollView findScroll(View view) {
        if (view instanceof NestedScrollView) return (NestedScrollView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            NestedScrollView found = findScroll(((ViewGroup) view).getChildAt(i)); if (found != null) return found;
        }
        return null;
    }
    private void scrollBottom(Activity activity, int screen, String state) throws Exception {
        NestedScrollView scroll = findScroll(activity.findViewById(android.R.id.content));
        if (scroll != null) { instrumentation.runOnMainSync(() -> scroll.fullScroll(View.FOCUS_DOWN)); instrumentation.waitForIdleSync(); capture(activity, screen, state); }
    }
    private void capture(Activity activity, int screen, String state) throws Exception {
        Thread.sleep(180);
        File folder = new File(activity.getExternalFilesDir(null), "phase2d-review"); assertTrue(folder.isDirectory() || folder.mkdirs());
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        android.content.res.Configuration config = activity.getResources().getConfiguration();
        String name = new String[]{"ticket-history", "profile-overview", "profile-help"}[screen];
        try (FileOutputStream out = new FileOutputStream(new File(folder, name + "-" + state + "-" + config.screenWidthDp + "x" + config.screenHeightDp + "-" + config.fontScale + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }
}

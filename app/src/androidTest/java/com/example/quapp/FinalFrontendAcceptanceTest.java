package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.checkbox.MaterialCheckBox;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FinalFrontendAcceptanceTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();

    @Test public void browseBookmarkIsReachableAfterHeaderCollapse() throws Exception {
        android.content.SharedPreferences prefs = ins.getTargetContext().getSharedPreferences("quapp_session", 0);
        boolean hadTown = prefs.contains("town"); String town = prefs.getString("town", null);
        boolean debug = FakeData.isDebugMode(); FakeData.setDebugMode(false);
        new Session(ins.getTargetContext()).setTown("Tagbilaran City");
        Activity home = ins.startActivitySync(QueuerHomeActivity.intent(ins.getTargetContext(), QueuerHomeActivity.TAB_BROWSE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            ins.waitForIdleSync();
            ins.runOnMainSync(() -> ((com.google.android.material.appbar.AppBarLayout) home.findViewById(R.id.browse_app_bar)).setExpanded(false, false));
            ins.waitForIdleSync();
            View[] save = new View[1];
            ins.runOnMainSync(() -> {
                androidx.recyclerview.widget.RecyclerView list = home.findViewById(R.id.browse_list);
                assertTrue(list.getAdapter().getItemCount() > 0);
                save[0] = list.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.card_save);
                assertNotNull(save[0]); assertEquals(View.VISIBLE, save[0].getVisibility());
                java.util.Set<String> before = new SavedServices(home).ids();
                assertTrue(save[0].performClick());
                assertNotEquals(before, new SavedServices(home).ids());
                assertTrue(save[0].performClick());
                assertEquals(before, new SavedServices(home).ids());
            });
            SavedServicesTest.revealAndCapture(ins, save[0], "browse-direct-bookmark");
        } finally {
            ins.runOnMainSync(home::finish); FakeData.setDebugMode(debug);
            android.content.SharedPreferences.Editor restore = prefs.edit();
            if (hadTown) restore.putString("town", town); else restore.remove("town");
            assertTrue(restore.commit());
        }
    }

    @Test public void preparationSurvivesActivityRecreation() {
        boolean debug = FakeData.isDebugMode();
        FakeData.setDebugMode(false);
        Intent intent = new Intent(ins.getTargetContext(), QueueDetailActivity.class)
                .putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, FakeData.queues().get(0).getId());
        try (androidx.test.core.app.ActivityScenario<QueueDetailActivity> scenario = androidx.test.core.app.ActivityScenario.launch(intent)) {
            ins.waitForIdleSync();
            scenario.onActivity(a -> ((MaterialCheckBox) a.findViewById(R.id.detail_bring)).setChecked(true));
            scenario.recreate(); ins.waitForIdleSync();
            scenario.onActivity(a -> assertTrue(((MaterialCheckBox) a.findViewById(R.id.detail_bring)).isChecked()));
        } finally { FakeData.setDebugMode(debug); }
    }

    @Test public void preparationReminderAndSupportStayLocal() throws Exception {
        boolean debug = FakeData.isDebugMode();
        ins.runOnMainSync(() -> FakeData.setDebugMode(false));
        Queue queue = FakeData.queues().get(0);
        Activity detail = ins.startActivitySync(new Intent(ins.getTargetContext(), QueueDetailActivity.class)
                .putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, queue.getId()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            ins.waitForIdleSync();
            ins.runOnMainSync(() -> {
                MaterialCheckBox preparation = detail.findViewById(R.id.detail_bring);
                assertEquals(View.VISIBLE, preparation.getVisibility());
                assertFalse(preparation.isChecked());
                preparation.performClick(); assertTrue(preparation.isChecked());
                assertNull(ActiveTicketStore.ticketForQueue(queue.getId()));
                LinearLayout slip = detail.findViewById(R.id.detail_slip);
                for (int i = 0; i < slip.getChildCount(); i++) {
                    android.widget.TextView value = slip.getChildAt(i).findViewById(R.id.slip_value);
                    assertNotNull(value.getLayout());
                    for (int line = 0; line < value.getLineCount(); line++) {
                        assertEquals(0, value.getLayout().getEllipsisCount(line));
                        assertTrue("Slip text=" + value.getText() + " line=" + line + " measured="
                                + value.getLayout().getLineMax(line) + " width=" + value.getWidth(),
                                value.getLayout().getLineMax(line) <= value.getWidth() + 1);
                    }
                }
                assertTrue(detail.getString(R.string.forgot_support_email).isEmpty());
            });
            ins.waitForIdleSync();
            assertFalse(detail.isFinishing());
            android.os.SystemClock.sleep(4000); // Let the preceding report's system toast leave the screenshot.
            SavedServicesTest.revealAndCapture(ins, detail.findViewById(R.id.detail_bring), "preparation-reminder");
            ins.runOnMainSync(() -> Support.email(detail, "Test", "No personal information"));
            ins.waitForIdleSync();
            assertFalse(detail.isFinishing());
        } finally {
            ins.runOnMainSync(() -> { detail.finish(); FakeData.setDebugMode(debug); });
        }
    }

    @Test public void repeatedReportTapDoesNotDuplicatePendingReport() throws Exception {
        boolean debug = FakeData.isDebugMode();
        java.lang.reflect.Field delayField = FakeData.class.getDeclaredField("debugDelayMs"); delayField.setAccessible(true);
        long delay = delayField.getLong(null);
        java.lang.reflect.Field failureField = FakeData.class.getDeclaredField("debugFailOnce"); failureField.setAccessible(true);
        boolean failure = failureField.getBoolean(null);
        java.lang.reflect.Field reportsField = FakeData.class.getDeclaredField("reports"); reportsField.setAccessible(true);
        java.util.List<Report> prior = FakeData.reports();
        int before = FakeData.reports().size();
        ins.runOnMainSync(() -> { FakeData.setDebugMode(true); FakeData.setDebugDelayMs(500); FakeData.setDebugFailOnce(false); });
        Activity report = ins.startActivitySync(ReportQueueActivity.intent(ins.getTargetContext(), FakeData.queues().get(0).getId())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            ins.runOnMainSync(() -> {
                report.findViewById(R.id.report_send).performClick();
                assertEquals(View.VISIBLE, report.findViewById(R.id.report_reason_error).getVisibility());
                ((LinearLayout) report.findViewById(R.id.report_reasons)).getChildAt(0).performClick();
                report.findViewById(R.id.report_send).performClick();
                report.findViewById(R.id.report_send).performClick();
            });
            android.os.SystemClock.sleep(900);
            ins.waitForIdleSync();
            assertEquals(before + 1, FakeData.reports().size());
            assertTrue(report.isFinishing() || report.isDestroyed());
            assertTrue(report.getString(R.string.report_sent).contains("Not sent"));
        } finally {
            ins.runOnMainSync(() -> { report.finish(); FakeData.setDebugMode(debug); FakeData.setDebugDelayMs(delay); FakeData.setDebugFailOnce(failure); });
            @SuppressWarnings("unchecked") java.util.List<Report> reports = (java.util.List<Report>) reportsField.get(null);
            reports.clear(); reports.addAll(prior);
        }
    }
}

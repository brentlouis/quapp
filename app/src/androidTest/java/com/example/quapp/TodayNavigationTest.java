package com.example.quapp;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Rect;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TodayNavigationTest {
    @Test public void manageQueuesReusesTabAndPreservesFilter() throws Exception {
        Instrumentation ins = InstrumentationRegistry.getInstrumentation();
        OwnerHomeActivity home = (OwnerHomeActivity) ins.startActivitySync(
                new Intent(ins.getTargetContext(), OwnerHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            final Fragment[] original = {null};
            final int[] filter = {0};
            ins.runOnMainSync(() -> {
                original[0] = home.getSupportFragmentManager().findFragmentByTag("queues");
                ChipGroup statuses = home.findViewById(R.id.owner_status_group);
                filter[0] = statuses.getChildAt(1).getId();
                statuses.check(filter[0]);
                ((BottomNavigationView) home.findViewById(R.id.owner_home_nav)).setSelectedItemId(R.id.nav_today);
            });
            ins.waitForIdleSync();
            ins.runOnMainSync(() -> {
                View shortcut = home.findViewById(R.id.today_manage_queues);
                Rect visible = new Rect();
                assertTrue(shortcut.isEnabled() && shortcut.isClickable());
                assertTrue(shortcut.getGlobalVisibleRect(visible));
                assertEquals(shortcut.getHeight(), visible.height());
                assertEquals(shortcut.getWidth(), visible.width());
                android.widget.TextView label = (android.widget.TextView) shortcut;
                assertNotNull(label.getLayout());
                for (int line = 0; line < label.getLineCount(); line++) {
                    assertEquals("Keep the complete button label", 0, label.getLayout().getEllipsisCount(line));
                }
                assertTrue(shortcut.getHeight() >= home.getResources().getDimensionPixelSize(R.dimen.touch_target_min));
                int[] served = new int[2];
                int[] waiting = new int[2];
                int[] missed = new int[2];
                home.findViewById(R.id.today_served).getLocationOnScreen(served);
                home.findViewById(R.id.today_waiting).getLocationOnScreen(waiting);
                home.findViewById(R.id.today_no_show_rate).getLocationOnScreen(missed);
                assertEquals("Totals share one reading edge", served[0], waiting[0]);
                assertEquals("Totals share one reading edge", served[0], missed[0]);
                int[] title = new int[2];
                home.findViewById(R.id.today_title).getLocationOnScreen(title);
                assertEquals("Totals align with the screen heading", title[0], served[0]);
            });
            capture(ins, "today");
            ins.runOnMainSync(() -> assertTrue(home.findViewById(R.id.today_manage_queues).performClick()));
            ins.waitForIdleSync();
            ins.runOnMainSync(() -> {
                assertEquals(R.id.nav_queues, ((BottomNavigationView) home.findViewById(R.id.owner_home_nav)).getSelectedItemId());
                Fragment queues = home.getSupportFragmentManager().findFragmentByTag("queues");
                assertSame("Reuse the existing Queues tab", original[0], queues);
                assertFalse(queues.isHidden());
                assertTrue(home.getSupportFragmentManager().findFragmentByTag("today").isHidden());
                assertEquals("Keep the selected queue status", filter[0], ((ChipGroup) home.findViewById(R.id.owner_status_group)).getCheckedChipId());
            });
            capture(ins, "queues");
        } finally {
            ins.runOnMainSync(home::finish);
            ins.waitForIdleSync();
        }
    }

    private void capture(Instrumentation ins, String screen) throws Exception {
        try (java.io.InputStream stream = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                ins.getUiAutomation().executeShellCommand("screencap -p /sdcard/Download/quapp-phase3-one-pass-" + screen + ".png"))) {
            byte[] buffer = new byte[4096];
            while (stream.read(buffer) != -1) { }
        }
    }
}

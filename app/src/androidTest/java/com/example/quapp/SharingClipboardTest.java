package com.example.quapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 28)
public class SharingClipboardTest {
    @Test public void copiesActualLocalLinkAndBackClosesScreen() {
        Queue queue = FakeData.ownedQueues().get(0);
        ClipboardManager clipboard = (ClipboardManager) InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData[] original = {null};
        boolean[] saved = {false};
        try (ActivityScenario<ShareQueueActivity> scenario = ActivityScenario.launch(
                ShareQueueActivity.intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), queue.getId()))) {
            try {
                boolean[] focused = {false};
                long deadline = android.os.SystemClock.uptimeMillis() + 5000;
                do {
                    scenario.onActivity(screen -> focused[0] = screen.hasWindowFocus());
                    if (focused[0]) break;
                    android.os.SystemClock.sleep(16);
                } while (android.os.SystemClock.uptimeMillis() < deadline);
                assertTrue("Share window has clipboard access", focused[0]);
                scenario.onActivity(screen -> {
                    original[0] = clipboard.getPrimaryClip();
                    saved[0] = true;
                    assertTrue(screen.findViewById(R.id.share_copy).performClick());
                    ClipData copied = clipboard.getPrimaryClip();
                    assertNotNull(copied);
                    assertEquals(1, copied.getItemCount());
                    assertTrue("Copied text matches the existing local link; resolution is not claimed",
                            QueueLink.url(queue).contentEquals(copied.getItemAt(0).getText()));
                });
            } finally {
                if (saved[0]) scenario.onActivity(screen -> {
                    if (original[0] == null) clipboard.clearPrimaryClip();
                    else clipboard.setPrimaryClip(original[0]);
                });
            }
            scenario.onActivity(screen -> {
                assertTrue(screen.findViewById(R.id.share_back).performClick());
                assertTrue(screen.isFinishing());
            });
        }
    }
}

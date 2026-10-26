package com.example.quapp;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OnboardingLandscapeTest {
    @Test public void welcomeHasReachableLandscapeActions() throws Exception {
        android.app.Instrumentation ins = InstrumentationRegistry.getInstrumentation();
        try (ActivityScenario<OnboardingActivity> scenario = ActivityScenario.launch(OnboardingActivity.class)) {
            scenario.onActivity(a -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            boolean[] landscape = {false};
            long deadline = SystemClock.uptimeMillis() + 5000;
            do {
                ins.waitForIdleSync();
                scenario.onActivity(a -> landscape[0] = a.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
                if (!landscape[0]) SystemClock.sleep(50);
            } while (!landscape[0] && SystemClock.uptimeMillis() < deadline);
            assertTrue("Actual Activity must be landscape", landscape[0]);
            for (int page = 0; page < 1; page++) {
                scenario.onActivity(a -> ((NestedScrollView) a.findViewById(R.id.onboarding_root)).fullScroll(View.FOCUS_DOWN));
                ins.waitForIdleSync();
                scenario.onActivity(a -> {
                    View root = a.findViewById(R.id.onboarding_root), next = a.findViewById(R.id.onboarding_next);
                    assertTrue("Actual app viewport must be wider than tall", root.getWidth() > root.getHeight());
                    Rect visible = new Rect(); assertTrue(next.getGlobalVisibleRect(visible));
                    assertEquals(next.getHeight(), visible.height());
                });
                SystemClock.sleep(250);
                Bitmap image = ins.getUiAutomation().takeScreenshot(); assertNotNull(image);
                File folder = new File(ins.getTargetContext().getExternalFilesDir(null), "onboarding-landscape");
                assertTrue(folder.isDirectory() || folder.mkdirs());
                try (FileOutputStream output = new FileOutputStream(new File(folder, "page-" + page + ".png"))) {
                    assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output));
                } finally { image.recycle(); }
            }
        }
    }
}

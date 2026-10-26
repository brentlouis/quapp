package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

/** No login or queue mutations. Restore the install's original completion preference. */
@RunWith(AndroidJUnit4.class)
public class OnboardingWorkflowTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();

    @Test public void singleWelcomeCompletionAndGuest() throws Exception {
        SharedPreferences prefs = ins.getTargetContext().getSharedPreferences("quapp_onboarding", 0);
        boolean present = prefs.contains("completed"), original = prefs.getBoolean("completed", false);
        Activity activity = null;
        try {
            prefs.edit().remove("completed").commit();
            assertFalse(OnboardingActivity.completed(ins.getTargetContext()));
            activity = launch();
            final Activity screen = activity;
            assertEquals(screen.getString(R.string.welcome_title), ((TextView) screen.findViewById(R.id.onboarding_title)).getText());
            capture("welcome");
            revealNext(screen);
            capture("welcome-actions");
            ins.runOnMainSync(() -> screen.findViewById(R.id.onboarding_skip).performClick());
            ins.waitForIdleSync();
            assertTrue(OnboardingActivity.completed(ins.getTargetContext()));
            assertTrue(screen.isFinishing());
        } finally {
            if (activity != null) { final Activity close = activity; ins.runOnMainSync(close::finish); }
            SharedPreferences.Editor restore = prefs.edit();
            if (present) restore.putBoolean("completed", original); else restore.remove("completed");
            assertTrue(restore.commit());
            assertEquals(present, prefs.contains("completed"));
            assertEquals(original, prefs.getBoolean("completed", false));
        }
    }

    @Test public void skipPersistsCompletion() {
        SharedPreferences prefs = ins.getTargetContext().getSharedPreferences("quapp_onboarding", 0);
        boolean present = prefs.contains("completed"), original = prefs.getBoolean("completed", false);
        try (androidx.test.core.app.ActivityScenario<OnboardingActivity> scenario =
                     androidx.test.core.app.ActivityScenario.launch(OnboardingActivity.class)) {
            scenario.onActivity(a -> {
                assertEquals(android.view.View.VISIBLE, a.findViewById(R.id.onboarding_skip).getVisibility());
                a.findViewById(R.id.onboarding_skip).performClick();
                assertTrue(OnboardingActivity.completed(a));
            });
        } finally {
            SharedPreferences.Editor restore = prefs.edit();
            if (present) restore.putBoolean("completed", original); else restore.remove("completed");
            assertTrue(restore.commit());
        }
    }

    @Test public void restoresWelcomeAfterRecreation() {
        try (androidx.test.core.app.ActivityScenario<OnboardingActivity> scenario =
                     androidx.test.core.app.ActivityScenario.launch(OnboardingActivity.class)) {
            scenario.recreate();
            scenario.onActivity(a -> assertEquals(a.getString(R.string.welcome_title),
                    ((TextView) a.findViewById(R.id.onboarding_title)).getText()));
        }
    }

    @Test public void loginExplainsUnavailableGoogleAuthentication() throws Exception {
        try (androidx.test.core.app.ActivityScenario<LoginActivity> scenario =
                     androidx.test.core.app.ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(a -> {
                TextView google = a.findViewById(R.id.login_google_button);
                assertFalse(google.isEnabled());
                assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(google.getCurrentTextColor(),
                        androidx.core.content.ContextCompat.getColor(a, R.color.paper)) >= 4.5);
                ((androidx.core.widget.NestedScrollView) a.findViewById(R.id.login_scroll)).fullScroll(android.view.View.FOCUS_DOWN);
            });
            ins.waitForIdleSync();
            scenario.onActivity(a -> {
                android.view.View google = a.findViewById(R.id.login_google_button);
                android.graphics.Rect visible = new android.graphics.Rect();
                assertTrue(google.getGlobalVisibleRect(visible));
                assertEquals(google.getHeight(), visible.height());
            });
            capture("login-authentication-boundary");
        }
    }

    @Test public void reducedMotionDoesNotMoveArtwork() {
        SharedPreferences prefs = ProfilePreferences.get(ins.getTargetContext());
        boolean present = prefs.contains("reduced"), original = prefs.getBoolean("reduced", false);
        prefs.edit().putBoolean("reduced", true).commit();
        try (androidx.test.core.app.ActivityScenario<OnboardingActivity> scenario =
                     androidx.test.core.app.ActivityScenario.launch(OnboardingActivity.class)) {
            scenario.onActivity(a -> {
                assertEquals(0f, a.findViewById(R.id.onboarding_art).getTranslationY(), 0f);
                assertEquals(1f, a.findViewById(R.id.onboarding_art).getAlpha(), 0f);
            });
        } finally {
            SharedPreferences.Editor restore = prefs.edit();
            if (present) restore.putBoolean("reduced", original); else restore.remove("reduced");
            assertTrue(restore.commit());
        }
    }

    private void revealNext(Activity screen) {
        ins.runOnMainSync(() -> ((androidx.core.widget.NestedScrollView) screen.findViewById(R.id.onboarding_root))
                .fullScroll(android.view.View.FOCUS_DOWN));
        ins.waitForIdleSync();
        android.graphics.Rect bounds = new android.graphics.Rect();
        android.view.View next = screen.findViewById(R.id.onboarding_next);
        assertTrue("Next remains reachable at large text", next.getGlobalVisibleRect(bounds));
        assertEquals(next.getHeight(), bounds.height());
        assertTrue(next.getHeight() >= 48 * screen.getResources().getDisplayMetrics().density);
    }

    private Activity launch() {
        Activity activity = ins.startActivitySync(new Intent(ins.getTargetContext(), OnboardingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.waitForIdleSync();
        return activity;
    }
    private void capture(String name) throws Exception {
        ins.waitForIdleSync();
        android.os.SystemClock.sleep(250);
        Bitmap image = ins.getUiAutomation().takeScreenshot();
        assertNotNull(image);
        File folder = new File(ins.getTargetContext().getExternalFilesDir(null), "onboarding-review");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(folder, name + ".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        image.recycle();
    }
}

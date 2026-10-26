package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Parcel;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

/** Fixtures enter native fields, never production controls. Every preference is restored. */
@RunWith(AndroidJUnit4.class)
public class AuthProposalTest {
    final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    final Map<String, Map<String, ?>> backup = new HashMap<>();
    boolean debug;
    @Before public void before() {
        debug = FakeData.isDebugMode(); FakeData.setDebugMode(false);
        for (String name : new String[]{"quapp_session", "quapp_device", "quapp_onboarding", "quapp_profile_preferences"})
            backup.put(name, new HashMap<>(ins.getTargetContext().getSharedPreferences(name, 0).getAll()));
    }
    @After public void after() {
        ins.runOnMainSync(() -> {
            for (Stage stage : new Stage[]{Stage.RESUMED, Stage.PAUSED, Stage.STOPPED})
                for (Activity a : new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) a.finish();
        });
        FakeData.setDebugFailOnce(false); FakeData.setDebugMode(debug);
        for (String name : backup.keySet()) {
            SharedPreferences prefs = ins.getTargetContext().getSharedPreferences(name, 0);
            SharedPreferences.Editor edit = prefs.edit();
            for (String key : prefs.getAll().keySet()) if (!backup.get(name).containsKey(key)) edit.remove(key);
            for (Map.Entry<String, ?> e : backup.get(name).entrySet()) {
                Object v = e.getValue();
                if (v instanceof String) edit.putString(e.getKey(), (String) v);
                else if (v instanceof Boolean) edit.putBoolean(e.getKey(), (Boolean) v);
                else if (v instanceof Integer) edit.putInt(e.getKey(), (Integer) v);
                else if (v instanceof Long) edit.putLong(e.getKey(), (Long) v);
                else if (v instanceof Float) edit.putFloat(e.getKey(), (Float) v);
                else { @SuppressWarnings("unchecked") Set<String> set = (Set<String>) v; edit.putStringSet(e.getKey(), new HashSet<>(set)); }
            }
            assertTrue(edit.commit()); assertEquals(backup.get(name), prefs.getAll());
        }
    }

    @Test public void registrationUnlockedAndErrorsRecover() throws Exception {
        SharedPreferences device = ins.getTargetContext().getSharedPreferences("quapp_device", 0);
        Set<String> accounts = new HashSet<>(device.getStringSet("accounts", Collections.emptySet()));
        accounts.add("09000000001"); accounts.add("09000000002"); device.edit().putStringSet("accounts", accounts).commit();
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(a -> {
                for (int id : new int[]{R.id.register_name_input, R.id.register_phone_input, R.id.register_password_input, R.id.register_confirm_input})
                    assertTrue(a.findViewById(id).isEnabled());
                assertTrue(input(a, R.id.register_password_input).getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
                assertTrue(input(a, R.id.register_confirm_input).getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
                input(a, R.id.register_confirm_input).onEditorAction(EditorInfo.IME_ACTION_DONE);
                assertNotNull(layout(a, R.id.register_name_layout).getError());
                assertTrue(input(a, R.id.register_name_input).hasFocus());
                input(a, R.id.register_name_input).setText("李");
                assertNull(layout(a, R.id.register_name_layout).getError());
                a.findViewById(R.id.register_submit_button).performClick();
                assertNull(layout(a, R.id.register_name_layout).getError());
                assertTrue(input(a, R.id.register_phone_input).hasFocus());
                assertEquals(EditorInfo.IME_ACTION_NEXT, input(a, R.id.register_password_input).getImeOptions() & EditorInfo.IME_MASK_ACTION);
                assertEquals(R.id.register_phone_input, input(a, R.id.register_name_input).getNextFocusForwardId());
                input(a, R.id.register_password_input).setText("1234567 ");
                input(a, R.id.register_confirm_input).setText("1234567");
                a.findViewById(R.id.register_submit_button).performClick();
                assertNull(layout(a, R.id.register_password_layout).getError());
                assertNotNull(layout(a, R.id.register_confirm_layout).getError());
            });
            capture("register-errors");
        }
        assertEquals(accounts, device.getStringSet("accounts", Collections.emptySet()));
    }

    @Test public void recreationKeepsNonsecretsAndNeverSavesRevealedPassword() throws Exception {
        final String secret = "  demo secret  ";
        final boolean[] saved = {false};
        android.app.Application app = (android.app.Application) ins.getTargetContext().getApplicationContext();
        android.app.Application.ActivityLifecycleCallbacks listener = new android.app.Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity a, android.os.Bundle b) { }
            public void onActivityStarted(Activity a) { }
            public void onActivityResumed(Activity a) { }
            public void onActivityPaused(Activity a) { }
            public void onActivityStopped(Activity a) { }
            public void onActivityDestroyed(Activity a) { }
            public void onActivitySaveInstanceState(Activity a, android.os.Bundle b) {
                if (!(a instanceof RegisterActivity)) return;
                Parcel parcel = Parcel.obtain();
                try { parcel.writeBundle(b); String raw = new String(parcel.marshall(), java.nio.charset.StandardCharsets.UTF_16LE);
                    assertFalse("Password absent from saved-state parcel", raw.contains(secret)); saved[0] = true;
                } finally { parcel.recycle(); }
            }
        };
        app.registerActivityLifecycleCallbacks(listener);
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(a -> {
                input(a, R.id.register_name_input).setText("Aleksandra Wiśniewska-Kowalczyk 👩🏽‍💻");
                input(a, R.id.register_phone_input).setText("09123456789");
                input(a, R.id.register_password_input).setText(secret);
                input(a, R.id.register_confirm_input).setText(secret);
                input(a, R.id.register_password_input).setTransformationMethod(null);
                assertEquals(secret, AuthForms.password(input(a, R.id.register_password_input)));
                assertFalse(input(a, R.id.register_password_input).isSaveEnabled());
            });
            capture("register-worst-name");
            scenario.recreate();
            scenario.onActivity(a -> {
                assertEquals("Aleksandra Wiśniewska-Kowalczyk 👩🏽‍💻", input(a, R.id.register_name_input).getText().toString());
                assertEquals("09123456789", input(a, R.id.register_phone_input).getText().toString());
                assertEquals("", AuthForms.password(input(a, R.id.register_password_input)));
                assertEquals("", AuthForms.password(input(a, R.id.register_confirm_input)));
                assertTrue(input(a, R.id.register_password_input).getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
            });
            assertTrue(saved[0]); capture("register-recreated");
        } finally { app.unregisterActivityLifecycleCallbacks(listener); }
    }

    @Test public void loginFailureRestoresSubmitAndDuplicateDoneIsGuarded() throws Exception {
        FakeData.setDebugFailOnce(true);
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(a -> {
                input(a, R.id.login_phone_input).setText("09123456789");
                input(a, R.id.login_password_input).setText(" sample ");
                input(a, R.id.login_password_input).onEditorAction(EditorInfo.IME_ACTION_DONE);
                assertFalse(a.findViewById(R.id.login_submit_button).isEnabled());
                input(a, R.id.login_password_input).onEditorAction(EditorInfo.IME_ACTION_DONE);
            });
            ins.waitForIdleSync();
            scenario.onActivity(a -> assertTrue(a.findViewById(R.id.login_submit_button).isEnabled()));
            capture("login-failure");
        }
    }

    @Test public void cancelledRegistrationDoesNotCreateSession() {
        Map<String, ?> original = new HashMap<>(ins.getTargetContext().getSharedPreferences("quapp_session", 0).getAll());
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(a -> {
                input(a, R.id.register_name_input).setText("Jo"); input(a, R.id.register_phone_input).setText("09123456789");
                input(a, R.id.register_password_input).setText("sample123"); input(a, R.id.register_confirm_input).setText("sample123");
                a.findViewById(R.id.register_submit_button).performClick(); a.finish();
            });
            ins.waitForIdleSync();
        }
        assertEquals(original, ins.getTargetContext().getSharedPreferences("quapp_session", 0).getAll());
    }

    @Test public void welcomeAuthBackAndGuestWithoutSession() throws Exception {
        ins.getTargetContext().getSharedPreferences("quapp_session", 0).edit().putBoolean("logged_in", false).commit();
        try (ActivityScenario<OnboardingActivity> scenario = ActivityScenario.launch(OnboardingActivity.class)) {
            capture("welcome");
            scenario.onActivity(a -> a.findViewById(R.id.onboarding_next).performClick());
            Activity register = resumed(RegisterActivity.class);
            ins.runOnMainSync(() -> register.findViewById(R.id.register_back).performClick());
            resumed(OnboardingActivity.class);
            scenario.onActivity(a -> a.findViewById(R.id.onboarding_sign_in).performClick());
            Activity login = resumed(LoginActivity.class);
            ins.runOnMainSync(() -> login.findViewById(R.id.login_back).performClick());
            resumed(OnboardingActivity.class);
            scenario.onActivity(a -> a.findViewById(R.id.onboarding_skip).performClick());
            resumed(QueuerHomeActivity.class);
            assertFalse(new Session(ins.getTargetContext()).isLoggedIn());
            assertTrue(OnboardingActivity.completed(ins.getTargetContext())); capture("guest-browse");
        }
    }

    @Test public void welcomeAndAuthActionsReachable() throws Exception {
        try (ActivityScenario<OnboardingActivity> scenario = ActivityScenario.launch(OnboardingActivity.class)) {
            scenario.onActivity(a -> ((NestedScrollView) a.findViewById(R.id.onboarding_root)).fullScroll(View.FOCUS_DOWN));
            ins.waitForIdleSync(); scenario.onActivity(a -> fullyVisible(a.findViewById(R.id.onboarding_skip))); capture("welcome-actions");
        }
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(a -> ((NestedScrollView) a.findViewById(R.id.login_scroll)).fullScroll(View.FOCUS_DOWN));
            ins.waitForIdleSync(); scenario.onActivity(a -> {
                fullyVisible(a.findViewById(R.id.login_google_button));
                assertFalse(a.findViewById(R.id.login_google_button).isEnabled());
            }); capture("login-actions");
        }
    }

    @Test public void preferencesAndFamilyLanding() throws Exception {
        try (ActivityScenario<ProfileSettingsActivity> scenario = ActivityScenario.launch(ProfileSettingsActivity.intent(ins.getTargetContext(), "preferences"))) {
            scenario.onActivity(a -> {
                String all = texts(a.findViewById(R.id.profile_settings_content));
                assertTrue(all.contains("Reduce motion")); assertTrue(all.contains("Service order"));
                assertFalse(all.contains("Text size")); assertFalse(all.contains("Default area")); assertFalse(all.contains("Notifications"));
            }); capture("preferences");
            scenario.onActivity(a -> ((NestedScrollView) a.findViewById(R.id.profile_settings_content).getParent()).fullScroll(View.FOCUS_DOWN));
            ins.waitForIdleSync();
            scenario.onActivity(a -> fullyVisible(textView(a.findViewById(R.id.profile_settings_content), ProfilePreferences.get(a).getString("order", "Recommended").replace("Shortest wait", "Shortest demo wait"))));
            capture("preferences-actions");
        }
        try (ActivityScenario<ProfileSettingsActivity> scenario = ActivityScenario.launch(ProfileSettingsActivity.intent(ins.getTargetContext(), "notifications"))) {
            scenario.onActivity(a -> { String all = texts(a.findViewById(R.id.profile_settings_content)); assertTrue(all.contains("Queue calls")); assertFalse(all.contains("Appointment reminders")); });
            capture("notifications");
        }
        try (ActivityScenario<Phase4Activity> scenario = ActivityScenario.launch(Phase4Activity.intent(ins.getTargetContext(), Phase4Activity.FAMILY))) {
            scenario.onActivity(a -> assertTrue(texts(a.findViewById(android.R.id.content)).contains("Planning only. No group ticket or membership will be created.")));
            capture("family-landing");
            scenario.onActivity(a -> ((NestedScrollView) a.findViewById(R.id.p4_scroll)).fullScroll(View.FOCUS_DOWN));
            ins.waitForIdleSync();
            scenario.onActivity(a -> fullyVisible(textView(a.findViewById(R.id.p4_content), a.getString(R.string.p4_family_status))));
            capture("family-landing-actions");
            scenario.onActivity(a -> {
                textView(a.findViewById(R.id.p4_content), a.getString(R.string.p4_family_intro)).performClick();
                List<EditText> fields = new ArrayList<>(); inputs(a.findViewById(R.id.p4_content), fields);
                fields.get(0).setText("李 · Aleksandra Wiśniewska-Kowalczyk");
                fields.get(1).setText("Jo\n家族 🌿\nNguyễn Thị Minh Khai");
                textView(a.findViewById(R.id.p4_content), a.getString(R.string.p4_review)).performClick();
            }); capture("family-review");
            scenario.recreate();
            scenario.onActivity(a -> {
                assertTrue(texts(a.findViewById(R.id.p4_content)).contains("Nguyễn Thị Minh Khai"));
                assertFalse(textView(a.findViewById(R.id.p4_content), a.getString(R.string.p4_create_group)).isEnabled());
            });
        }
    }

    @Test public void completionMigrationAndTicketEntry() throws Exception {
        SharedPreferences session = ins.getTargetContext().getSharedPreferences("quapp_session", 0);
        SharedPreferences intro = ins.getTargetContext().getSharedPreferences("quapp_onboarding", 0);
        session.edit().putBoolean("logged_in", false).commit(); intro.edit().putBoolean("completed", true).commit();
        try (ActivityScenario<SplashActivity> ignored = ActivityScenario.launch(SplashActivity.class)) { resumed(LoginActivity.class); }
        session.edit().putBoolean("logged_in", true).putString("role", "QUEUER").commit(); intro.edit().putBoolean("completed", false).commit();
        try (ActivityScenario<SplashActivity> ignored = ActivityScenario.launch(SplashActivity.class)) { resumed(QueuerHomeActivity.class); }
        try (ActivityScenario<QueuerHomeActivity> scenario = ActivityScenario.launch(QueuerHomeActivity.intent(ins.getTargetContext(), QueuerHomeActivity.TAB_TICKETS))) {
            ins.waitForIdleSync();
            scenario.onActivity(a -> {
                assertTrue(texts(a.findViewById(android.R.id.content)).contains(a.getString(R.string.family_draft_hint)));
                View family = a.findViewById(R.id.my_tickets_family);
                family.requestRectangleOnScreen(new Rect(0, 0, family.getWidth(), family.getHeight()), true);
            }); ins.waitForIdleSync();
            scenario.onActivity(a -> fullyVisible(a.findViewById(R.id.my_tickets_family)));
            capture("tickets-family-entry");
            scenario.onActivity(a -> a.findViewById(R.id.my_tickets_family).performClick()); resumed(Phase4Activity.class);
        }
    }

    @Test public void realKeyboardNextDoneAndPasswordVisibility() throws Exception {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.register_name_input))
                    .perform(androidx.test.espresso.action.ViewActions.scrollTo(), androidx.test.espresso.action.ViewActions.click(),
                            androidx.test.espresso.action.ViewActions.typeText("Jo"), androidx.test.espresso.action.ViewActions.pressImeActionButton());
            scenario.onActivity(a -> assertTrue(input(a, R.id.register_phone_input).hasFocus()));
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.register_phone_input))
                    .perform(androidx.test.espresso.action.ViewActions.typeText("09123456789"), androidx.test.espresso.action.ViewActions.pressImeActionButton());
            scenario.onActivity(a -> assertTrue(input(a, R.id.register_password_input).hasFocus()));
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.register_password_input))
                    .perform(androidx.test.espresso.action.ViewActions.typeText("short"), androidx.test.espresso.action.ViewActions.pressImeActionButton());
            scenario.onActivity(a -> assertTrue(input(a, R.id.register_confirm_input).hasFocus()));
            capture("register-keyboard-confirm");
            scenario.onActivity(a -> fullyVisible(input(a, R.id.register_confirm_input)));
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.register_confirm_input))
                    .perform(androidx.test.espresso.action.ViewActions.typeText("short"), androidx.test.espresso.action.ViewActions.pressImeActionButton());
            ins.waitForIdleSync();
            scenario.onActivity(a -> {
                assertNotNull(layout(a, R.id.register_password_layout).getError());
                assertTrue(input(a, R.id.register_password_input).hasFocus());
                assertTrue(input(a, R.id.register_password_input).getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
                layout(a, R.id.register_password_layout).findViewById(com.google.android.material.R.id.text_input_end_icon).performClick();
                assertNull(input(a, R.id.register_password_input).getTransformationMethod());
                layout(a, R.id.register_password_layout).findViewById(com.google.android.material.R.id.text_input_end_icon).performClick();
                assertTrue(input(a, R.id.register_password_input).getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
            });
            capture("register-keyboard-error");
            androidx.test.espresso.Espresso.closeSoftKeyboard();
        }
    }

    static EditText input(Activity a, int id) { return a.findViewById(id); }
    static TextInputLayout layout(Activity a, int id) { return a.findViewById(id); }
    static void fullyVisible(View v) { Rect r = new Rect(); assertTrue(v.getGlobalVisibleRect(r)); assertEquals(v.getHeight(), r.height()); assertTrue(v.getHeight() >= 48 * v.getResources().getDisplayMetrics().density); }
    static String texts(View v) { String s = v instanceof TextView ? ((TextView) v).getText().toString() : ""; if (v instanceof ViewGroup) for (int i=0;i<((ViewGroup)v).getChildCount();i++) s += "\n" + texts(((ViewGroup)v).getChildAt(i)); return s; }
    static TextView textView(View v, String label) {
        if (v instanceof TextView && label.contentEquals(((TextView) v).getText())) return (TextView) v;
        if (v instanceof ViewGroup) for (int i=0;i<((ViewGroup)v).getChildCount();i++) { TextView found = textView(((ViewGroup)v).getChildAt(i), label); if (found != null) return found; }
        return null;
    }
    static void inputs(View v, List<EditText> result) { if (v instanceof EditText) result.add((EditText) v); if (v instanceof ViewGroup) for (int i=0;i<((ViewGroup)v).getChildCount();i++) inputs(((ViewGroup)v).getChildAt(i), result); }
    Activity resumed(Class<?> type) {
        final Activity[] result = {null};
        for (int i = 0; i < 50 && result[0] == null; i++) {
            ins.waitForIdleSync(); ins.runOnMainSync(() -> { for (Activity a : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) if (type.isInstance(a)) result[0] = a; });
            if (result[0] == null) android.os.SystemClock.sleep(50);
        }
        assertNotNull(type.getSimpleName(), result[0]); return result[0];
    }
    void capture(String name) throws Exception {
        ins.waitForIdleSync(); android.os.SystemClock.sleep(150);
        Bitmap bitmap = ins.getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        String config = ins.getTargetContext().getResources().getConfiguration().fontScale > 1.5f ? "large" : "normal";
        File dir = new File(ins.getTargetContext().getExternalFilesDir(null), "auth-proposal-" + config); assertTrue(dir.isDirectory() || dir.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); } finally { bitmap.recycle(); }
    }
}

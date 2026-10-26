package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Rect;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.time.LocalTime;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

/** Invalid Preview must reveal its first editable problem; no queue is submitted. */
@RunWith(AndroidJUnit4.class)
public class CreateValidationNavigationTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private Activity form;

    @Test public void revealsFirstInvalidFieldAndUsesExistingPickers() throws Exception {
        try {
            form = ins.startActivitySync(new Intent(ins.getTargetContext(), CreateQueueActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            ins.runOnMainSync(() -> form.findViewById(R.id.create_bring_input).requestFocus());
            assertEquals(View.GONE, form.findViewById(R.id.create_name_layout).getVisibility());



            preview(); first(R.id.create_municipality, R.id.create_municipality);
            ins.runOnMainSync(() -> form.findViewById(R.id.create_municipality).performClick());
            awaitInteractiveDialog();
            String town = form.getResources().getStringArray(R.array.bohol_municipalities)[0];
            onView(withText(town)).inRoot(isDialog()).perform(click());
            awaitFormFocus();
            preview(); first(R.id.create_category, R.id.create_category);
            ins.runOnMainSync(() -> form.findViewById(R.id.create_category).performClick());
            awaitInteractiveDialog();
            onView(withId(R.id.category_list)).inRoot(isDialog()).perform(new androidx.test.espresso.ViewAction() {
                public org.hamcrest.Matcher<View> getConstraints() { return isAssignableFrom(View.class); }
                public String getDescription() { return "Wait for category sheet to settle"; }
                public void perform(androidx.test.espresso.UiController controller, View view) {
                    View sheet = view.getRootView().findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
                    long deadline = android.os.SystemClock.uptimeMillis() + 5000;
                    do {
                        controller.loopMainThreadUntilIdle();
                        int state = behavior.getState();
                        if (state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                                || state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
                                || state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED) return;
                        controller.loopMainThreadForAtLeast(16);
                    } while (android.os.SystemClock.uptimeMillis() < deadline);
                    fail("Category sheet did not settle; state=" + behavior.getState());
                }
            });
            shot("category-before-tap");
            onView(withText(R.string.category_relief)).inRoot(isDialog()).perform(click());
            awaitFormFocus();
            shot("category-after-tap");
            long pickedBy = android.os.SystemClock.uptimeMillis() + 3000;
            final boolean[] picked = {false};
            do { ins.waitForIdleSync(); ins.runOnMainSync(() -> picked[0] = form.getString(R.string.category_relief).contentEquals(((TextView) form.findViewById(R.id.create_category)).getText())); if (picked[0]) break; android.os.SystemClock.sleep(50); } while (android.os.SystemClock.uptimeMillis() < pickedBy);
            ins.runOnMainSync(() -> assertEquals("Category picker selection is bound", form.getString(R.string.category_relief), ((TextView) form.findViewById(R.id.create_category)).getText().toString()));
            assertEquals("Resolved picker errors disappear", View.GONE, form.findViewById(R.id.create_pick_error).getVisibility());


            if (form.getResources().getConfiguration().fontScale <= 1f) {
                ins.runOnMainSync(() -> form.findViewById(R.id.create_start_date).performClick());
                awaitInteractiveDialog();
                onView(withId(com.google.android.material.R.id.confirm_button)).inRoot(isDialog()).perform(click());
                awaitFormFocus();
                ins.runOnMainSync(() -> form.findViewById(R.id.create_opens).performClick());
                awaitInteractiveDialog();
                onView(withId(com.google.android.material.R.id.material_timepicker_mode_button)).inRoot(isDialog()).perform(click());
                onView(allOf(isDescendantOfA(withId(com.google.android.material.R.id.material_hour_text_input)), isAssignableFrom(android.widget.EditText.class))).inRoot(isDialog()).perform(replaceText("9"));
                onView(withId(com.google.android.material.R.id.material_minute_text_input)).inRoot(isDialog()).perform(new androidx.test.espresso.ViewAction() {
                    public org.hamcrest.Matcher<View> getConstraints() { return isAssignableFrom(View.class); }
                    public String getDescription() { return "Select the native minute editor without toggling an already selected editor off"; }
                    public void perform(androidx.test.espresso.UiController controller, View view) {
                        assertTrue(view instanceof android.widget.Checkable);
                        ((android.widget.Checkable) view).setChecked(true);
                        controller.loopMainThreadUntilIdle();
                    }
                });
                onView(allOf(isDescendantOfA(withId(com.google.android.material.R.id.material_minute_text_input)), isAssignableFrom(android.widget.EditText.class))).inRoot(isDialog()).perform(replaceText("15"), closeSoftKeyboard());
                onView(withId(com.google.android.material.R.id.material_timepicker_ok_button)).inRoot(isDialog()).perform(click());
                awaitFormFocus();
                ins.runOnMainSync(() -> assertEquals(Format.time(form, LocalTime.of(9, 15)), ((TextView)form.findViewById(R.id.create_opens)).getText().toString()));
            }
            ins.runOnMainSync(() -> form.findViewById(R.id.create_multiple_days).performClick());
            set("endDate", Format.today().minusDays(1));
            preview(); first(R.id.create_end_date, R.id.create_end_date);
            set("endDate", Format.today()); set("closesAt", LocalTime.of(8, 0));
            preview(); first(R.id.create_closes, R.id.create_closes);
            set("closesAt", LocalTime.of(17, 0));
            preview();
            final Activity[] openedPreview = {null};
            long previewDeadline = android.os.SystemClock.uptimeMillis() + 5000;
            do {
                ins.waitForIdleSync();
                ins.runOnMainSync(() -> {
                    for (Activity a : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                        if (a instanceof OrganizerPreviewActivity) openedPreview[0] = a;
                });
                if (openedPreview[0] != null) break;
                android.os.SystemClock.sleep(16);
            } while (android.os.SystemClock.uptimeMillis() < previewDeadline);
            assertNotNull("Valid form opens its read-only preview", openedPreview[0]);
            ins.runOnMainSync(() -> {
                Queue snapshot = PreviewSnapshots.get(openedPreview[0].getIntent().getStringExtra(OrganizerPreviewActivity.EXTRA_SNAPSHOT_ID));
                assertEquals(town, snapshot.getMunicipality());
                assertEquals(Category.RELIEF, snapshot.getCategory());
                openedPreview[0].finish();
            });
        } finally {
            if (form != null) ins.runOnMainSync(() -> form.finish());
            ins.waitForIdleSync();
        }
    }
    private void preview() {
        ins.runOnMainSync(() -> { View button = form.findViewById(R.id.create_preview_button); button.requestRectangleOnScreen(new Rect(0, 0, button.getWidth(), button.getHeight()), true); });
        ins.waitForIdleSync();
        ins.runOnMainSync(() -> form.findViewById(R.id.create_preview_button).performClick());
        ins.waitForIdleSync();
    }
    private void awaitFormFocus() {
        long deadline = android.os.SystemClock.uptimeMillis() + 5000;
        final boolean[] focused = {false};
        do {
            ins.waitForIdleSync();
            ins.runOnMainSync(() -> focused[0] = form.hasWindowFocus());
            if (focused[0]) return;
            android.os.SystemClock.sleep(16);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        fail("Create form did not regain window focus after picker dismissal");
    }
    private void awaitInteractiveDialog() throws Exception {
        long deadline = android.os.SystemClock.uptimeMillis() + 5000;
        String lastWindow = "No focused dialog";
        do {
            ins.waitForIdleSync();
            final boolean[] formFocused = {true};
            ins.runOnMainSync(() -> formFocused[0] = form.hasWindowFocus());
            String dump;
            try (java.io.InputStream stream = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                    ins.getUiAutomation().executeShellCommand("dumpsys input"));
                 java.util.Scanner scanner = new java.util.Scanner(stream, "UTF-8").useDelimiter("\\A")) {
                dump = scanner.hasNext() ? scanner.next() : "";
            }
            // shortcut: emulator input-dump format; fail closed if a future Android changes it.
            int focusStart = dump.indexOf("  FocusedWindows:");
            if (focusStart < 0) continue;
            int nameStart = dump.indexOf("name='", focusStart);
            if (nameStart < 0) continue;
            int nameEnd = dump.indexOf("'", nameStart + 6);
            if (nameEnd < 0) continue;
            String name = dump.substring(nameStart + 6, nameEnd);
            if (!name.contains("com.example.quapp.CreateQueueActivity")) continue;
            for (String line : dump.split("\n")) {
                if (!line.contains("name=" + name + ", id=")) continue;
                lastWindow = line.trim();
                if (!formFocused[0] && line.contains(", alpha=1,")
                        && !line.contains("NOT_VISIBLE") && !line.contains("NOT_TOUCHABLE")
                        && !line.contains("NO_INPUT_CHANNEL")) {
                    android.util.Log.i("PickerReadiness", lastWindow);
                    return;
                }
            }
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        fail("Dialog input window never became opaque/touchable: " + lastWindow);
    }
    private void first(int field, int region) {
        final boolean[] ready = {false}; long deadline = android.os.SystemClock.uptimeMillis() + 3000;
        do {
            ins.waitForIdleSync();
            ins.runOnMainSync(() -> { View v = form.findViewById(region); Rect r = new Rect(); ready[0] = form.getCurrentFocus() == form.findViewById(field) && v.getGlobalVisibleRect(r) && r.height() == v.getHeight(); });
            if (ready[0]) break; android.os.SystemClock.sleep(50);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        ins.runOnMainSync(() -> {
            View target = form.findViewById(field);
            assertNotNull(form.getCurrentFocus()); assertEquals("First invalid field gets focus: " + form.getResources().getResourceEntryName(field) + " shown=" + target.isShown() + " height=" + target.getHeight() + " category=" + ((TextView)form.findViewById(R.id.create_category)).getText(), field, form.getCurrentFocus().getId());
            View v = form.findViewById(region); Rect visible = new Rect(); assertTrue(v.getGlobalVisibleRect(visible));
            assertEquals("First error region is fully visible", v.getHeight(), visible.height());
        });
    }
    private void text(int id, String value) { ins.runOnMainSync(() -> ((TextView) form.findViewById(id)).setText(value)); }
    private void shot(String name) throws Exception {
        android.graphics.Bitmap bitmap = ins.getUiAutomation().takeScreenshot();
        java.io.File dir = new java.io.File(ins.getTargetContext().getExternalFilesDir(null), "phase3-organizer-closure");
        assertTrue(dir.isDirectory() || dir.mkdirs());
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(dir, name + ".png"))) { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out); }
        finally { bitmap.recycle(); }
    }
    private void set(String name, Object value) throws Exception { Field f = CreateQueueActivity.class.getDeclaredField(name); f.setAccessible(true); ins.runOnMainSync(() -> { try { f.set(form, value); } catch (IllegalAccessException e) { throw new AssertionError(e); } }); }
}

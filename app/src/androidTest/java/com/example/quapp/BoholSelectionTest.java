package com.example.quapp;

import android.app.Instrumentation;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.HashSet;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/** Official location coverage and the existing eight-category contract; no service data added. */
@RunWith(AndroidJUnit4.class)
public class BoholSelectionTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    // PSA PSGC Bohol, verified 2026-10-19; city deliberately excluded from municipalities.
    private static final String MUNICIPALITIES = "Alburquerque|Alicia|Anda|Antequera|Baclayon|Balilihan|Batuan|Bien Unido|Bilar|Buenavista|Calape|Candijay|Carmen|Catigbian|Clarin|Corella|Cortes|Dagohoy|Danao|Dauis|Dimiao|Duero|Garcia Hernandez|Getafe|Guindulman|Inabanga|Jagna|Lila|Loay|Loboc|Loon|Mabini|Maribojoc|Panglao|Pilar|President Carlos P. Garcia|Sagbayan|San Isidro|San Miguel|Sevilla|Sierra Bullones|Sikatuna|Talibon|Trinidad|Tubigon|Ubay|Valencia";

    @Test public void exactOfficialLocationsAndApprovedCategories() {
        String[] towns = ins.getTargetContext().getResources().getStringArray(R.array.bohol_municipalities);
        HashSet<String> expected = new HashSet<>(Arrays.asList(MUNICIPALITIES.split("\\|")));
        assertEquals(47, expected.size());
        expected.add("Tagbilaran City");
        assertEquals(48, towns.length);
        assertEquals(expected, new HashSet<>(Arrays.asList(towns)));
        assertArrayEquals(new String[]{"Tagbilaran City", "Baclayon", "Corella", "Dauis", "Loon",
                "Panglao", "Maribojoc", "Cortes", "Sikatuna", "Alburquerque"}, Arrays.copyOf(towns, 10));
        assertArrayEquals(new Category[]{Category.RELIEF, Category.MEDICAL, Category.GOVERNMENT,
                Category.EDUCATION, Category.BILLS, Category.IDS, Category.JOBS, Category.OTHER}, Category.values());
    }

    @Test public void createSelectionScrollCancelAndRecreation() throws Exception {
        try (ActivityScenario<CreateQueueActivity> form = ActivityScenario.launch(CreateQueueActivity.class)) {
            form.onActivity(a -> assertEquals("", ((TextView)a.findViewById(R.id.create_municipality)).getText().toString()));
            openCreateTown(form);
            onData(is("President Carlos P. Garcia")).inRoot(isDialog())
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            capture("town-dialog");
            onData(is("President Carlos P. Garcia")).inRoot(isDialog()).perform(click());
            awaitFocusedForm(form);
            capture("create-long-town");
            form.recreate();
            form.onActivity(a -> assertEquals("President Carlos P. Garcia", ((TextView)a.findViewById(R.id.create_municipality)).getText().toString()));
            openCreateTown(form);
            onData(is("Valencia")).inRoot(isDialog()).perform(click());
            awaitFocusedForm(form);
            openCreateTown(form);
            onView(isRoot()).inRoot(isDialog()).perform(androidx.test.espresso.action.ViewActions.pressBack());
            awaitFocusedForm(form);
            form.onActivity(a -> assertEquals("Valencia", ((TextView)a.findViewById(R.id.create_municipality)).getText().toString()));
            for (Category category : Category.values()) {
                awaitFocusedForm(form);
                form.onActivity(a -> a.findViewById(R.id.create_category).performClick());
                onView(withId(R.id.category_list)).inRoot(isDialog()).perform(new androidx.test.espresso.ViewAction() {
                    public org.hamcrest.Matcher<View> getConstraints() { return isAssignableFrom(View.class); }
                    public String getDescription() { return "Wait for category sheet animation to settle"; }
                    public void perform(androidx.test.espresso.UiController controller, View view) {
                        View sheet = view.getRootView().findViewById(com.google.android.material.R.id.design_bottom_sheet);
                        com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
                        long deadline = android.os.SystemClock.uptimeMillis() + 5000;
                        do {
                            controller.loopMainThreadUntilIdle();
                            int state = behavior.getState();
                            if (state != com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING
                                    && state != com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING) return;
                            controller.loopMainThreadForAtLeast(16);
                        } while (android.os.SystemClock.uptimeMillis() < deadline);
                        fail("Category sheet did not settle");
                    }
                });
                awaitOpaqueFormDialog();
                onView(isAssignableFrom(com.google.android.material.bottomsheet.BottomSheetDragHandleView.class))
                        .inRoot(isDialog()).perform(new androidx.test.espresso.ViewAction() {
                            public org.hamcrest.Matcher<View> getConstraints() { return isDisplayed(); }
                            public String getDescription() { return "Expand category sheet through its accessible drag handle"; }
                            public void perform(androidx.test.espresso.UiController controller, View view) {
                                View sheet = view.getRootView().findViewById(com.google.android.material.R.id.design_bottom_sheet);
                                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
                                if (behavior.getState() == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED) return;
                                assertTrue(view.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null));
                                long deadline = android.os.SystemClock.uptimeMillis() + 5000;
                                do {
                                    controller.loopMainThreadUntilIdle();
                                    if (behavior.getState() == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED) return;
                                    controller.loopMainThreadForAtLeast(16);
                                } while (android.os.SystemClock.uptimeMillis() < deadline);
                                fail("Accessible category sheet did not expand");
                            }
                        });
                onView(withId(R.id.category_list)).inRoot(isDialog()).check((v, error) -> {
                    if (error != null) throw error;
                    RecyclerView list = (RecyclerView)v;
                    assertEquals(Category.values().length, list.getAdapter().getItemCount());
                });
                onView(withText(category.label)).inRoot(isDialog()).perform(scrollTo());
                awaitOpaqueFormDialog();
                if (category == Category.IDS) capture("category-sheet");
                onView(withId(R.id.category_list)).inRoot(isDialog()).check((v, error) -> {
                    if (error != null) throw error;
                    RecyclerView list = (RecyclerView)v;
                    View row = list.findViewHolderForAdapterPosition(category.ordinal()).itemView;
                    assertTrue(row.getHeight() >= v.getResources().getDimensionPixelSize(R.dimen.touch_target_min));
                    assertTrue(row.isClickable());
                    assertEquals(ins.getTargetContext().getString(category.label), ((TextView)row.findViewById(R.id.category_label)).getText().toString());
                });
                onView(withText(category.label)).inRoot(isDialog()).perform(new androidx.test.espresso.ViewAction() {
                    public org.hamcrest.Matcher<View> getConstraints() { return isDisplayed(); }
                    public String getDescription() { return "Activate the visible category row's accessibility click"; }
                    public void perform(androidx.test.espresso.UiController controller, View view) {
                        View row = (View)view.getParent().getParent();
                        assertTrue(row.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null));
                    }
                });
                long selectedDeadline = android.os.SystemClock.uptimeMillis() + 5000;
                boolean[] selected = {false};
                do {
                    ins.waitForIdleSync();
                    form.onActivity(a -> selected[0] = a.getString(category.label).contentEquals(
                            ((TextView)a.findViewById(R.id.create_category)).getText()));
                } while (!selected[0] && android.os.SystemClock.uptimeMillis() < selectedDeadline);
                form.onActivity(a -> assertEquals(a.getString(category.label), ((TextView)a.findViewById(R.id.create_category)).getText().toString()));
            }
            form.recreate();
            form.onActivity(a -> {
                assertEquals("Valencia", ((TextView)a.findViewById(R.id.create_municipality)).getText().toString());
                assertEquals(a.getString(Category.OTHER.label), ((TextView)a.findViewById(R.id.create_category)).getText().toString());
                ((TextView)a.findViewById(R.id.create_name_input)).setText("Local fixture 王秀英 🏥");
            });
            capture("create-category");
        }
    }

    @Test public void browseAllCategoriesTownAndRecreation() throws Exception {
        Session session = new Session(ins.getTargetContext());
        String previousTown = session.getTown();
        session.setTown("Tagbilaran City");
        try (ActivityScenario<QueuerHomeActivity> home = ActivityScenario.launch(QueuerHomeActivity.class)) {
            for (int i = 0; i <= Category.values().length; i++) {
                final int position = i;
                home.onActivity(a -> {
                    ChipGroup group = a.findViewById(R.id.browse_category_group);
                    assertEquals(Category.values().length + 1, group.getChildCount());
                    Chip chip = (Chip)group.getChildAt(position);
                    assertEquals(position == 0 ? null : Category.values()[position - 1], chip.getTag());
                    chip.performClick();
                    assertTrue(chip.isChecked());
                    chip.requestRectangleOnScreen(new android.graphics.Rect(0, 0, chip.getWidth(), chip.getHeight()), true);
                });
            }
            capture("browse-other");
            home.recreate();
            home.onActivity(a -> {
                ChipGroup group = a.findViewById(R.id.browse_category_group);
                assertTrue(((Chip)group.getChildAt(8)).isChecked());
                ((Chip)group.getChildAt(0)).performClick();
                assertNull(group.getChildAt(0).getTag());
                a.findViewById(R.id.browse_town_chip).performClick();
            });
            onData(is("President Carlos P. Garcia")).inRoot(isDialog()).perform(click());
            assertEquals("President Carlos P. Garcia", session.getTown());
            home.recreate();
            assertEquals("President Carlos P. Garcia", session.getTown());
            home.onActivity(a -> a.findViewById(R.id.browse_town_chip).performClick());
            onView(isRoot()).inRoot(isDialog()).perform(androidx.test.espresso.action.ViewActions.pressBack());
            assertEquals("President Carlos P. Garcia", session.getTown());
            capture("browse-town");
        } finally { session.setTown(previousTown); }
    }

    @Test public void firstVisitMoreOpensCompleteListWithoutChangingTownOnBack() throws Exception {
        Session session = new Session(ins.getTargetContext());
        String previousTown = session.getTown();
        session.setTown(null);
        try (ActivityScenario<QueuerHomeActivity> home = ActivityScenario.launch(QueuerHomeActivity.class)) {
            home.onActivity(a -> {
                ChipGroup towns = a.findViewById(R.id.browse_town_chips);
                assertEquals(7, towns.getChildCount());
                Chip more = (Chip)towns.getChildAt(6);
                assertEquals(a.getString(R.string.browse_more_towns_format, 42), more.getText().toString());
                more.performClick();
            });
            onData(is("Valencia")).inRoot(isDialog())
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            onView(isRoot()).inRoot(isDialog()).perform(androidx.test.espresso.action.ViewActions.pressBack());
            assertNull(session.getTown());
            home.onActivity(a -> ((ChipGroup)a.findViewById(R.id.browse_town_chips)).getChildAt(6).performClick());
            onData(is("Valencia")).inRoot(isDialog()).perform(click());
            assertEquals("Valencia", session.getTown());
        } finally { session.setTown(previousTown); }
    }

    private void capture(String name) throws Exception {
        ins.waitForIdleSync();
        String config = InstrumentationRegistry.getArguments().getString("fixtureConfig", "standard");
        ParcelFileDescriptor output = ins.getUiAutomation().executeShellCommand(
                "screencap -p /sdcard/Download/quapp-bohol-" + config + "-" + name + ".png");
        try (java.io.InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(output)) {
            byte[] buffer = new byte[1024]; while (stream.read(buffer) != -1) { }
        }
    }

    private void awaitOpaqueFormDialog() throws Exception {
        long deadline = android.os.SystemClock.uptimeMillis() + 5000;
        do {
            ins.waitForIdleSync();
            String dump;
            try (java.io.InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(
                    ins.getUiAutomation().executeShellCommand("dumpsys input"));
                 java.util.Scanner scanner = new java.util.Scanner(stream, "UTF-8").useDelimiter("\\A")) {
                dump = scanner.hasNext() ? scanner.next() : "";
            }
            // shortcut: emulator dumpsys format, fail closed if Android changes it.
            int focused = dump.indexOf("  FocusedWindows:");
            if (focused < 0) continue;
            int start = dump.indexOf("name='", focused);
            if (start < 0) continue;
            int end = dump.indexOf("'", start + 6);
            if (end < 0) continue;
            String name = dump.substring(start + 6, end);
            if (!name.contains("com.example.quapp.CreateQueueActivity")) continue;
            for (String line : dump.split("\n")) {
                if (line.contains("name=" + name + ", id=") && line.contains(", alpha=1,")
                        && !line.contains("NOT_VISIBLE") && !line.contains("NOT_TOUCHABLE")
                        && !line.contains("NO_INPUT_CHANNEL")) return;
            }
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        fail("Category input window never became opaque and touchable");
    }

    private void openCreateTown(ActivityScenario<CreateQueueActivity> form) throws Exception {
        awaitFocusedForm(form);
        form.onActivity(a -> a.findViewById(R.id.create_municipality).performClick());
        awaitOpaqueFormDialog();
    }

    private void awaitFocusedForm(ActivityScenario<CreateQueueActivity> form) {
        long deadline = android.os.SystemClock.uptimeMillis() + 5000;
        boolean[] focused = {false};
        do {
            ins.waitForIdleSync();
            form.onActivity(a -> focused[0] = a.hasWindowFocus());
        } while (!focused[0] && android.os.SystemClock.uptimeMillis() < deadline);
        assertTrue("Form regained focus after dialog dismissal", focused[0]);
    }
}

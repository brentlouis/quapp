package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

/** Non-shipped visual fixtures; all production APIs and model instances remain unchanged. */
@RunWith(AndroidJUnit4.class)
public class BrowseJoinVisualReviewTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final String worst = "👩🏽‍💻 王秀英 — Aleksandra Wiśniewska-Kowalczyk";

    @Test public void reviewBrowseJoinScreens() throws Exception {
        Queue queue = FakeData.queues().get(0);
        boolean originalDebug = FakeData.isDebugMode();
        try {
            for (int screen = 0; screen < 3; screen++) {
                FakeData.setDebugMode(false);
                Activity activity = launch(screen, queue.getId());
                try {
                    instrumentation.waitForIdleSync();
                    capture(activity, screen, "default");
                    int title = screen == 0 ? R.id.browse_greeting : screen == 1 ? R.id.detail_name : R.id.join_queue_name;
                    instrumentation.runOnMainSync(() -> ((TextView) activity.findViewById(title)).setText(worst));
                    instrumentation.waitForIdleSync();
                    capture(activity, screen, "unicode-long");
                    if (screen == 0) {
                        swipeUp(activity);
                        capture(activity, screen, "collapsed-list");
                        instrumentation.runOnMainSync(() -> ((EditText) activity.findViewById(R.id.browse_search_input))
                            .setText("no-matching-demo-service-王秀英"));
                        Thread.sleep(500);
                        assertTrue(activity.findViewById(R.id.browse_empty).isShown());
                        capture(activity, screen, "empty-search");
                    } else {
                        NestedScrollView scroll = findScroll(activity.findViewById(android.R.id.content));
                        if (scroll != null) {
                            instrumentation.runOnMainSync(() -> scroll.fullScroll(View.FOCUS_DOWN));
                            instrumentation.waitForIdleSync();
                            capture(activity, screen, "bottom");
                        }
                        if (screen == 2) {
                            instrumentation.runOnMainSync(() -> {
                                activity.findViewById(R.id.join_edit_button).performClick();
                                ((EditText) activity.findViewById(R.id.join_name_input)).setText("");
                                ((EditText) activity.findViewById(R.id.join_phone_input)).setText("");
                                activity.findViewById(R.id.join_submit_button).performClick();
                                TextInputLayout field = activity.findViewById(R.id.join_phone_layout);
                                assertNotNull("Existing empty validation", field.getError());
                                field.requestRectangleOnScreen(new Rect(0, 0, field.getWidth(), field.getHeight()), true);
                            });
                            instrumentation.waitForIdleSync();
                            capture(activity, screen, "error-helper");
                            instrumentation.runOnMainSync(() -> {
                                EditText field = activity.findViewById(R.id.join_name_input);
                                field.setText(worst);
                                field.requestFocus();
                                ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                                    .showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
                            });
                            Thread.sleep(300);
                            capture(activity, screen, "keyboard");
                            instrumentation.runOnMainSync(() -> ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                                .hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0));
                        }
                    }
                } finally {
                    instrumentation.runOnMainSync(activity::finish);
                }
                if (screen < 2) {
                    FakeData.setDebugMode(true);
                    FakeData.setDebugDelayMs(5000);
                    FakeData.setDebugFailOnce(true);
                    Activity error = launch(screen, queue.getId());
                    try {
                        assertTrue(error.findViewById(screen == 0 ? R.id.browse_loading : R.id.detail_loading).isShown());
                        capture(error, screen, "loading");
                        View errorState = error.findViewById(screen == 0 ? R.id.browse_error : R.id.detail_error);
                        long deadline = android.os.SystemClock.uptimeMillis() + 10000;
                        while (!errorState.isShown() && android.os.SystemClock.uptimeMillis() < deadline) {
                            Thread.sleep(50);
                            instrumentation.waitForIdleSync();
                        }
                        assertTrue(errorState.isShown());
                        capture(error, screen, "error");
                        NestedScrollView stateScroll = findScroll(error.findViewById(screen == 0 ? R.id.browse_error : R.id.detail_error));
                        if (stateScroll != null) {
                            instrumentation.runOnMainSync(() -> stateScroll.fullScroll(View.FOCUS_DOWN));
                            instrumentation.waitForIdleSync();
                            capture(error, screen, "error-bottom");
                        }
                    } finally {
                        instrumentation.runOnMainSync(error::finish);
                        FakeData.setDebugFailOnce(false);
                    }
                }
            }
        } finally {
            FakeData.setDebugMode(originalDebug);
            FakeData.setDebugDelayMs(1500);
            FakeData.setDebugFailOnce(false);
            FakeData.setDebugEmptyOnce(false);
        }
    }

    private void swipeUp(Activity activity) {
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        int height = activity.getResources().getDisplayMetrics().heightPixels;
        long start = SystemClock.uptimeMillis();
        for (int i = 0; i <= 12; i++) {
            int action = i == 0 ? MotionEvent.ACTION_DOWN : i == 12 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE;
            MotionEvent event = MotionEvent.obtain(start, SystemClock.uptimeMillis(), action, width * .65f, height * (.68f - .4f * i / 12), 0);
            instrumentation.sendPointerSync(event);
            event.recycle();
            SystemClock.sleep(20);
        }
        instrumentation.waitForIdleSync();
    }

    @Test public void locationExplanationKeepsActionsReachable() throws Exception {
        Queue queue = FakeData.queues().get(0);
        Activity activity = launch(1, queue.getId());
        try {
            java.lang.reflect.Method show = QueueDetailActivity.class.getDeclaredMethod("showLocationSheet", Queue.class);
            show.setAccessible(true);
            instrumentation.runOnMainSync(() -> {
                try { show.invoke(activity, queue); } catch (Exception error) { throw new AssertionError(error); }
            });
            instrumentation.getUiAutomation().waitForIdle(500, 5000);
            capture(activity, 1, "location-explanation-top");
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(NestedScrollView.class))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.swipeUp());
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.location_sheet_not_now))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            capture(activity, 1, "location-explanation");
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.location_sheet_not_now))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click());
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }

    private Activity launch(int screen, String queueId) {
        Class<?> target = screen == 0 ? QueuerHomeActivity.class : screen == 1 ? QueueDetailActivity.class : JoinQueueActivity.class;
        Intent intent = new Intent(instrumentation.getTargetContext(), target);
        if (screen == 1) intent.putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, queueId);
        if (screen == 2) intent.putExtra(JoinQueueActivity.EXTRA_QUEUE_ID, queueId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return instrumentation.startActivitySync(intent);
    }

    private NestedScrollView findScroll(View view) {
        if (view instanceof NestedScrollView) return (NestedScrollView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                NestedScrollView found = findScroll(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void capture(Activity activity, int screen, String fixture) throws Exception {
        Thread.sleep(160);
        File folder = new File(activity.getExternalFilesDir(null), "phase2b-review");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        String name = new String[]{"explore", "service-details", "join-confirmation"}[screen];
        String size = activity.getResources().getConfiguration().screenWidthDp + "x" + activity.getResources().getConfiguration().screenHeightDp;
        String scale = String.valueOf(activity.getResources().getConfiguration().fontScale);
        try (FileOutputStream stream = new FileOutputStream(new File(folder, name + "-" + fixture + "-" + size + "-" + scale + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally {
            bitmap.recycle();
        }
    }
}

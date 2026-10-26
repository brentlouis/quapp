package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

/** Native development-only fixtures. Existing tests and FakeData remain untouched. */
@RunWith(AndroidJUnit4.class)
public class M3ExpressiveUiReviewTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private Activity activity;
    private View browse;
    private QueueAdapter adapter;
    private ContextThemeWrapper themed;

    private Queue queue(String id, String name, String description) {
        return new Queue.Builder().setId(id).setName(name).setVenue("Panglao Municipal Health and Community Services Center")
                .setMunicipality("Panglao").setCategory(Category.MEDICAL).setShortDescription(description)
                .setSchedule(LocalDate.now(), LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(17, 0))
                .setStatus(Queue.Status.OPEN).setPeopleWaiting(1284).setEstimatedWaitMinutes(125).build();
    }

    private List<Queue> fixture(boolean worst) {
        return Arrays.asList(queue("m3-a", worst ? "Aleksandra Wiśniewska-Kowalczyk Community Assistance" : "Community health services",
                worst ? "Free consultations for families, senior citizens and registered residents." : "Consultations and checkups."),
                queue("m3-b", "Jo", ""));
    }

    private void show(String state) {
        for (int id : new int[]{R.id.browse_list, R.id.browse_empty, R.id.browse_loading, R.id.browse_error})
            browse.findViewById(id).setVisibility(View.GONE);
        int id = "empty".equals(state) ? R.id.browse_empty : "loading".equals(state) ? R.id.browse_loading
                : "error".equals(state) ? R.id.browse_error : R.id.browse_list;
        browse.findViewById(id).setVisibility(View.VISIBLE);
        ((com.google.android.material.appbar.AppBarLayout) browse.findViewById(R.id.browse_app_bar))
                .setExpanded(id == R.id.browse_list, false);
        if (id == R.id.browse_list) adapter.submitQueues(fixture("worst".equals(state)));
        ((TextView) browse.findViewById(R.id.browse_empty_title)).setText("No queues nearby yet");
        ((TextView) browse.findViewById(R.id.browse_empty_body)).setText("Try another town or clear your filters to find an open queue.");
        ((TextView) browse.findViewById(R.id.browse_empty_button)).setText("Change town");
        browse.findViewById(R.id.browse_nearby_label).setVisibility(View.GONE);
        browse.findViewById(R.id.browse_nearby_row).setVisibility(View.GONE);
        QuappMotion.buttons(browse);
    }

    @Test public void renderBrowseAndVerifyPresentation() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        String mode = args.getString("mode", "light");
        float scale = Float.parseFloat(args.getString("scale", "1"));
        activity = instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                Configuration configuration = new Configuration(activity.getResources().getConfiguration());
                configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                        | ("dark".equals(mode) ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
                configuration.fontScale = scale;
                themed = new ContextThemeWrapper(activity.createConfigurationContext(configuration), R.style.Theme_Quapp);
                LinearLayout root = new LinearLayout(themed);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(
                        themed, android.R.attr.colorBackground, 0));
                activity.setContentView(root);
                SystemBars.applyPadding(root);
                androidx.core.view.ViewCompat.requestApplyInsets(root);
                browse = activity.getLayoutInflater().cloneInContext(themed)
                        .inflate(R.layout.fragment_browse, root, false);
                root.addView(browse, new LinearLayout.LayoutParams(-1, 0, 1f));
                browse.findViewById(R.id.browse_first_visit).setVisibility(View.GONE);
                browse.findViewById(R.id.browse_highlight).setVisibility(View.GONE);
                browse.findViewById(R.id.browse_banner).setVisibility(View.GONE);
                RecyclerView list = browse.findViewById(R.id.browse_list);
                list.setLayoutManager(new LinearLayoutManager(themed));
                adapter = new QueueAdapter(queue -> {});
                list.setAdapter(adapter);
                QuappMotion.list(list);
                com.google.android.material.snackbar.Snackbar snackbar = QuappMotion.snackbar(browse, "Review", -1);
                int duration = QuappMotion.enabled(themed) ? 150 : 0;
                for (int attribute : new int[]{com.google.android.material.R.attr.motionDurationLong2,
                        com.google.android.material.R.attr.motionDurationMedium1}) {
                    assertEquals("Snackbar fade respects motion guard", duration,
                            com.google.android.material.motion.MotionUtils.resolveThemeDuration(
                                    snackbar.getContext(), attribute, -1));
                }
                RadioGroup toggle = new RadioGroup(activity);
                toggle.setOrientation(RadioGroup.HORIZONTAL);
                for (String state : new String[]{"demo", "worst", "empty", "loading", "error"}) {
                    RadioButton button = new RadioButton(activity);
                    button.setText(state);
                    button.setOnClickListener(v -> show(state));
                    toggle.addView(button);
                }
                HorizontalScrollView track = new HorizontalScrollView(activity);
                track.addView(toggle);
                root.addView(track);
                final int[] moves = {0};
                adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override public void onItemRangeMoved(int from, int to, int count) { moves[0] += count; }
                });
                List<Queue> original = fixture(false);
                adapter.submitQueues(original);
                adapter.submitQueues(Arrays.asList(original.get(1), original.get(0)));
                assertTrue("Reorder must dispatch a move", moves[0] > 0);
                adapter.submitQueues(Collections.emptyList());
                assertEquals(0, adapter.getItemCount());
            });
            for (String state : new String[]{"demo", "worst", "empty", "loading", "error"}) {
                instrumentation.runOnMainSync(() -> show(state));
                instrumentation.waitForIdleSync();
                Thread.sleep(300);
                if ("error".equals(state)) {
                    instrumentation.runOnMainSync(() -> {
                        androidx.core.widget.NestedScrollView panel = browse.findViewById(R.id.browse_error);
                        panel.setSmoothScrollingEnabled(false);
                        panel.fullScroll(View.FOCUS_DOWN);
                    });
                    instrumentation.waitForIdleSync();
                }
                instrumentation.runOnMainSync(() -> {
                    assertTrue("State action remains a 48dp target", browse.findViewById(R.id.error_retry_button).getMinimumHeight()
                            >= 48 * themed.getResources().getDisplayMetrics().density - 1);
                    assertEquals("Reduced motion disables list animator", QuappMotion.enabled(themed),
                            ((RecyclerView) browse.findViewById(R.id.browse_list)).getItemAnimator() != null);
                    if ("error".equals(state)) {
                        View action = browse.findViewById(R.id.error_retry_button);
                        android.graphics.Rect visible = new android.graphics.Rect();
                        assertTrue("Retry action is reachable", action.getGlobalVisibleRect(visible));
                        View panel = browse.findViewById(R.id.browse_error);
                        assertTrue("Retry action is not clipped: visible=" + visible + ", actionHeight="
                                + action.getHeight() + ", panelHeight=" + panel.getHeight() + ", scrollY="
                                + panel.getScrollY() + ", childHeight=" + ((android.view.ViewGroup) panel).getChildAt(0).getHeight(),
                                visible.height() >= action.getHeight() - 1);
                    }
                });
                File folder = new File(activity.getExternalFilesDir(null), "m3-review");
                assertTrue(folder.isDirectory() || folder.mkdirs());
                Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
                assertNotNull(screenshot);
                String width = args.getString("width", "device");
                try (FileOutputStream output = new FileOutputStream(new File(folder,
                        width + "-" + mode + "-" + scale + "-" + state + ".png"))) {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, output);
                }
                screenshot.recycle();
            }
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }
}

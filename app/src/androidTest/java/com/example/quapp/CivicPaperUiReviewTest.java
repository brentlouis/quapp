package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Development-only review: renders the production controls and QueueCards binder with
 * plausible difficult content. Lives in androidTest and never enters a shipped APK.
 * Runner arguments: screen=controls|cards|owner|login|console, fixture=demo|worst|empty|one.
 */
@RunWith(AndroidJUnit4.class)
public class CivicPaperUiReviewTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final List<MaterialButton> actionButtons = new ArrayList<>();
    private Activity activity;
    private ScrollView scroll;
    private LinearLayout content;
    private String screen;
    private String fixture;

    @Test
    public void reviewNativeComponents() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        screen = args.getString("screen", "controls");
        fixture = args.getString("fixture", "demo");
        Intent intent = new Intent(instrumentation.getTargetContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = instrumentation.startActivitySync(intent);
        try {
            instrumentation.runOnMainSync(this::render);
            instrumentation.waitForIdleSync();
            // Allow a real frame to finish before UiAutomation takes the screenshot.
            Thread.sleep(350);
            instrumentation.runOnMainSync(() -> {
                float density = activity.getResources().getDisplayMetrics().density;
                for (MaterialButton button : actionButtons) {
                    assertTrue("Action is narrower than 48dp", button.getWidth() >= 48 * density - 1);
                    assertTrue("Action is shorter than 48dp", button.getHeight() >= 48 * density - 1);
                    assertNotNull(button.getLayout());
                    for (int line = 0; line < button.getLayout().getLineCount(); line++) {
                        assertTrue("Action label was ellipsized",
                                button.getLayout().getEllipsisCount(line) == 0);
                        assertTrue("Action label exceeds its available width",
                                button.getLayout().getLineWidth(line)
                                        <= button.getLayout().getWidth() + 1);
                    }
                }
            });
            capture("top");
            if (scroll != null) {
                instrumentation.runOnMainSync(() -> scroll.fullScroll(View.FOCUS_DOWN));
                instrumentation.waitForIdleSync();
                Thread.sleep(150);
                capture("bottom");
            }
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    private void render() {
        if ("login".equals(screen)) {
            actionButtons.add(activity.findViewById(R.id.login_submit_button));
            actionButtons.add(activity.findViewById(R.id.login_forgot_button));
            actionButtons.add(activity.findViewById(R.id.login_register_button));
            return;
        }
        if ("console".equals(screen)) {
            activity.setContentView(R.layout.activity_live_console);
            View root = activity.findViewById(android.R.id.content);
            SystemBars.applyPadding(root);
            ViewCompat.requestApplyInsets(root);
            ((TextView) activity.findViewById(R.id.console_serving_name))
                    .setText("Aleksandra Wiśniewska-Kowalczyk");
            ((TextView) activity.findViewById(R.id.console_serving_number)).setText("#1284");
            ((TextView) activity.findViewById(R.id.console_serving_phone)).setText("0917 ••• 0002");
            MaterialButton call = activity.findViewById(R.id.console_call_next);
            call.setText("Served · call the next person");
            actionButtons.add(call);
            actionButtons.add(activity.findViewById(R.id.console_no_show));
            return;
        }

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        activity.setContentView(root);
        SystemBars.applyPaddingWithKeyboard(root);
        ViewCompat.requestApplyInsets(root);
        TextView title = new TextView(activity);
        title.setText("Civic Paper · " + screen);
        title.setTextAppearance(R.style.TextAppearance_Quapp_HeadingSmall);
        title.setPadding(dp(16), dp(12), dp(16), 0);
        root.addView(title);
        HorizontalScrollView strip = new HorizontalScrollView(activity);
        RadioGroup toggle = new RadioGroup(activity);
        toggle.setOrientation(RadioGroup.HORIZONTAL);
        for (String name : new String[]{"demo", "worst", "empty", "one"}) {
            RadioButton radio = new RadioButton(activity);
            radio.setId(View.generateViewId());
            radio.setText("worst".equals(name) ? "Worst case" : name);
            radio.setTextAppearance(R.style.TextAppearance_Quapp_Caption);
            radio.setMinHeight(dp(48));
            radio.setTag(name);
            toggle.addView(radio);
            radio.setChecked(name.equals(fixture));
        }
        strip.addView(toggle);
        root.addView(strip);
        scroll = new ScrollView(activity);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(content);
        toggle.setOnCheckedChangeListener((group, id) -> {
            fixture = (String) group.findViewById(id).getTag();
            bindContent();
        });
        bindContent();
    }

    private void bindContent() {
        content.removeAllViews();
        actionButtons.clear();
        if ("controls".equals(screen)) {
            TextView heading = new TextView(activity);
            heading.setText("Clear actions. Familiar paper.");
            heading.setTextAppearance(R.style.TextAppearance_Quapp_Title);
            content.addView(heading);
            View login = activity.getLayoutInflater().inflate(R.layout.activity_login, null);
            addAction(login, R.id.login_submit_button, "worst".equals(fixture)
                    ? "Confirm and join the community queue" : "Join queue", true);
            View console = activity.getLayoutInflater().inflate(R.layout.activity_live_console, null);
            addAction(console, R.id.console_no_show, "Retry loading available queues", true);
            View ticket = activity.getLayoutInflater().inflate(R.layout.activity_active_ticket, null);
            addAction(ticket, R.id.ticket_more_time_button, "I need more time", true);
            View disabled = activity.getLayoutInflater().inflate(R.layout.activity_login, null);
            addAction(disabled, R.id.login_submit_button, "worst".equals(fixture)
                    ? "Confirm and join the community queue" : "Join queue", false);
            TextView label = login.findViewById(R.id.login_phone_label);
            detach(label);
            label.setText("Mobile number");
            content.addView(label);
            View field = login.findViewById(R.id.login_phone_layout);
            detach(field);
            content.addView(field);
            if ("empty".equals(fixture)) {
                ((com.google.android.material.textfield.TextInputLayout) field)
                        .setError("Enter your mobile number to continue.");
            }
            return;
        }
        if ("empty".equals(fixture)) {
            View empty = activity.getLayoutInflater().inflate(R.layout.view_error_state, content, false);
            empty.setVisibility(View.VISIBLE);
            ((TextView) empty.findViewById(R.id.error_title)).setText("No queues available");
            ((TextView) empty.findViewById(R.id.error_message))
                    .setText("Try another town or check back later.");
            content.addView(empty);
            return;
        }
        boolean worst = "worst".equals(fixture);
        boolean owner = "owner".equals(screen);
        int count = "one".equals(fixture) ? 1 : 4;
        Queue.Status[] states = {Queue.Status.OPEN, Queue.Status.PAUSED,
                Queue.Status.UPCOMING, Queue.Status.CLOSED};
        for (int i = 0; i < count; i++) {
            LocalDate today = LocalDate.now();
            Queue queue = new Queue.Builder().setId("q" + (i + 1))
                    .setName(worst && i == 0
                            ? "Barangay Poblacion Community Health and Social Services"
                            : (worst && i == 1 ? "Jo" : "Community service queue " + (i + 1)))
                    .setOrganizer("review", "Community office", i == 0)
                    .setVenue(worst
                            ? "Municipal Multi-Purpose Hall, Barangay Poblacion, Tagbilaran City"
                            : "Barangay Poblacion Hall")
                    .setMunicipality("Tagbilaran City")
                    .setShortDescription(worst && i == 1 ? ""
                            : "Health consultations and community assistance")
                    .setSchedule(today, worst ? today.plusDays(2) : today,
                            LocalTime.of(8, 0), LocalTime.of(23, 0))
                    .setStatus(states[i])
                    .setPausedAt(states[i] == Queue.Status.PAUSED ? Instant.now() : null)
                    .setClosedAt(states[i] == Queue.Status.CLOSED ? Instant.now() : null)
                    .setPeopleWaiting(worst ? 1284 : ("one".equals(fixture) ? 1 : 42))
                    .setNowServing(worst && i == 1 ? null : (worst ? 999 : 21))
                    .setEstimatedWaitMinutes(worst ? 125 : 55).setProximity(true, 1000).build();
            View card = activity.getLayoutInflater().inflate(R.layout.item_queue_card, content, false);
            QueueStats stats = owner ? new QueueStats(worst ? 1284 : 18, 3,
                    queue.getPeopleWaiting(), 3, 20, queue.getEstimatedWaitMinutes(),
                    QueueStats.EstimateSource.ROLLING_AVERAGE, 0) : null;
            QueueCards.bind(card, queue, stats);
            card.setClickable(false);
            card.setFocusable(false);
            content.addView(card);
        }
    }

    private void addAction(View source, int id, String text, boolean enabled) {
        MaterialButton button = source.findViewById(id);
        detach(button);
        button.setText(text);
        button.setEnabled(enabled);
        button.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(8);
        content.addView(button, params);
        actionButtons.add(button);
    }

    private static void detach(View view) {
        ((ViewGroup) view.getParent()).removeView(view);
    }

    private void capture(String position) throws Exception {
        File folder = new File(activity.getExternalFilesDir(null), "ui-review");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        String scale = String.valueOf(activity.getResources().getConfiguration().fontScale);
        File file = new File(folder, screen + "-" + fixture + "-" + scale + "-" + position + ".png");
        try (FileOutputStream stream = new FileOutputStream(file)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally {
            bitmap.recycle();
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}

package com.example.quapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Report a queue (canvas 54). Reports go to the admin page; the organizer never sees who sent
 * one (DECISIONS.md "Anti-prank measures"). Opened with a queue id from Queue detail, or
 * without one from Help and support, where it asks which queue.
 */
public class ReportQueueActivity extends AppCompatActivity {

    /** Optional: without it, the screen shows a queue picker. */
    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_REPORT_QUEUE_ID";

    private static final String STATE_QUEUE = "queue";
    private static final String STATE_REASON = "reason";

    @Nullable
    private String queueId;
    @Nullable
    private Report.Reason reason;
    private final List<View> reasonRows = new ArrayList<>();

    public static Intent intent(Context context, String queueId) {
        Intent intent = new Intent(context, ReportQueueActivity.class);
        intent.putExtra(EXTRA_QUEUE_ID, queueId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_queue);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.report_root));

        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        boolean picking = queueId == null;
        if (savedInstanceState != null) {
            queueId = savedInstanceState.getString(STATE_QUEUE);
            String saved = savedInstanceState.getString(STATE_REASON);
            reason = saved == null ? null : Report.Reason.valueOf(saved);
        }

        findViewById(R.id.report_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.report_queue).setVisibility(picking ? View.GONE : View.VISIBLE);
        findViewById(R.id.report_pick_group).setVisibility(picking ? View.VISIBLE : View.GONE);
        findViewById(R.id.report_pick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickQueue();
            }
        });
        bindQueue();
        addReasons();

        findViewById(R.id.report_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                send();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUEUE, queueId);
        outState.putString(STATE_REASON, reason == null ? null : reason.name());
    }

    /** "Purok 3 Rice Sharing · Purok 3 Youth Volunteers", on the line or in the picker. */
    private void bindQueue() {
        Queue queue = queueId == null ? null : FakeData.queueById(queueId);
        String line = queue == null ? null : getString(R.string.report_queue_format,
                queue.getName(), queue.getOrganizerName());
        ((TextView) findViewById(R.id.report_queue)).setText(line);
        ((TextView) findViewById(R.id.report_pick)).setText(line);
    }

    /** From Help: every queue people can see on Browse (closed ones aren't listed). */
    private void pickQueue() {
        final List<Queue> queues = new ArrayList<>();
        for (Queue queue : FakeData.queues()) {
            if (queue.getStatus() != Queue.Status.CLOSED) {
                queues.add(queue);
            }
        }
        String[] names = new String[queues.size()];
        for (int i = 0; i < queues.size(); i++) {
            names[i] = getString(R.string.report_queue_format,
                    queues.get(i).getName(), queues.get(i).getOrganizerName());
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.report_pick_label)
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        queueId = queues.get(which).getId();
                        findViewById(R.id.report_pick_error).setVisibility(View.GONE);
                        bindQueue();
                    }
                })
                .setNegativeButton(R.string.console_cancel, null)
                .show();
    }

    /** The four reasons as radio rows; the whole row is the touch target. */
    private void addReasons() {
        LinearLayout group = findViewById(R.id.report_reasons);
        LayoutInflater inflater = getLayoutInflater();
        for (final Report.Reason option : Report.Reason.values()) {
            View row = inflater.inflate(R.layout.view_option_row, group, false);
            row.findViewById(R.id.option_tile).setVisibility(View.GONE);
            row.findViewById(R.id.option_radio).setVisibility(View.VISIBLE);
            ((TextView) row.findViewById(R.id.option_title)).setText(option.label);
            ((TextView) row.findViewById(R.id.option_body)).setText(option.body);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reason = option;
                    findViewById(R.id.report_reason_error).setVisibility(View.GONE);
                    checkReason();
                }
            });
            row.setTag(option);
            group.addView(row);
            reasonRows.add(row);
        }
        checkReason();
    }

    /** Like a RadioGroup: the chosen row on, the rest off. */
    private void checkReason() {
        for (View row : reasonRows) {
            boolean on = row.getTag() == reason;
            ((MaterialRadioButton) row.findViewById(R.id.option_radio)).setChecked(on);
            row.setSelected(on);
        }
    }

    private void send() {
        String details = Forms.text(findViewById(R.id.report_details_input));

        // Every problem shows at once, top to bottom.
        boolean valid = queueId != null;
        findViewById(R.id.report_pick_error).setVisibility(valid ? View.GONE : View.VISIBLE);
        boolean hasReason = reason != null;
        findViewById(R.id.report_reason_error).setVisibility(hasReason ? View.GONE : View.VISIBLE);
        valid &= hasReason;
        // "Something else" means nothing unless they say what.
        valid &= Forms.check(findViewById(R.id.report_details_layout),
                reason != Report.Reason.OTHER || !details.isEmpty(),
                getString(R.string.report_details_error));
        if (!valid) {
            return;
        }

        FakeData.report(queueId, reason, details.isEmpty() ? null : details);
        Toast.makeText(this, R.string.report_sent, Toast.LENGTH_LONG).show();
        finish();
    }
}

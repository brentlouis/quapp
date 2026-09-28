package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class QueueDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_QUEUE_ID";

    private String queueId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_detail);
        SystemBars.applyPadding(findViewById(R.id.detail_root));

        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);

        if (FakeData.queueById(queueId) == null) {
            finish();
            return;
        }

        ImageButton backButton = findViewById(R.id.detail_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /** Re-bound on every return: the ticket or cooldown may have changed while away. */
    @Override
    protected void onResume() {
        super.onResume();

        Queue queue = FakeData.queueById(queueId);
        if (queue == null) {
            finish();
            return;
        }

        bindQueue(queue);
        bindJoinButton(queue);
    }

    /**
     * Checked in priority order — the first one that applies decides the button:
     * already holding this queue's ticket, queue not open, holding another
     * queue's ticket, on cooldown, or free to join.
     */
    private void bindJoinButton(final Queue queue) {
        MaterialButton joinButton = findViewById(R.id.detail_join_button);
        TextView notice = findViewById(R.id.detail_notice);
        Ticket ticket = ActiveTicketStore.getTicket();

        notice.setVisibility(View.GONE);
        joinButton.setEnabled(false);
        joinButton.setOnClickListener(null);

        if (ticket != null && ticket.getQueueId().equals(queue.getId())) {
            joinButton.setEnabled(true);
            joinButton.setText(R.string.detail_view_ticket_action);
            joinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(QueueDetailActivity.this, ActiveTicketActivity.class));
                }
            });
            return;
        }

        if (queue.getStatus() == Queue.Status.CLOSED) {
            joinButton.setText(R.string.detail_closed_action);
            return;
        }

        if (queue.getStatus() == Queue.Status.PAUSED) {
            joinButton.setText(R.string.detail_paused_action);
            showNotice(notice, getString(R.string.detail_paused_notice));
            return;
        }

        if (ActiveTicketStore.hasLiveTicket()) {
            joinButton.setText(R.string.detail_busy_action);
            showNotice(notice, getString(R.string.detail_busy_notice, ticket.getQueueName()));
            return;
        }

        if (queue.isNoShowPenaltyEnabled() && Cooldown.isActive()) {
            joinButton.setText(R.string.detail_cooldown_action);
            showNotice(notice, getString(R.string.detail_cooldown_notice,
                    Cooldown.remainingMinutes()));
            return;
        }

        joinButton.setEnabled(true);
        joinButton.setText(R.string.detail_join_action);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(QueueDetailActivity.this, JoinQueueActivity.class);
                intent.putExtra(JoinQueueActivity.EXTRA_QUEUE_ID, queue.getId());
                startActivity(intent);
            }
        });
    }

    private void showNotice(TextView notice, String text) {
        notice.setText(text);
        notice.setVisibility(View.VISIBLE);
    }

    private void bindQueue(Queue queue) {
        TextView nameText = findViewById(R.id.detail_name);
        TextView venueText = findViewById(R.id.detail_venue);
        TextView categoryText = findViewById(R.id.detail_category);
        View waitingStat = findViewById(R.id.detail_waiting);
        View etaStat = findViewById(R.id.detail_eta);
        TextView waitingValue = waitingStat.findViewById(R.id.stat_value);
        TextView etaValue = etaStat.findViewById(R.id.stat_value);
        TextView hoursValue = findViewById(R.id.detail_hours_value);
        TextView descriptionText = findViewById(R.id.detail_description);

        nameText.setText(queue.getName());
        venueText.setText(getString(R.string.browse_venue_format,
                queue.getVenue(), queue.getMunicipality()));
        categoryText.setText(queue.getCategory());
        ((TextView) waitingStat.findViewById(R.id.stat_label)).setText(R.string.detail_waiting_label);
        ((TextView) etaStat.findViewById(R.id.stat_label)).setText(R.string.detail_eta_label);
        waitingValue.setText(String.valueOf(queue.getPeopleWaiting()));
        hoursValue.setText(queue.getServiceHours());
        descriptionText.setText(queue.getDescription());

        if (queue.getStatus() == Queue.Status.CLOSED) {
            etaValue.setText(R.string.browse_eta_none);
        } else {
            etaValue.setText(getString(R.string.detail_eta_value_format,
                    queue.getEstimatedWaitMinutes()));
        }

        bindVerification(queue);
    }

    private void bindVerification(Queue queue) {
        View section = findViewById(R.id.detail_verification_section);

        if (!queue.hasAnyVerification()) {
            section.setVisibility(View.GONE);
            return;
        }

        section.setVisibility(View.VISIBLE);

        bindRule(R.id.detail_rule_otp, queue.isSmsOtpEnabled(), R.drawable.ic_sms,
                R.string.requirement_otp_title, R.string.detail_verification_otp);
        bindRule(R.id.detail_rule_grace, queue.isGracePeriodEnabled(), R.drawable.ic_timer,
                R.string.requirement_grace_title, R.string.detail_verification_grace);
        bindRule(R.id.detail_rule_penalty, queue.isNoShowPenaltyEnabled(), R.drawable.ic_block,
                R.string.requirement_penalty_title, R.string.detail_verification_penalty);
        bindRule(R.id.detail_rule_proximity, queue.isProximityCheckEnabled(), R.drawable.ic_place,
                R.string.requirement_proximity_title, R.string.detail_verification_proximity);
    }

    private void bindRule(int includeId, boolean enabled, int iconRes, int titleRes, int bodyRes) {
        View row = findViewById(includeId);
        row.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) {
            return;
        }

        ImageView icon = row.findViewById(R.id.requirement_icon);
        TextView title = row.findViewById(R.id.requirement_title);
        TextView body = row.findViewById(R.id.requirement_body);

        icon.setImageResource(iconRes);
        title.setText(titleRes);
        body.setText(bodyRes);
    }
}

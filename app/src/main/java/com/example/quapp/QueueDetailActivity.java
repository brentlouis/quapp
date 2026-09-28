package com.example.quapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

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

        findViewById(R.id.detail_directions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Directions.open(QueueDetailActivity.this, FakeData.queueById(queueId));
            }
        });

        // Tear-off stub: raised paper with its top corners bitten out.
        findViewById(R.id.detail_dock).setBackground(TicketShapes.stubDockBackground(this));
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
        View youWillBe = findViewById(R.id.detail_you_will_be);
        Ticket ticket = ActiveTicketStore.getTicket();

        notice.setVisibility(View.GONE);
        youWillBe.setVisibility(View.GONE);
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

        // Free to join: the stub shows the number you'd be handed.
        youWillBe.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.detail_next_number)).setText(
                getString(R.string.ticket_number_format, queue.getPeopleWaiting() + 1));
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
        ((TextView) findViewById(R.id.detail_name)).setText(queue.getName());
        ((TextView) findViewById(R.id.detail_venue)).setText(getString(R.string.browse_venue_format,
                queue.getVenue(), queue.getMunicipality()));
        // The serial line: the category printed like a ticket serial (the Label style caps it).
        ((TextView) findViewById(R.id.detail_category)).setText(queue.getCategory());
        ((TextView) findViewById(R.id.detail_description)).setText(queue.getDescription());

        bindStatus(queue.getStatus());

        LinearLayout slip = findViewById(R.id.detail_slip);
        ReceiptSlip.clear(slip);
        ReceiptSlip.addRow(slip, getString(R.string.detail_eta_label),
                queue.getStatus() == Queue.Status.CLOSED ? getString(R.string.browse_eta_none)
                        : getString(R.string.detail_eta_value_format, queue.getEstimatedWaitMinutes()));
        ReceiptSlip.addRow(slip, getString(R.string.detail_waiting_label),
                String.valueOf(queue.getPeopleWaiting()));
        ReceiptSlip.addRow(slip, getString(R.string.detail_hours_slip_label), queue.getServiceHours());

        bindVerification(queue);
    }

    /** Open is green on its soft ground, paused amber, closed a plain outline-less grey. */
    private void bindStatus(Queue.Status status) {
        TextView pill = findViewById(R.id.detail_status);
        int text;
        int color;
        int ground;
        switch (status) {
            case PAUSED:
                text = R.string.detail_status_paused;
                color = R.color.warn;
                ground = R.color.warn_soft;
                break;
            case CLOSED:
                text = R.string.detail_status_closed;
                color = R.color.ink_muted;
                ground = R.color.paper_sunk;
                break;
            case OPEN:
            default:
                text = R.string.detail_status_open;
                color = R.color.ok;
                ground = R.color.ok_soft;
                break;
        }
        pill.setText(text);
        pill.setTextColor(ContextCompat.getColor(this, color));
        ViewCompat.setBackgroundTintList(pill,
                ColorStateList.valueOf(ContextCompat.getColor(this, ground)));
    }

    /**
     * The checks always appear in the same order. Ones this queue uses say ON; SMS confirmation
     * isn't built (future work), so it always shows as PLANNED.
     */
    private void bindVerification(Queue queue) {
        bindRule(R.id.detail_rule_proximity, queue.isProximityCheckEnabled(), R.drawable.ic_navigation,
                R.string.requirement_proximity_title, getString(R.string.detail_verification_proximity));
        bindRule(R.id.detail_rule_grace, queue.isGracePeriodEnabled(), R.drawable.ic_timer,
                R.string.requirement_grace_title, getString(R.string.detail_verification_grace));
        bindRule(R.id.detail_rule_penalty, queue.isNoShowPenaltyEnabled(), R.drawable.ic_block,
                R.string.requirement_penalty_title, getString(R.string.detail_verification_penalty));

        View otp = findViewById(R.id.detail_rule_otp);
        fillRule(otp, R.drawable.ic_sms, R.string.requirement_otp_title,
                getString(R.string.detail_verification_otp_planned), false);
    }

    private void bindRule(int includeId, boolean enabled, int iconRes, int titleRes, String body) {
        View row = findViewById(includeId);
        row.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (enabled) {
            fillRule(row, iconRes, titleRes, body, true);
        }
    }

    private void fillRule(View row, int iconRes, int titleRes, String body, boolean on) {
        ImageView icon = row.findViewById(R.id.requirement_icon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(this, on ? R.color.ok : R.color.ink_muted));
        ((TextView) row.findViewById(R.id.requirement_title)).setText(titleRes);
        ((TextView) row.findViewById(R.id.requirement_body)).setText(body);

        TextView state = row.findViewById(R.id.requirement_state);
        state.setText(on ? R.string.detail_rule_on : R.string.detail_rule_planned);
        state.setTextColor(ContextCompat.getColor(this, on ? R.color.ok : R.color.ink_muted));
        ViewCompat.setBackgroundTintList(state, ColorStateList.valueOf(
                ContextCompat.getColor(this, on ? R.color.ok_soft : R.color.paper_sunk)));
    }
}

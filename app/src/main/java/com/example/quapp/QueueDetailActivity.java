package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class QueueDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_QUEUE_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_detail);
        SystemBars.applyPadding(findViewById(R.id.detail_root));

        String queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        Queue queue = FakeData.queueById(queueId);

        if (queue == null) {
            finish();
            return;
        }

        bindQueue(queue);

        ImageButton backButton = findViewById(R.id.detail_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        MaterialButton joinButton = findViewById(R.id.detail_join_button);

        if (queue.isOpen()) {
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
        } else {
            joinButton.setEnabled(false);
            joinButton.setText(R.string.detail_closed_action);
        }
    }

    private void bindQueue(Queue queue) {
        TextView nameText = findViewById(R.id.detail_name);
        TextView venueText = findViewById(R.id.detail_venue);
        TextView categoryText = findViewById(R.id.detail_category);
        TextView waitingValue = findViewById(R.id.detail_waiting_value);
        TextView etaValue = findViewById(R.id.detail_eta_value);
        TextView hoursValue = findViewById(R.id.detail_hours_value);
        TextView descriptionText = findViewById(R.id.detail_description);

        nameText.setText(queue.getName());
        venueText.setText(getString(R.string.browse_venue_format,
                queue.getVenue(), queue.getMunicipality()));
        categoryText.setText(queue.getCategory());
        waitingValue.setText(String.valueOf(queue.getPeopleWaiting()));
        hoursValue.setText(queue.getServiceHours());
        descriptionText.setText(queue.getDescription());

        if (queue.isOpen()) {
            etaValue.setText(getString(R.string.detail_eta_value_format,
                    queue.getEstimatedWaitMinutes()));
        } else {
            etaValue.setText(R.string.browse_eta_none);
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

        showIf(R.id.detail_verification_otp, queue.isSmsOtpEnabled());
        showIf(R.id.detail_verification_grace, queue.isGracePeriodEnabled());
        showIf(R.id.detail_verification_penalty, queue.isNoShowPenaltyEnabled());
        showIf(R.id.detail_verification_proximity, queue.isProximityCheckEnabled());
    }

    private void showIf(int viewId, boolean visible) {
        findViewById(viewId).setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class JoinQueueActivity extends AppCompatActivity {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_JOIN_QUEUE_ID";

    private Queue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_queue);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.join_root));

        String queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        queue = FakeData.queueById(queueId);

        if (queue == null) {
            finish();
            return;
        }

        bindQueue();
        bindRequirements();

        ImageButton backButton = findViewById(R.id.join_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        MaterialButton submitButton = findViewById(R.id.join_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitJoin();
            }
        });
    }

    private void bindQueue() {
        TextView queueName = findViewById(R.id.join_queue_name);
        TextView positionPreview = findViewById(R.id.join_position_preview);

        queueName.setText(getString(R.string.browse_venue_format,
                queue.getName(), queue.getMunicipality()));

        positionPreview.setText(getString(R.string.join_position_format,
                queue.getPeopleWaiting() + 1));
    }

    private void bindRequirements() {
        View section = findViewById(R.id.join_requirements_section);

        if (!queue.hasAnyVerification()) {
            section.setVisibility(View.GONE);
            return;
        }

        section.setVisibility(View.VISIBLE);

        bindRequirement(R.id.join_requirement_otp, queue.isSmsOtpEnabled(),
                R.string.join_otp_title, R.string.join_otp_body);

        bindRequirement(R.id.join_requirement_grace, queue.isGracePeriodEnabled(),
                R.string.join_grace_title, R.string.join_grace_body);

        bindRequirement(R.id.join_requirement_penalty, queue.isNoShowPenaltyEnabled(),
                R.string.join_penalty_title, R.string.join_penalty_body);

        bindRequirement(R.id.join_requirement_proximity, queue.isProximityCheckEnabled(),
                R.string.join_proximity_title, R.string.join_proximity_body);
    }

    private void bindRequirement(int includeId, boolean enabled, int titleRes, int bodyRes) {
        View block = findViewById(includeId);

        if (!enabled) {
            block.setVisibility(View.GONE);
            return;
        }

        block.setVisibility(View.VISIBLE);

        TextView titleText = block.findViewById(R.id.requirement_title);
        TextView bodyText = block.findViewById(R.id.requirement_body);

        titleText.setText(titleRes);
        bodyText.setText(bodyRes);
    }

    private void submitJoin() {
        TextInputEditText nameInput = findViewById(R.id.join_name_input);
        TextInputEditText phoneInput = findViewById(R.id.join_phone_input);

        String holderName = nameInput.getText() == null
                ? "" : nameInput.getText().toString().trim();
        String holderPhone = phoneInput.getText() == null
                ? "" : phoneInput.getText().toString().trim();

        Ticket ticket = new Ticket(
                "t-" + queue.getId(),
                queue.getId(),
                queue.getName(),
                queue.getVenue(),
                holderName,
                holderPhone,
                queue.getPeopleWaiting() + 1,
                queue.getPeopleWaiting() + 1,
                queue.getEstimatedWaitMinutes(),
                Ticket.Status.WAITING);

        ActiveTicketStore.setTicket(ticket);

        Intent intent = new Intent(this, ActiveTicketActivity.class);
        startActivity(intent);
        finish();
    }
}
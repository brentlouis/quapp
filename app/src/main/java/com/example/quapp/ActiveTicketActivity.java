package com.example.quapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class ActiveTicketActivity extends AppCompatActivity {

    private static final long GRACE_PERIOD_MS = 180_000L;
    private static final long COUNTDOWN_TICK_MS = 1_000L;

    private Ticket ticket;
    private CountDownTimer graceTimer;

    private TextView statusBanner;
    private TextView ticketNumber;
    private TextView countdownText;
    private TextView instructionText;
    private TextView positionText;
    private TextView etaText;
    private MaterialButton primaryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_ticket);
        SystemBars.applyPadding(findViewById(R.id.ticket_root));

        ticket = ActiveTicketStore.getTicket();

        if (ticket == null) {
            finish();
            return;
        }

        cacheViews();
        bindStaticDetails();
        renderState(ticket.getStatus());

        ImageButton backButton = findViewById(R.id.ticket_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void cacheViews() {
        statusBanner = findViewById(R.id.ticket_status_banner);
        ticketNumber = findViewById(R.id.ticket_number);
        countdownText = findViewById(R.id.ticket_countdown);
        instructionText = findViewById(R.id.ticket_instruction);
        positionText = findViewById(R.id.ticket_position);
        etaText = findViewById(R.id.ticket_eta);
        primaryButton = findViewById(R.id.ticket_primary_button);
    }

    private void bindStaticDetails() {
        TextView queueName = findViewById(R.id.ticket_queue_name);
        TextView venue = findViewById(R.id.ticket_venue);

        queueName.setText(ticket.getQueueName());
        venue.setText(ticket.getVenue());

        ticketNumber.setText(String.valueOf(ticket.getTicketNumber()));
        positionText.setText(String.valueOf(ticket.getPosition()));
        etaText.setText(getString(R.string.detail_eta_value_format,
                ticket.getEstimatedWaitMinutes()));
    }

    private void renderState(Ticket.Status status) {
        cancelGraceTimer();

        switch (status) {
            case WAITING:
                renderWaiting();
                break;
            case CALLED:
                renderCalled();
                break;
            case SERVED:
                renderServed();
                break;
            case NO_SHOW:
                renderExpired();
                break;
        }
    }

    private void renderWaiting() {
        setBanner(R.string.ticket_status_waiting, R.color.status_waiting);
        countdownText.setVisibility(View.GONE);
        instructionText.setVisibility(View.GONE);

        primaryButton.setText(R.string.ticket_leave_action);
        primaryButton.setEnabled(true);
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.clearTicket();
                finish();
            }
        });

        // Demo hook: long-press the number to simulate being called.
        ticketNumber.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                renderState(Ticket.Status.CALLED);
                return true;
            }
        });
    }

    private void renderCalled() {
        setBanner(R.string.ticket_status_called, R.color.status_called);

        countdownText.setVisibility(View.VISIBLE);
        instructionText.setVisibility(View.VISIBLE);
        instructionText.setText(R.string.ticket_called_instruction);

        positionText.setText(R.string.browse_eta_none);

        primaryButton.setText(R.string.ticket_here_action);
        primaryButton.setEnabled(true);
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renderState(Ticket.Status.SERVED);
            }
        });

        startGraceTimer();
    }

    private void renderServed() {
        setBanner(R.string.ticket_status_served, R.color.status_served);

        countdownText.setVisibility(View.GONE);
        instructionText.setVisibility(View.VISIBLE);
        instructionText.setText(R.string.ticket_served_instruction);

        primaryButton.setText(R.string.ticket_done_action);
        primaryButton.setEnabled(true);
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.clearTicket();
                finish();
            }
        });
    }

    private void renderExpired() {
        setBanner(R.string.ticket_status_expired, R.color.status_expired);

        countdownText.setVisibility(View.GONE);
        instructionText.setVisibility(View.VISIBLE);
        instructionText.setText(R.string.ticket_expired_instruction);

        primaryButton.setText(R.string.ticket_done_action);
        primaryButton.setEnabled(true);
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.clearTicket();
                finish();
            }
        });
    }

    private void setBanner(int textRes, int colorRes) {
        statusBanner.setText(textRes);
        statusBanner.setBackgroundColor(ContextCompat.getColor(this, colorRes));
        statusBanner.setTextColor(Color.WHITE);
    }

    private void startGraceTimer() {
        graceTimer = new CountDownTimer(GRACE_PERIOD_MS, COUNTDOWN_TICK_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000L;
                long minutes = totalSeconds / 60L;
                long seconds = totalSeconds % 60L;

                countdownText.setText(String.format(Locale.US,
                        getString(R.string.ticket_countdown_format), minutes, seconds));
            }

            @Override
            public void onFinish() {
                renderState(Ticket.Status.NO_SHOW);
            }
        };

        graceTimer.start();
    }

    private void cancelGraceTimer() {
        if (graceTimer != null) {
            graceTimer.cancel();
            graceTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelGraceTimer();
    }
}
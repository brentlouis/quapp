package com.example.quapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;

public class ActiveTicketActivity extends AppCompatActivity {

    private static final long GRACE_PERIOD_MS = 180_000L;
    private static final long COUNTDOWN_TICK_MS = 1_000L;

    private CountDownTimer graceTimer;

    // Track behind the grace ring: the content color at 25% opacity.
    private static final int RING_TRACK_ALPHA = 64;

    private View hero;
    private TextView statusText;
    private TextView aheadText;
    private TextView aheadLabel;
    private TextView etaText;
    private CircularProgressIndicator graceRing;
    private TextView countdownText;
    private TextView headlineText;
    private TextView instructionText;
    private TextView numberLabel;
    private TextView ticketNumber;
    private MaterialButton leaveButton;
    private MaterialButton primaryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_ticket);
        SystemBars.applyPadding(findViewById(R.id.ticket_root));

        Ticket ticket = ActiveTicketStore.getTicket();

        if (ticket == null) {
            finish();
            return;
        }

        cacheViews();
        bindStaticDetails(ticket);

        ImageButton backButton = findViewById(R.id.ticket_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
     * Rendered here rather than in onCreate so the screen catches up if the
     * grace window ran out while the queuer was somewhere else.
     */
    @Override
    protected void onResume() {
        super.onResume();

        Ticket ticket = ActiveTicketStore.getTicket();
        if (ticket == null) {
            finish();
            return;
        }
        renderState(ticket.getStatus());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The deadline lives in ActiveTicketStore, so stopping the display timer loses nothing.
        cancelGraceTimer();
    }

    private void cacheViews() {
        hero = findViewById(R.id.ticket_hero);
        statusText = findViewById(R.id.ticket_status);
        aheadText = findViewById(R.id.ticket_ahead);
        aheadLabel = findViewById(R.id.ticket_ahead_label);
        etaText = findViewById(R.id.ticket_eta);
        graceRing = findViewById(R.id.ticket_ring);
        countdownText = findViewById(R.id.ticket_countdown);
        headlineText = findViewById(R.id.ticket_headline);
        instructionText = findViewById(R.id.ticket_instruction);
        numberLabel = findViewById(R.id.ticket_number_label);
        ticketNumber = findViewById(R.id.ticket_number);
        leaveButton = findViewById(R.id.ticket_leave_button);
        primaryButton = findViewById(R.id.ticket_primary_button);
    }

    private void bindStaticDetails(Ticket ticket) {
        TextView queueName = findViewById(R.id.ticket_queue_name);
        TextView venue = findViewById(R.id.ticket_venue);

        queueName.setText(ticket.getQueueName());
        venue.setText(ticket.getVenue());

        ticketNumber.setText(getString(R.string.ticket_number_format, ticket.getTicketNumber()));
        etaText.setText(getString(R.string.ticket_eta_format, ticket.getEstimatedWaitMinutes()));

        // Position counts the queuer too, so the people ahead are one fewer.
        int ahead = ticket.getPosition() - 1;
        if (ahead > 0) {
            aheadText.setText(String.valueOf(ahead));
            aheadLabel.setText(R.string.ticket_ahead_label);
        } else {
            aheadText.setText(null);
            aheadLabel.setText(R.string.ticket_next_label);
        }
    }

    private void renderState(Ticket.Status status) {
        cancelGraceTimer();
        hideStateViews();
        aheadText.setOnLongClickListener(null);
        aheadLabel.setOnLongClickListener(null);
        countdownText.setOnLongClickListener(null);

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

    /** Waiting is calm, so it uses the brand container colors rather than a status color. */
    private void renderWaiting() {
        applyHeroColors(
                MaterialColors.getColor(hero, com.google.android.material.R.attr.colorPrimaryContainer),
                MaterialColors.getColor(hero, com.google.android.material.R.attr.colorOnPrimaryContainer));
        statusText.setText(R.string.ticket_status_waiting);

        // An empty count means "you're next", and the label alone says that.
        if (aheadText.getText().length() > 0) {
            aheadText.setVisibility(View.VISIBLE);
        }
        aheadLabel.setVisibility(View.VISIBLE);
        etaText.setVisibility(View.VISIBLE);

        leaveButton.setVisibility(View.VISIBLE);
        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.clearTicket();
                finish();
            }
        });

        // Demo hook: long-press the count to simulate being called.
        View.OnLongClickListener simulateCall = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ActiveTicketStore.markCalled(GRACE_PERIOD_MS);
                renderState(Ticket.Status.CALLED);
                return true;
            }
        };
        aheadText.setOnLongClickListener(simulateCall);
        aheadLabel.setOnLongClickListener(simulateCall);
    }

    private void renderCalled() {
        applyStatusColors(R.color.status_called);
        statusText.setText(R.string.ticket_status_called);

        graceRing.setVisibility(View.VISIBLE);
        countdownText.setVisibility(View.VISIBLE);
        instructionText.setVisibility(View.VISIBLE);
        instructionText.setText(R.string.ticket_called_instruction);

        primaryButton.setVisibility(View.VISIBLE);
        primaryButton.setText(R.string.ticket_here_action);
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.markServed();
                renderState(Ticket.Status.SERVED);
            }
        });

        // Demo hook: long-press the countdown to skip to the end of the grace period,
        // so the no-show and cooldown flow can be shown without waiting 3 minutes.
        countdownText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                expire();
                return true;
            }
        });

        startGraceTimer(ActiveTicketStore.graceRemainingMs());
    }

    private void renderServed() {
        applyStatusColors(R.color.status_served);
        statusText.setText(R.string.ticket_status_served);

        headlineText.setVisibility(View.VISIBLE);
        headlineText.setText(R.string.ticket_served_headline);
        instructionText.setVisibility(View.VISIBLE);
        instructionText.setText(R.string.ticket_served_instruction);

        showDoneButton();
    }

    private void renderExpired() {
        applyStatusColors(R.color.status_expired);
        statusText.setText(R.string.ticket_status_expired);

        headlineText.setVisibility(View.VISIBLE);
        headlineText.setText(R.string.ticket_expired_headline);
        instructionText.setVisibility(View.VISIBLE);
        instructionText.setText(expiredInstruction());

        showDoneButton();
    }

    /** The expired message depends on whether this no-show counted toward a cooldown. */
    private String expiredInstruction() {
        Ticket ticket = ActiveTicketStore.getTicket();
        Queue queue = ticket == null ? null : FakeData.queueById(ticket.getQueueId());

        if (queue == null || !queue.isNoShowPenaltyEnabled()) {
            return getString(R.string.ticket_expired_instruction);
        }
        if (Cooldown.isActive()) {
            return getString(R.string.ticket_expired_cooldown_instruction,
                    Cooldown.remainingMinutes());
        }
        return getString(R.string.ticket_expired_warning_instruction, Cooldown.DURATION_MINUTES);
    }

    private void showDoneButton() {
        primaryButton.setVisibility(View.VISIBLE);
        primaryButton.setText(R.string.ticket_done_action);
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.finishTicket();
                finish();
            }
        });
    }

    private void expire() {
        ActiveTicketStore.markNoShow();
        renderState(Ticket.Status.NO_SHOW);
    }

    /** Each render starts with nothing state-specific visible, then shows what it needs. */
    private void hideStateViews() {
        aheadText.setVisibility(View.GONE);
        aheadLabel.setVisibility(View.GONE);
        etaText.setVisibility(View.GONE);
        graceRing.setVisibility(View.GONE);
        countdownText.setVisibility(View.GONE);
        headlineText.setVisibility(View.GONE);
        instructionText.setVisibility(View.GONE);
        leaveButton.setVisibility(View.GONE);
        primaryButton.setVisibility(View.GONE);
    }

    /** Called, served and expired fill the panel with their status color and white text. */
    private void applyStatusColors(int colorRes) {
        applyHeroColors(ContextCompat.getColor(this, colorRes), Color.WHITE);
    }

    private void applyHeroColors(int containerColor, int contentColor) {
        hero.setBackgroundTintList(ColorStateList.valueOf(containerColor));

        TextView[] texts = {statusText, aheadText, aheadLabel, etaText, countdownText,
                headlineText, instructionText, numberLabel, ticketNumber};
        for (TextView text : texts) {
            text.setTextColor(contentColor);
        }

        graceRing.setIndicatorColor(contentColor);
        graceRing.setTrackColor(ColorUtils.setAlphaComponent(contentColor, RING_TRACK_ALPHA));
    }

    private void startGraceTimer(long remainingMs) {
        graceTimer = new CountDownTimer(remainingMs, COUNTDOWN_TICK_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000L;
                long minutes = totalSeconds / 60L;
                long seconds = totalSeconds % 60L;

                countdownText.setText(String.format(Locale.US,
                        getString(R.string.ticket_countdown_format), minutes, seconds));

                // The ring drains from full to empty across the whole grace period.
                graceRing.setProgressCompat(
                        (int) (millisUntilFinished * graceRing.getMax() / GRACE_PERIOD_MS), true);
            }

            @Override
            public void onFinish() {
                expire();
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
}

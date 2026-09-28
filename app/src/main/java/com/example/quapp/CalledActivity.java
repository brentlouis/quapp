package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;

/**
 * You're being called (canvas 09). Its own Activity because the whole screen changes theme
 * (Theme.Quapp.Called in the manifest): espresso ground, light status bar icons, marigold number.
 *
 * The grace period (the anti-prank check) runs here: confirm with "I'm here" before the timer
 * ends, or the slot is released. Either way this screen hands back to ActiveTicketActivity,
 * which shows the outcome.
 */
public class CalledActivity extends AppCompatActivity {

    public static final long GRACE_PERIOD_MS = 180_000L;
    private static final long COUNTDOWN_TICK_MS = 1_000L;

    private CountDownTimer graceTimer;
    private TextView countdown;
    private CircularProgressIndicator ring;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_called);
        SystemBars.applyPadding(findViewById(R.id.called_root));

        countdown = findViewById(R.id.called_countdown);
        ring = findViewById(R.id.called_ring);

        findViewById(R.id.called_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.called_here_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.markServed();
                showOutcome();
            }
        });

        // Demo hook: long-press the countdown to skip to the end of the grace period, so the
        // slot-released flow can be shown without waiting 3 minutes.
        countdown.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                expire();
                return true;
            }
        });
    }

    /** onResume so the timer picks up the real time left after the screen was away. */
    @Override
    protected void onResume() {
        super.onResume();

        Ticket ticket = ActiveTicketStore.getTicket();
        if (ticket == null || ticket.getStatus() != Ticket.Status.CALLED) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.called_queue_name)).setText(ticket.getQueueName());
        ((TextView) findViewById(R.id.called_venue)).setText(ticket.getVenue());
        ((TextView) findViewById(R.id.called_number)).setText(
                getString(R.string.ticket_number_format, ticket.getTicketNumber()));

        startGraceTimer(ActiveTicketStore.graceRemainingMs());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The deadline lives in ActiveTicketStore, so stopping the display timer loses nothing.
        cancelGraceTimer();
    }

    private void startGraceTimer(long remainingMs) {
        cancelGraceTimer();
        graceTimer = new CountDownTimer(remainingMs, COUNTDOWN_TICK_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000L;
                countdown.setText(String.format(Locale.US, "%d:%02d",
                        totalSeconds / 60L, totalSeconds % 60L));
                // The ring drains from full to empty across the whole grace period.
                ring.setProgressCompat(
                        (int) (millisUntilFinished * ring.getMax() / GRACE_PERIOD_MS), true);
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

    private void expire() {
        cancelGraceTimer();
        ActiveTicketStore.markNoShow();
        showOutcome();
    }

    /** Back to the ticket screen, which now shows Served or Slot released. */
    private void showOutcome() {
        startActivity(new Intent(this, ActiveTicketActivity.class));
        finish();
    }
}

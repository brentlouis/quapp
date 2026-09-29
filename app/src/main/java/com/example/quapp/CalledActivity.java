package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

/**
 * You're being called (canvas 09). Its own Activity because the whole screen changes theme
 * (Theme.Quapp.Called in the manifest): espresso ground, light status bar icons, marigold number.
 *
 * The grace period (the anti-prank check) runs here: confirm with "I'm here" before the timer
 * ends, or the slot is released. "Move me back" opens the I-need-more-time sheet instead, which
 * hands the slot back without a no-show. Every way out goes back to ActiveTicketActivity, which
 * shows where the ticket ended up.
 */
public class CalledActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "com.example.quapp.EXTRA_CALLED_TICKET_ID";
    public static final long GRACE_PERIOD_MS = 180_000L;
    private static final long COUNTDOWN_TICK_MS = 1_000L;

    private String ticketId;
    private CountDownTimer graceTimer;
    private TextView countdown;
    private CircularProgressIndicator ring;

    public static Intent intent(Context context, String ticketId) {
        Intent intent = new Intent(context, CalledActivity.class);
        intent.putExtra(EXTRA_TICKET_ID, ticketId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_called);
        SystemBars.applyPadding(findViewById(R.id.called_root));

        ticketId = getIntent().getStringExtra(EXTRA_TICKET_ID);
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
                confirmHere(view);
            }
        });

        findViewById(R.id.called_move_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreTimeSheet.show(CalledActivity.this, ticketId, new MoreTimeSheet.OnMovedListener() {
                    @Override
                    public void onMoved(Ticket moved, int places) {
                        showOutcome();
                    }
                });
            }
        });
    }

    /** onResume so the timer picks up the real time left after the screen was away. */
    @Override
    protected void onResume() {
        super.onResume();

        Ticket ticket = ActiveTicketStore.ticket(ticketId);
        if (ticket == null || ticket.getStatus() != Ticket.Status.CALLED) {
            // Released or served while away: the ticket screen shows how it ended.
            if (ticket != null) {
                showOutcome();
            } else {
                finish();
            }
            return;
        }

        ((TextView) findViewById(R.id.called_queue_name)).setText(ticket.getQueueName());
        ((TextView) findViewById(R.id.called_venue)).setText(ticket.getVenue());
        ((TextView) findViewById(R.id.called_number)).setText(
                getString(R.string.ticket_number_format, ticket.getTicketNumber()));

        // Moving back is once per ticket.
        boolean canMove = !ticket.isMovedBack();
        findViewById(R.id.called_move_back).setVisibility(canMove ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.called_move_back_hint)).setText(canMove
                ? R.string.called_move_back_hint : R.string.called_moved_back_hint);

        // A queue without a grace period has no deadline: nothing to count down.
        Queue queue = Queues.get(ticket.getQueueId());
        boolean grace = queue == null || queue.isGracePeriodEnabled();
        findViewById(R.id.called_timer).setVisibility(grace ? View.VISIBLE : View.GONE);
        if (grace) {
            startGraceTimer(ActiveTicketStore.graceRemainingMs(ticket));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The deadline is the ticket's calledAt + 3 min, so stopping the display timer loses nothing.
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

    /** The server releases the slot at the deadline; the store already shows it as a no-show. */
    private void expire() {
        cancelGraceTimer();
        showOutcome();
    }

    /** Tell the server; the ticket screen then shows Served. */
    private void confirmHere(final View button) {
        button.setEnabled(false);
        ActiveTicketStore.here(this, ticketId, new ActiveTicketStore.Done<Ticket>() {
            @Override
            public void onDone(@Nullable Ticket ticket) {
                showOutcome();
            }

            @Override
            public void onFailed(@NonNull ApiError error) {
                button.setEnabled(true);
                Snackbar.make(button, error.is(ApiError.OFFLINE)
                        ? getString(R.string.api_offline) : error.message,
                        Snackbar.LENGTH_LONG).show();
                // Maybe the deadline passed or the organizer already served it: find out.
                ActiveTicketStore.sync(CalledActivity.this);
            }
        });
    }

    /** Back to the ticket screen, which shows Served, Slot released or the moved-back ticket. */
    private void showOutcome() {
        startActivity(ActiveTicketActivity.intent(this, ticketId));
        finish();
    }
}

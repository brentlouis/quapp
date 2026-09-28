package com.example.quapp;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;
import java.util.Locale;

/**
 * My ticket (canvas 08) while you wait, and the outcome once you're served or your slot is
 * released. Being called is its own screen, CalledActivity: the whole screen turns espresso,
 * which is a different Activity theme (Theme.Quapp.Called).
 */
public class ActiveTicketActivity extends AppCompatActivity {

    /** "Be there by" is the estimated call time minus this (DECISIONS.md "Arrival info"). */
    private static final int ARRIVAL_BUFFER_MINUTES = 10;

    private TextView statusPill;
    private View ticketCard;
    private View ticketRows;
    private View outcome;
    private View leaveButton;
    private View doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_ticket);
        SystemBars.applyPadding(findViewById(R.id.ticket_root));

        statusPill = findViewById(R.id.ticket_status);
        ticketCard = findViewById(R.id.ticket_card);
        ticketRows = findViewById(R.id.ticket_rows);
        outcome = findViewById(R.id.ticket_outcome);
        leaveButton = findViewById(R.id.ticket_leave_button);
        doneButton = findViewById(R.id.ticket_done_button);

        findViewById(R.id.ticket_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmLeave();
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.finishTicket();
                finish();
            }
        });

        findViewById(R.id.ticket_directions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDirections();
            }
        });

        // Demo hook until the server pushes it: long-press the ticket to simulate being called.
        ticketCard.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ActiveTicketStore.markCalled(CalledActivity.GRACE_PERIOD_MS);
                openCalled();
                return true;
            }
        });

        // The stub's bottom punch is placed from the right edge, so the shape needs the width.
        ticketCard.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left != oldRight - oldLeft) {
                    applyTicketShape(v, right - left);
                }
            }
        });
    }

    /**
     * Rendered in onResume, not onCreate, so the screen catches up with whatever happened while
     * the queuer was on another screen (called, served, slot released).
     */
    @Override
    protected void onResume() {
        super.onResume();

        Ticket ticket = ActiveTicketStore.getTicket();
        if (ticket == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.ticket_queue_name)).setText(ticket.getQueueName());
        ((TextView) findViewById(R.id.ticket_venue)).setText(ticket.getVenue());

        switch (ticket.getStatus()) {
            case WAITING:
                renderWaiting(ticket);
                break;
            case CALLED:
                openCalled();
                break;
            case SERVED:
                renderOutcome(R.string.ticket_status_served, R.color.ok, R.color.ok_soft,
                        getString(R.string.ticket_served_headline),
                        getString(R.string.ticket_served_instruction));
                break;
            case NO_SHOW:
                renderOutcome(R.string.ticket_status_expired, R.color.err, R.color.err_soft,
                        getString(R.string.ticket_expired_headline), expiredInstruction());
                break;
        }
    }

    /** Called replaces this screen, so Back from Called goes to where you came from. */
    private void openCalled() {
        startActivity(new Intent(this, CalledActivity.class));
        finish();
    }

    // ---- Waiting ------------------------------------------------------------

    private void renderWaiting(Ticket ticket) {
        setPill(R.string.ticket_status_waiting, R.color.ink_muted, R.color.paper_sunk);
        showWaiting(true);

        ((TextView) findViewById(R.id.ticket_number)).setText(
                getString(R.string.ticket_number_format, ticket.getTicketNumber()));

        // Position counts the queuer too, so the people ahead are one fewer.
        int ahead = ticket.getPosition() - 1;
        TextView aheadText = findViewById(R.id.ticket_ahead);
        TextView aheadLabel = findViewById(R.id.ticket_ahead_label);
        if (ahead > 0) {
            aheadText.setVisibility(View.VISIBLE);
            aheadText.setText(String.valueOf(ahead));
            aheadLabel.setText(R.string.ticket_ahead_label);
        } else {
            aheadText.setVisibility(View.GONE);
            aheadLabel.setText(R.string.ticket_next_label);
        }

        // Ticks: how far the numbers being served have come towards yours.
        Ticket serving = FakeData.nowServing(ticket.getQueueId());
        ProgressTicksView ticks = findViewById(R.id.ticket_ticks);
        ticks.setProgress(serving == null ? 0f
                : serving.getTicketNumber() / (float) ticket.getTicketNumber());
        ((TextView) findViewById(R.id.ticket_now_serving)).setText(serving == null
                ? getString(R.string.ticket_now_serving_none)
                : getString(R.string.ticket_now_serving_format, serving.getTicketNumber()));

        ticketCard.setContentDescription(getString(R.string.ticket_description,
                ticket.getTicketNumber(), aheadText.getVisibility() == View.VISIBLE
                        ? ahead + " " + getString(R.string.ticket_ahead_label)
                        : getString(R.string.ticket_next_label)));

        bindEtaRow(ticket);
        bindArrival(ticket);
    }

    /** "About 55 min", and what that estimate is based on. */
    private void bindEtaRow(Ticket ticket) {
        QueueStats stats = FakeData.stats(ticket.getQueueId());
        // Locale.US keeps the decimal point a point, matching the rest of the numbers.
        String basis = stats.getServiceSampleCount() == 0
                ? getString(R.string.ticket_eta_basis_none)
                : String.format(Locale.US, getString(R.string.ticket_eta_basis_format),
                        stats.getAverageServiceMinutes(), stats.getServiceSampleCount());
        ListRow.bind(findViewById(R.id.ticket_eta_row), R.drawable.ic_clock,
                getString(R.string.ticket_eta_format, ticket.getEstimatedWaitMinutes()), basis);
        findViewById(R.id.ticket_eta_row).findViewById(R.id.row_chevron).setVisibility(View.GONE);
    }

    /** "Be there by": the estimated call time minus a buffer, in the phone's 12/24h format. */
    private void bindArrival(Ticket ticket) {
        int minutes = Math.max(0, ticket.getEstimatedWaitMinutes() - ARRIVAL_BUFFER_MINUTES);
        Date beThereBy = new Date(System.currentTimeMillis() + minutes * 60_000L);
        ((TextView) findViewById(R.id.ticket_be_there)).setText(getString(
                R.string.ticket_be_there_format, DateFormat.getTimeFormat(this).format(beThereBy)));
        ((TextView) findViewById(R.id.ticket_arrival_venue)).setText(ticket.getVenue());
    }

    /**
     * Opens the venue in whatever maps app is installed: a geo: link with the venue's
     * coordinates and its name as the label. No Maps SDK needed.
     */
    private void openDirections() {
        Ticket ticket = ActiveTicketStore.getTicket();
        Queue queue = ticket == null ? null : FakeData.queueById(ticket.getQueueId());
        if (queue == null) {
            return;
        }
        Uri uri = Uri.parse(String.format(Locale.US, "geo:0,0?q=%f,%f(%s)",
                queue.getLatitude(), queue.getLongitude(), Uri.encode(queue.getVenue())));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(ticketCard, R.string.ticket_no_maps_app, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Leaving gives up the number for good, so it asks first (canvas 26). */
    private void confirmLeave() {
        Ticket ticket = ActiveTicketStore.getTicket();
        if (ticket == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ticket_leave_title)
                .setMessage(getString(R.string.ticket_leave_message,
                        ticket.getTicketNumber(), ticket.getQueueName()))
                .setNegativeButton(R.string.ticket_leave_cancel, null)
                .setPositiveButton(R.string.ticket_leave_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActiveTicketStore.clearTicket();
                                finish();
                            }
                        })
                .show();
    }

    // ---- Served / slot released ---------------------------------------------

    private void renderOutcome(int pillText, @ColorRes int pillColor, @ColorRes int pillGround,
                               String headline, String body) {
        setPill(pillText, pillColor, pillGround);
        showWaiting(false);
        ((TextView) findViewById(R.id.ticket_outcome_headline)).setText(headline);
        ((TextView) findViewById(R.id.ticket_outcome_body)).setText(body);
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

    // ---- Shared -------------------------------------------------------------

    private void showWaiting(boolean waiting) {
        ticketCard.setVisibility(waiting ? View.VISIBLE : View.GONE);
        ticketRows.setVisibility(waiting ? View.VISIBLE : View.GONE);
        leaveButton.setVisibility(waiting ? View.VISIBLE : View.GONE);
        outcome.setVisibility(waiting ? View.GONE : View.VISIBLE);
        doneButton.setVisibility(waiting ? View.GONE : View.VISIBLE);
    }

    private void setPill(int text, @ColorRes int color, @ColorRes int ground) {
        statusPill.setText(text);
        statusPill.setTextColor(ContextCompat.getColor(this, color));
        ViewCompat.setBackgroundTintList(statusPill,
                ColorStateList.valueOf(ContextCompat.getColor(this, ground)));
    }

    /** Espresso ticket with its holes top and bottom, centred on the tear line. */
    private void applyTicketShape(View v, int width) {
        // The tear line is 2dp wide and starts at the stub's edge, so its centre is 1dp further in.
        float tearCentre = getResources().getDimension(R.dimen.ticket_stub_width)
                + getResources().getDimension(R.dimen.hairline);
        ShapeAppearanceModel shape = TicketShapes.stub(this,
                R.dimen.radius_md, R.dimen.punch_radius, tearCentre, width);
        v.setBackground(TicketShapes.background(shape,
                ContextCompat.getColor(this, R.color.spotlight)));
    }
}

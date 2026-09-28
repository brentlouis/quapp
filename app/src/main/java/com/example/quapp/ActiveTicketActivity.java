package com.example.quapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
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
    private View doneOutlinedButton;
    private View joinAgainButton;
    private View browseButton;

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
        doneOutlinedButton = findViewById(R.id.ticket_done_outlined_button);
        joinAgainButton = findViewById(R.id.ticket_join_again_button);
        browseButton = findViewById(R.id.ticket_browse_button);
        browseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.finishTicket();
                startActivity(QueuerHomeActivity.intent(ActiveTicketActivity.this,
                        QueuerHomeActivity.TAB_BROWSE));
                finish();
            }
        });

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

        View.OnClickListener done = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.finishTicket();
                finish();
            }
        };
        doneButton.setOnClickListener(done);
        doneOutlinedButton.setOnClickListener(done);

        // Slot released: file the ticket, then open the same queue so you can join again.
        joinAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Ticket ticket = ActiveTicketStore.getTicket();
                ActiveTicketStore.finishTicket();
                if (ticket != null) {
                    Intent intent = new Intent(ActiveTicketActivity.this, QueueDetailActivity.class);
                    intent.putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, ticket.getQueueId());
                    startActivity(intent);
                }
                finish();
            }
        });

        // The kept ticket's punches sit halfway across, so its shape also needs the width.
        findViewById(R.id.ticket_kept).addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left != oldRight - oldLeft) {
                    v.setBackground(TicketShapes.keptTicketBackground(
                            ActiveTicketActivity.this, right - left));
                }
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
                renderServed(ticket);
                break;
            case NO_SHOW:
                renderReleased(ticket);
                break;
            case QUEUE_CLOSED:
                renderEnded(ticket, R.string.ticket_status_queue_closed,
                        R.string.ticket_closed_headline, R.string.ticket_closed_instruction);
                break;
            case REMOVED:
                renderEnded(ticket, R.string.ticket_status_removed,
                        R.string.ticket_removed_headline, R.string.ticket_removed_instruction);
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
        bindBring(ticket);
    }

    /** "Bring · Barangay ID · claim stub", only when the organizer filled it in. */
    private void bindBring(Ticket ticket) {
        Queue queue = FakeData.queueById(ticket.getQueueId());
        String bring = queue == null ? null : queue.getBring();
        View row = findViewById(R.id.ticket_bring_row);
        int visibility = bring == null ? View.GONE : View.VISIBLE;
        row.setVisibility(visibility);
        findViewById(R.id.ticket_bring_divider).setVisibility(visibility);
        if (bring != null) {
            ListRow.bind(row, R.drawable.ic_category_relief, getString(R.string.ticket_bring_title), bring);
            row.findViewById(R.id.row_chevron).setVisibility(View.GONE);
        }
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

    private void openDirections() {
        Ticket ticket = ActiveTicketStore.getTicket();
        Directions.open(this, ticket == null ? null : FakeData.queueById(ticket.getQueueId()));
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

    /** Served (canvas 22): a check, then the kept ticket stamped SERVED. */
    private void renderServed(Ticket ticket) {
        showOutcome(ticket, R.string.ticket_status_served);
        findViewById(R.id.ticket_outcome_tile).setVisibility(View.VISIBLE);
        setText(R.id.ticket_outcome_headline, getString(R.string.ticket_served_headline));
        setText(R.id.ticket_outcome_body, getString(R.string.ticket_served_instruction));

        TextView number = keptNumber(ticket, false);
        number.setTextColor(ContextCompat.getColor(this, R.color.ink));
        setText(R.id.ticket_kept_right_label, getString(R.string.ticket_outcome_status_label));
        stamp(R.id.ticket_kept_stamp_right, R.string.ticket_status_served, R.color.ok);
        findViewById(R.id.ticket_kept_stamp_left).setVisibility(View.GONE);
        findViewById(R.id.ticket_kept_no_shows).setVisibility(View.GONE);
        findViewById(R.id.ticket_outcome_note).setVisibility(View.GONE);

        doneButton.setVisibility(View.VISIBLE);
    }

    /**
     * Slot released (canvas 23): your number struck through and stamped. When the queue counts
     * no-shows, the other half shows how close you are to a cooldown; otherwise it carries the
     * stamp.
     */
    private void renderReleased(Ticket ticket) {
        showOutcome(ticket, R.string.ticket_status_expired);
        findViewById(R.id.ticket_outcome_tile).setVisibility(View.GONE);
        setText(R.id.ticket_outcome_headline, getString(R.string.ticket_expired_headline));
        setText(R.id.ticket_outcome_body, getString(R.string.ticket_expired_instruction));

        keptNumber(ticket, true).setTextColor(ContextCompat.getColor(this, R.color.ink_faint));

        Queue queue = FakeData.queueById(ticket.getQueueId());
        boolean counts = queue != null && queue.isNoShowCooldownEnabled();
        TextView noShows = findViewById(R.id.ticket_kept_no_shows);
        if (counts) {
            stamp(R.id.ticket_kept_stamp_left, R.string.ticket_status_expired, R.color.err);
            findViewById(R.id.ticket_kept_stamp_right).setVisibility(View.GONE);
            setText(R.id.ticket_kept_right_label, getString(R.string.ticket_outcome_no_shows_label));
            // During a cooldown the count has already hit the limit.
            int count = Cooldown.isActive() ? Cooldown.NO_SHOW_LIMIT
                    : Cooldown.NO_SHOW_LIMIT - Cooldown.noShowsUntilCooldown();
            noShows.setText(getString(R.string.ticket_outcome_no_shows_format,
                    count, Cooldown.NO_SHOW_LIMIT));
            noShows.setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.ticket_kept_stamp_left).setVisibility(View.GONE);
            setText(R.id.ticket_kept_right_label, getString(R.string.ticket_outcome_status_label));
            stamp(R.id.ticket_kept_stamp_right, R.string.ticket_status_expired, R.color.err);
            noShows.setVisibility(View.GONE);
        }

        findViewById(R.id.ticket_outcome_note).setVisibility(View.VISIBLE);
        setText(R.id.ticket_outcome_note_text, expiredNote(counts));

        doneOutlinedButton.setVisibility(View.VISIBLE);
        joinAgainButton.setVisibility(View.VISIBLE);
    }

    /**
     * Queue closed (canvas 24) or removed: not your fault in the first case, so the kept ticket
     * says the no-show record isn't affected. One way on: browse other queues.
     */
    private void renderEnded(Ticket ticket, int title, int headline, int body) {
        showOutcome(ticket, title);
        findViewById(R.id.ticket_outcome_tile).setVisibility(View.GONE);
        setText(R.id.ticket_outcome_headline, getString(headline));
        setText(R.id.ticket_outcome_body, getString(body));

        keptNumber(ticket, false).setTextColor(ContextCompat.getColor(this, R.color.ink_muted));
        findViewById(R.id.ticket_kept_stamp_right).setVisibility(View.GONE);
        findViewById(R.id.ticket_outcome_note).setVisibility(View.GONE);

        boolean closed = ticket.getStatus() == Ticket.Status.QUEUE_CLOSED;
        stamp(R.id.ticket_kept_stamp_left, title, closed ? R.color.ink_muted : R.color.err);
        TextView record = findViewById(R.id.ticket_kept_no_shows);
        if (closed) {
            setText(R.id.ticket_kept_right_label, getString(R.string.ticket_outcome_record_label));
            record.setText(R.string.ticket_outcome_not_affected);
            record.setVisibility(View.VISIBLE);
        } else {
            setText(R.id.ticket_kept_right_label, getString(R.string.ticket_outcome_status_label));
            record.setVisibility(View.GONE);
        }

        browseButton.setVisibility(View.VISIBLE);
    }

    /** What the no-show means for you next time. */
    private String expiredNote(boolean countsTowardCooldown) {
        if (!countsTowardCooldown) {
            return getString(R.string.ticket_expired_note);
        }
        if (Cooldown.isActive()) {
            return getString(R.string.ticket_expired_cooldown_instruction,
                    Cooldown.remainingMinutes());
        }
        return getString(R.string.ticket_expired_warning_instruction, Cooldown.DURATION_MINUTES);
    }

    /** The outcome's app bar reads "Served" over "Queue · venue"; no pill, the stamp says it. */
    private void showOutcome(Ticket ticket, int title) {
        showWaiting(false);
        statusPill.setVisibility(View.GONE);
        setText(R.id.ticket_queue_name, getString(title));
        setText(R.id.ticket_venue, getString(R.string.ticket_outcome_subtitle_format,
                ticket.getQueueName(), ticket.getVenue()));
    }

    private TextView keptNumber(Ticket ticket, boolean struck) {
        TextView number = findViewById(R.id.ticket_kept_number);
        number.setText(getString(R.string.ticket_number_format, ticket.getTicketNumber()));
        // Paint flags are bits; | turns strike-through on, & ~ turns it off.
        int flags = number.getPaintFlags();
        number.setPaintFlags(struck ? flags | Paint.STRIKE_THRU_TEXT_FLAG
                : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        return number;
    }

    /** A rubber stamp: its text and double rule both take the status colour. */
    private void stamp(int viewId, int text, @ColorRes int color) {
        TextView stamp = findViewById(viewId);
        int c = ContextCompat.getColor(this, color);
        stamp.setVisibility(View.VISIBLE);
        stamp.setText(text);
        stamp.setTextColor(c);
        stamp.setBackground(TicketShapes.stampBackground(this, c));
    }

    private void setText(int viewId, CharSequence text) {
        ((TextView) findViewById(viewId)).setText(text);
    }

    // ---- Shared -------------------------------------------------------------

    /** Switches between the waiting ticket and an outcome; each outcome then shows its buttons. */
    private void showWaiting(boolean waiting) {
        ticketCard.setVisibility(waiting ? View.VISIBLE : View.GONE);
        ticketRows.setVisibility(waiting ? View.VISIBLE : View.GONE);
        leaveButton.setVisibility(waiting ? View.VISIBLE : View.GONE);
        statusPill.setVisibility(waiting ? View.VISIBLE : View.GONE);
        outcome.setVisibility(waiting ? View.GONE : View.VISIBLE);
        doneButton.setVisibility(View.GONE);
        doneOutlinedButton.setVisibility(View.GONE);
        joinAgainButton.setVisibility(View.GONE);
        browseButton.setVisibility(View.GONE);
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

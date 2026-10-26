package com.example.quapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.snackbar.Snackbar;

import java.time.Instant;
import java.util.Date;
import java.util.Locale;

public class ActiveTicketActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "com.example.quapp.EXTRA_TICKET_ID";

    private String ticketId;
    private TextView statusPill;
    private View ticketCard;
    private View ticketRows;
    private View outcome;
    private View leaveButton;
    private MaterialButton moreTimeButton;
    private View doneButton;
    private View doneOutlinedButton;
    private View joinAgainButton;
    private View browseButton;

    private Connectivity connectivity;
    private Ticket lastSeen;
    private Instant lastSeenAt;
    private LoadingStateHelper loadingStateHelper;
    private boolean isLeaving;
    /** The ticket wasn't in the store and a sync was asked for; see render(). */
    private boolean askedServer;

    /** Redraw when a sync brings news: moved up the line, called, served, released. */
    private final Runnable ticketsChanged = new Runnable() {
        @Override
        public void run() {
            render();
        }
    };

    public static Intent intent(Context context, String ticketId) {
        Intent intent = new Intent(context, ActiveTicketActivity.class);
        intent.putExtra(EXTRA_TICKET_ID, ticketId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_ticket);
        SystemBars.applyPadding(findViewById(R.id.ticket_root));

        ticketId = getIntent().getStringExtra(EXTRA_TICKET_ID);

        statusPill = findViewById(R.id.ticket_status);
        ticketCard = findViewById(R.id.ticket_card);
        ticketRows = findViewById(R.id.ticket_rows);
        outcome = findViewById(R.id.ticket_outcome);
        leaveButton = findViewById(R.id.ticket_leave_button);
        moreTimeButton = findViewById(R.id.ticket_more_time_button);
        doneButton = findViewById(R.id.ticket_done_button);
        doneOutlinedButton = findViewById(R.id.ticket_done_outlined_button);
        joinAgainButton = findViewById(R.id.ticket_join_again_button);
        browseButton = findViewById(R.id.ticket_browse_button);
        browseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.finishTicket(ticketId);
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

        loadingStateHelper = new LoadingStateHelper(
                null,
                null,
                findViewById(R.id.ticket_loading),
                findViewById(R.id.ticket_error)
        );
        loadingStateHelper.setOnRetryListener(new LoadingStateHelper.OnRetryListener() {
            @Override
            public void onRetry() {
                render();
            }
        });

        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmLeave();
            }
        });

        moreTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreTimeSheet.show(ActiveTicketActivity.this, ticketId,
                        new MoreTimeSheet.OnMovedListener() {
                            @Override
                            public void onMoved(Ticket moved, int places) {
                                render();
                                QuappMotion.snackbar(ticketCard, getResources().getQuantityString(
                                        R.plurals.more_time_moved, places, places,
                                        moved.getTicketNumber()), Snackbar.LENGTH_LONG)
                                        .setAnchorView(R.id.ticket_tear)
                                        .show();
                            }
                        });
            }
        });

        View.OnClickListener done = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActiveTicketStore.finishTicket(ticketId);
                finish();
            }
        };
        doneButton.setOnClickListener(done);
        doneOutlinedButton.setOnClickListener(done);

        joinAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Ticket ticket = ActiveTicketStore.ticket(ticketId);
                ActiveTicketStore.finishTicket(ticketId);
                if (ticket != null) {
                    Intent intent = new Intent(ActiveTicketActivity.this, QueueDetailActivity.class);
                    intent.putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, ticket.getQueueId());
                    startActivity(intent);
                }
                finish();
            }
        });

        connectivity = new Connectivity(this);
        findViewById(R.id.ticket_offline_retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectivity.isOnline()) {
                    render();
                } else {
                    QuappMotion.snackbar(ticketCard, R.string.ticket_offline_still, Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.ticket_tear)
                            .show();
                }
            }
        });

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

        findViewById(R.id.ticket_report).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Ticket ticket = ActiveTicketStore.ticket(ticketId);
                if (ticket != null) {
                    startActivity(ReportQueueActivity.intent(ActiveTicketActivity.this,
                            ticket.getQueueId()));
                }
            }
        });

        findViewById(R.id.ticket_directions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDirections();
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

    @Override
    protected void onResume() {
        super.onResume();
        ActiveTicketStore.addListener(ticketsChanged);
        render();
        connectivity.start(new Connectivity.Listener() {
            @Override
            public void onConnectivityChanged(boolean online) {
                render();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        ActiveTicketStore.removeListener(ticketsChanged);
        connectivity.stop();
    }

    /**
     * The store keeps the tickets in step with the server, so this draws from it straight away.
     * A ticket it doesn't have yet (opened from a notification right after the app started):
     * loading, one sync, and if it's still not there it was left or dismissed, so the screen closes.
     */
    private void render() {
        if (isLeaving) {
            return;
        }
        if (ActiveTicketStore.ticket(ticketId) != null) {
            loadingStateHelper.showContent();
            renderInternal();
            return;
        }
        if (askedServer) {
            finish();
            return;
        }
        askedServer = true;
        loadingStateHelper.showLoading(getString(R.string.loading_ticket_details));
        ActiveTicketStore.sync(this);  // ticketsChanged calls render again when it's in
    }

    private void renderInternal() {
        boolean online = connectivity.isOnline();
        Ticket ticket = ActiveTicketStore.ticket(ticketId);
        if (ticket == null) {
            finish();
            return;
        }
        if (online || lastSeen == null) {
            lastSeen = ticket;
            lastSeenAt = Instant.now();
        } else if (ticket.getStatus() == Ticket.Status.WAITING) {
            ticket = lastSeen;
        }

        ((TextView) findViewById(R.id.ticket_queue_name)).setText(ticket.getQueueName());
        ((TextView) findViewById(R.id.ticket_venue)).setText(ticket.getVenue());

        switch (ticket.getStatus()) {
            case WAITING:
                renderWaiting(ticket);
                bindOffline(ticket, online);
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
                        R.string.ticket_removed_headline, removedReason(ticket));
                break;
        }
    }

    private void openCalled() {
        startActivity(CalledActivity.intent(ActiveTicketActivity.this, ticketId));
        finish();
    }

    private void renderWaiting(Ticket ticket) {
        setPill(R.string.ticket_status_waiting, R.color.ink_muted, R.color.paper_sunk);
        showWaiting(true);

        ((TextView) findViewById(R.id.ticket_number)).setText(
                getString(R.string.ticket_number_format, ticket.getTicketNumber()));

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

        // Ticks: how far the line has come towards you. Counted in people, not ticket numbers,
        // because a moved-back ticket keeps its number but not its place.
        Queue queue = Queues.get(ticket.getQueueId());
        Integer serving = queue == null ? null : queue.getNowServing();
        ProgressTicksView ticks = findViewById(R.id.ticket_ticks);
        ticks.setProgress(serving == null ? 0f : serving / (float) (serving + ahead + 1));
        ((TextView) findViewById(R.id.ticket_now_serving)).setText(serving == null
                ? getString(R.string.ticket_now_serving_none)
                : getString(R.string.ticket_now_serving_format, serving));

        ticketCard.setContentDescription(getString(R.string.ticket_description,
                ticket.getTicketNumber(), aheadText.getVisibility() == View.VISIBLE
                        ? ahead + " " + getString(R.string.ticket_ahead_label)
                        : getString(R.string.ticket_next_label)));

        bindEtaRow(ticket);
        bindArrival(ticket);
        bindBring(ticket);
        bindMoreTime(ticket);
    }

    private void bindOffline(Ticket ticket, boolean online) {
        int offline = online ? View.GONE : View.VISIBLE;
        findViewById(R.id.ticket_offline).setVisibility(offline);
        findViewById(R.id.ticket_as_of).setVisibility(offline);
        findViewById(R.id.ticket_offline_note).setVisibility(offline);
        findViewById(R.id.ticket_ticks).setVisibility(online ? View.VISIBLE : View.GONE);
        leaveButton.setEnabled(online);
        if (online) {
            return;
        }
        moreTimeButton.setEnabled(false);

        long minutes = (System.currentTimeMillis() - lastSeenAt.toEpochMilli()) / 60_000L;
        ((TextView) findViewById(R.id.ticket_offline_updated)).setText(minutes < 1
                ? getString(R.string.ticket_offline_updated_now)
                : getResources().getQuantityString(R.plurals.ticket_offline_updated,
                        (int) minutes, (int) minutes));
        ((TextView) findViewById(R.id.ticket_as_of)).setText(getString(
                R.string.ticket_offline_as_of, Format.time(ActiveTicketActivity.this, lastSeenAt)));
        ((TextView) findViewById(R.id.ticket_now_serving)).setText(
                R.string.ticket_offline_now_serving);
        ((TextView) findViewById(R.id.ticket_offline_note)).setText(getString(
                R.string.ticket_offline_note, ticket.getTicketNumber()));

        View etaRow = findViewById(R.id.ticket_eta_row);
        ListRow.bind(etaRow, R.drawable.ic_clock, getString(R.string.ticket_offline_eta), null);
    }

    private void bindMoreTime(Ticket ticket) {
        boolean moved = ticket.isMovedBack() && ticket.getMovedBackAt() != null;
        moreTimeButton.setEnabled(!moved);
        moreTimeButton.setText(moved
                ? getString(R.string.ticket_moved_back_format, Format.time(ActiveTicketActivity.this, ticket.getMovedBackAt()))
                : getString(R.string.ticket_more_time_action));
    }

    private void bindBring(Ticket ticket) {
        Queue queue = Queues.get(ticket.getQueueId());
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

    private void bindEtaRow(Ticket ticket) {
        // The server's minutes per person for this queue today, and what it's from
        Queue queue = Queues.get(ticket.getQueueId());
        // Locale.US keeps the decimal point a point, matching the rest of the numbers.
        String basis = queue == null || queue.getServiceSampleCount() == 0
                ? getString(R.string.ticket_eta_basis_none)
                : String.format(Locale.US, getString(R.string.ticket_eta_basis_format),
                        queue.getMinutesPerPerson(), queue.getServiceSampleCount());
        ListRow.bind(findViewById(R.id.ticket_eta_row), R.drawable.ic_clock,
                getString(R.string.ticket_eta_format, ticket.getEstimatedWaitMinutes()), basis);
        findViewById(R.id.ticket_eta_row).findViewById(R.id.row_chevron).setVisibility(View.GONE);
    }

    private void bindArrival(Ticket ticket) {
        int minutes = TicketRules.beThereInMinutes(ticket.getEstimatedWaitMinutes());
        Date beThereBy = new Date(System.currentTimeMillis() + minutes * 60_000L);
        ((TextView) findViewById(R.id.ticket_be_there)).setText(getString(
                R.string.ticket_be_there_format, DateFormat.getTimeFormat(ActiveTicketActivity.this).format(beThereBy)));
        ((TextView) findViewById(R.id.ticket_arrival_venue)).setText(ticket.getVenue());
    }

    private void openDirections() {
        Ticket ticket = ActiveTicketStore.ticket(ticketId);
        Directions.open(this, ticket == null ? null : Queues.get(ticket.getQueueId()));
    }

    private void confirmLeave() {
        final Ticket ticket = ActiveTicketStore.ticket(ticketId);
        if (ticket == null) {
            return;
        }
        new MaterialAlertDialogBuilder(ActiveTicketActivity.this)
                .setTitle(R.string.ticket_leave_title)
                .setMessage(getString(R.string.ticket_leave_message,
                        ticket.getTicketNumber(), ticket.getQueueName()))
                .setNegativeButton(R.string.ticket_leave_cancel, null)
                .setPositiveButton(R.string.ticket_leave_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                leave();
                            }
                        })
                .show();
    }

    /** The server gives the place up; only then does the screen close. */
    private void leave() {
        isLeaving = true;
        loadingStateHelper.showLoading(getString(R.string.leaving_queue));
        leaveButton.setEnabled(false);
        moreTimeButton.setEnabled(false);
        ActiveTicketStore.leave(this, ticketId, new ActiveTicketStore.Done<Void>() {
            @Override
            public void onDone(@Nullable Void nothing) {
                finish();
            }

            @Override
            public void onFailed(@NonNull ApiError error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                isLeaving = false;
                leaveButton.setEnabled(true);
                moreTimeButton.setEnabled(true);
                loadingStateHelper.showContent();
                render();
                QuappMotion.snackbar(ticketCard, error.is(ApiError.OFFLINE)
                        ? getString(R.string.api_offline) : error.message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void renderServed(Ticket ticket) {
        showOutcome(ticket, R.string.ticket_status_served);
        findViewById(R.id.ticket_outcome_tile).setVisibility(View.VISIBLE);
        setText(R.id.ticket_outcome_headline, getString(R.string.ticket_served_headline));
        setText(R.id.ticket_outcome_body, getString(R.string.ticket_served_instruction));

        TextView number = keptNumber(ticket, false);
        number.setTextColor(ContextCompat.getColor(ActiveTicketActivity.this, R.color.ink));
        setText(R.id.ticket_kept_right_label, getString(R.string.ticket_outcome_status_label));
        stamp(R.id.ticket_kept_stamp_right, R.string.ticket_status_served, R.color.ok);
        findViewById(R.id.ticket_kept_stamp_left).setVisibility(View.GONE);
        findViewById(R.id.ticket_kept_no_shows).setVisibility(View.GONE);
        findViewById(R.id.ticket_outcome_note).setVisibility(View.GONE);

        doneButton.setVisibility(View.VISIBLE);
    }

    private void renderReleased(Ticket ticket) {
        showOutcome(ticket, R.string.ticket_status_expired);
        findViewById(R.id.ticket_outcome_tile).setVisibility(View.GONE);
        setText(R.id.ticket_outcome_headline, getString(R.string.ticket_expired_headline));
        setText(R.id.ticket_outcome_body, getString(R.string.ticket_expired_instruction));

        keptNumber(ticket, true).setTextColor(ContextCompat.getColor(ActiveTicketActivity.this, R.color.ink_faint));

        Queue queue = Queues.get(ticket.getQueueId());
        boolean counts = queue != null && queue.isNoShowCooldownEnabled();
        TextView noShows = findViewById(R.id.ticket_kept_no_shows);
        if (counts) {
            stamp(R.id.ticket_kept_stamp_left, R.string.ticket_status_expired, R.color.err);
            findViewById(R.id.ticket_kept_stamp_right).setVisibility(View.GONE);
            setText(R.id.ticket_kept_right_label, getString(R.string.ticket_outcome_no_shows_label));
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
        ((TextView) findViewById(R.id.ticket_outcome_note_text)).setText(expiredNote(counts));

        doneOutlinedButton.setVisibility(View.VISIBLE);
        joinAgainButton.setVisibility(View.VISIBLE);
    }

    private void renderEnded(Ticket ticket, int title, int headline, int body) {
        showOutcome(ticket, title);
        findViewById(R.id.ticket_outcome_tile).setVisibility(View.GONE);
        setText(R.id.ticket_outcome_headline, getString(headline));
        setText(R.id.ticket_outcome_body, getString(body));

        keptNumber(ticket, false).setTextColor(ContextCompat.getColor(ActiveTicketActivity.this, R.color.ink_muted));
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

    private static int removedReason(Ticket ticket) {
        if (ticket.getRemovalReason() == null) {
            return R.string.ticket_removed_instruction;
        }
        switch (ticket.getRemovalReason()) {
            case PRANK:
                return R.string.ticket_removed_prank;
            case DUPLICATE:
                return R.string.ticket_removed_duplicate;
            case ASKED_TO_LEAVE:
            default:
                return R.string.ticket_removed_asked;
        }
    }

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
        int flags = number.getPaintFlags();
        number.setPaintFlags(struck ? flags | Paint.STRIKE_THRU_TEXT_FLAG
                : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        return number;
    }

    private void stamp(int viewId, int text, @ColorRes int color) {
        TextView stamp = findViewById(viewId);
        int c = ContextCompat.getColor(ActiveTicketActivity.this, color);
        stamp.setVisibility(View.VISIBLE);
        stamp.setText(text);
        stamp.setTextColor(c);
        stamp.setBackground(TicketShapes.stampBackground(ActiveTicketActivity.this, c));
    }

    private void setText(int viewId, CharSequence text) {
        ((TextView) findViewById(viewId)).setText(text);
    }

    private void showWaiting(boolean waiting) {
        ticketCard.setVisibility(waiting ? View.VISIBLE : View.GONE);
        ticketRows.setVisibility(waiting ? View.VISIBLE : View.GONE);
        leaveButton.setVisibility(waiting ? View.VISIBLE : View.GONE);
        moreTimeButton.setVisibility(waiting ? View.VISIBLE : View.GONE);
        statusPill.setVisibility(waiting ? View.VISIBLE : View.GONE);
        outcome.setVisibility(waiting ? View.GONE : View.VISIBLE);
        doneButton.setVisibility(View.GONE);
        doneOutlinedButton.setVisibility(View.GONE);
        joinAgainButton.setVisibility(View.GONE);
        browseButton.setVisibility(View.GONE);
    }

    private void setPill(int text, @ColorRes int color, @ColorRes int ground) {
        statusPill.setText(text);
        statusPill.setTextColor(ContextCompat.getColor(ActiveTicketActivity.this, color));
        ViewCompat.setBackgroundTintList(statusPill,
                ColorStateList.valueOf(ContextCompat.getColor(ActiveTicketActivity.this, ground)));
    }

    private void applyTicketShape(View v, int width) {
        float tearCentre = ActiveTicketActivity.this.getResources().getDimension(R.dimen.ticket_stub_width)
                + ActiveTicketActivity.this.getResources().getDimension(R.dimen.hairline);
        ShapeAppearanceModel shape = TicketShapes.stub(ActiveTicketActivity.this,
                R.dimen.radius_card, R.dimen.punch_radius, tearCentre, width);
        v.setBackground(TicketShapes.background(ActiveTicketActivity.this, shape,
                ContextCompat.getColor(ActiveTicketActivity.this, R.color.spotlight)));
    }
}

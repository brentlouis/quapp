package com.example.quapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;

public class LiveConsoleActivity extends AppCompatActivity
        implements WaitingTicketAdapter.OnTicketActionListener {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_CONSOLE_QUEUE_ID";

    private String queueId;
    private Queue queue;

    private WaitingTicketAdapter ticketAdapter;
    private RecyclerView ticketList;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyBody;
    private TextView queueName;
    private TextView statusText;
    private TextView servingNumber;
    private TextView servingName;
    private TextView servingPhone;
    private TextView servingWait;
    private View servingRule;
    private CountDownTimer graceTimer;
    private View servingNone;
    private View[] servingViews;
    private TextView waitingHeader;
    private MaterialButton callNextButton;
    private MaterialButton noShowButton;
    private MaterialButton walkInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_console);
        SystemBars.applyPadding(findViewById(R.id.console_root));

        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);

        if (FakeData.queueById(queueId) == null) {
            finish();
            return;
        }

        cacheViews();

        ticketList.setLayoutManager(new LinearLayoutManager(this));
        ticketAdapter = new WaitingTicketAdapter(this);
        ticketList.setAdapter(ticketAdapter);

        findViewById(R.id.console_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.console_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showQueueMenu(view);
            }
        });

        // The spotlight panel: 10dp corners with punches halfway down both sides.
        findViewById(R.id.console_now_serving).setBackground(
                TicketShapes.spotlightBackground(this));

        // Served (if someone is being served) and call the next number.
        callNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // After a time-out the previous person is already a no-show; nothing to announce.
                Ticket done = FakeData.checkGraceExpired(queueId) ? null : FakeData.nowServing(queueId);
                FakeData.callNext(queueId);
                render();
                if (done != null) {
                    announce(R.string.console_marked_served, done);
                }
            }
        });

        noShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Ticket missed = FakeData.nowServing(queueId);
                FakeData.noShowAndCallNext(queueId);
                render();
                if (missed != null) {
                    announce(R.string.console_marked_no_show, missed);
                }
            }
        });

        walkInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWalkInDialog();
            }
        });
    }

    /** onResume so edits made on the Edit screen show as soon as the owner comes back. */
    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelGraceTimer();
    }

    private void cacheViews() {
        ticketList = findViewById(R.id.console_list);
        emptyState = findViewById(R.id.console_empty);
        emptyTitle = findViewById(R.id.console_empty_title);
        emptyBody = findViewById(R.id.console_empty_body);
        queueName = findViewById(R.id.console_queue_name);
        statusText = findViewById(R.id.console_status);
        servingNumber = findViewById(R.id.console_serving_number);
        servingName = findViewById(R.id.console_serving_name);
        servingPhone = findViewById(R.id.console_serving_phone);
        servingNone = findViewById(R.id.console_serving_none);
        servingWait = findViewById(R.id.console_serving_wait);
        servingRule = findViewById(R.id.console_serving_rule);
        servingViews = new View[]{servingNumber, servingName, servingPhone,
                findViewById(R.id.console_serving_tear)};
        waitingHeader = findViewById(R.id.console_waiting_header);
        callNextButton = findViewById(R.id.console_call_next);
        noShowButton = findViewById(R.id.console_no_show);
        walkInButton = findViewById(R.id.console_walk_in);
    }

    @Override
    public void onServeTicket(Ticket ticket) {
        FakeData.markServed(queueId, ticket.getId());
        render();
    }

    @Override
    public void onNoShowTicket(Ticket ticket) {
        FakeData.markNoShow(queueId, ticket.getId());
        render();
    }

    // ---- Queue options menu -------------------------------------------------

    private void showQueueMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.menu_console);

        // Only offer the moves that make sense from the current status.
        Menu menu = popup.getMenu();
        Queue.Status status = queue.getStatus();
        menu.findItem(R.id.console_action_pause).setVisible(status == Queue.Status.OPEN);
        menu.findItem(R.id.console_action_resume).setVisible(status == Queue.Status.PAUSED);
        menu.findItem(R.id.console_action_close).setVisible(status != Queue.Status.CLOSED);
        menu.findItem(R.id.console_action_reopen).setVisible(status == Queue.Status.CLOSED);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onQueueMenuItem(item.getItemId());
            }
        });
        popup.show();
    }

    private boolean onQueueMenuItem(int itemId) {
        if (itemId == R.id.console_action_insights) {
            Intent intent = new Intent(this, QueueAnalyticsActivity.class);
            intent.putExtra(QueueAnalyticsActivity.EXTRA_QUEUE_ID, queueId);
            startActivity(intent);
        } else if (itemId == R.id.console_action_edit) {
            Intent intent = new Intent(this, CreateQueueActivity.class);
            intent.putExtra(CreateQueueActivity.EXTRA_QUEUE_ID, queueId);
            startActivity(intent);
        } else if (itemId == R.id.console_action_pause) {
            changeStatus(Queue.Status.PAUSED);
        } else if (itemId == R.id.console_action_resume
                || itemId == R.id.console_action_reopen) {
            changeStatus(Queue.Status.OPEN);
        } else if (itemId == R.id.console_action_close) {
            confirmClose();
        } else {
            return false;
        }
        return true;
    }

    /** Closing releases everyone in line, so it asks first. Pausing is easy to undo, so it doesn't. */
    private void confirmClose() {
        int waiting = FakeData.waitingTickets(queueId).size();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.console_close_title)
                .setNegativeButton(R.string.console_cancel, null)
                .setPositiveButton(R.string.console_close_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                changeStatus(Queue.Status.CLOSED);
                            }
                        });

        if (waiting > 0) {
            builder.setMessage(getResources().getQuantityString(
                    R.plurals.console_close_message, waiting, waiting));
        }

        builder.show();
    }

    private void changeStatus(Queue.Status status) {
        FakeData.setQueueStatus(queueId, status);
        render();
    }

    // ---- Walk-in ------------------------------------------------------------

    private void showWalkInDialog() {
        View body = getLayoutInflater().inflate(R.layout.dialog_walk_in, null);
        final TextInputLayout nameLayout = body.findViewById(R.id.walk_in_name_layout);
        final TextInputEditText nameInput = body.findViewById(R.id.walk_in_name_input);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.console_walk_in_title)
                .setView(body)
                .setNegativeButton(R.string.console_cancel, null)
                .setPositiveButton(R.string.console_walk_in_confirm, null)
                .show();

        // Set after show(): a listener passed to setPositiveButton always closes the
        // dialog, even when the name is empty. This one only closes it on success.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = Forms.text(nameInput);
                if (!Forms.check(nameLayout, !Validation.isBlank(name),
                        getString(R.string.console_walk_in_error))) {
                    return;
                }

                Ticket ticket = FakeData.addWalkIn(queueId, name);
                dialog.dismiss();
                render();
                Snackbar.make(ticketList, getString(R.string.console_walk_in_added,
                        ticket.getTicketNumber(), ticket.getHolderName()),
                        Snackbar.LENGTH_SHORT).setAnchorView(R.id.console_tear).show();
            }
        });
    }

    // ---- Rendering ----------------------------------------------------------

    private void render() {
        queue = FakeData.queueById(queueId);
        if (queue == null) {
            finish();
            return;
        }

        List<Ticket> waitingTickets = FakeData.waitingTickets(queueId);
        Ticket nowServing = FakeData.nowServing(queueId);
        boolean closed = queue.getStatus() == Queue.Status.CLOSED;

        queueName.setText(queue.getName());

        switch (queue.getStatus()) {
            case UPCOMING:
                statusText.setText(R.string.console_status_upcoming);
                break;
            case OPEN:
                statusText.setText(R.string.console_status_open);
                break;
            case PAUSED:
                statusText.setText(R.string.console_status_paused);
                break;
            case CLOSED:
                statusText.setText(R.string.console_status_closed);
                break;
        }

        boolean timedOut = FakeData.checkGraceExpired(queueId);
        bindNowServing(nowServing, timedOut);

        waitingHeader.setText(getString(R.string.console_up_next_format, waitingTickets.size()));
        bindDock(nowServing, timedOut, waitingTickets, closed);
        walkInButton.setEnabled(!closed);

        if (waitingTickets.isEmpty()) {
            ticketList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyTitle.setText(closed ? R.string.console_closed_title : R.string.console_empty_title);
            emptyBody.setText(closed ? R.string.console_closed_body : R.string.console_empty_body);
        } else {
            ticketList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        ticketAdapter.submitTickets(waitingTickets);
    }

    private void bindNowServing(Ticket nowServing, boolean timedOut) {
        cancelGraceTimer();
        boolean someone = nowServing != null;
        for (View v : servingViews) {
            v.setVisibility(someone ? View.VISIBLE : View.GONE);
        }
        servingNone.setVisibility(someone ? View.GONE : View.VISIBLE);
        if (!someone) {
            servingWait.setVisibility(View.GONE);
            servingRule.setVisibility(View.GONE);
            return;
        }
        servingNumber.setText(getString(R.string.console_ticket_format, nowServing.getTicketNumber()));
        // Marigold while being served; dimmed once the slot is released (DESIGN.md section 6).
        servingNumber.setTextColor(ContextCompat.getColor(this,
                timedOut ? R.color.on_spotlight_muted : R.color.signal));
        servingName.setText(nowServing.getHolderName());
        servingPhone.setText(nowServing.isWalkIn() ? getString(R.string.console_walk_in_label)
                : WaitingTicketAdapter.maskPhone(this, nowServing.getHolderPhone()));

        bindGraceLine(nowServing, timedOut);
    }

    /**
     * Under the name: the grace countdown while waiting for "I'm here", or that the slot was
     * released. Only for queues with the presence check on.
     */
    private void bindGraceLine(Ticket nowServing, boolean timedOut) {
        boolean grace = queue.isGracePeriodEnabled() && nowServing.getCalledAt() != null;
        servingWait.setVisibility(grace ? View.VISIBLE : View.GONE);
        servingRule.setVisibility(grace ? View.VISIBLE : View.GONE);
        if (!grace) {
            return;
        }
        if (timedOut) {
            servingWait.setText(R.string.console_timed_out);
            servingWait.setTextColor(ContextCompat.getColor(this, R.color.err_on_spotlight));
            return;
        }
        servingWait.setTextColor(ContextCompat.getColor(this, R.color.on_spotlight));
        long left = FakeData.graceDeadline(nowServing).toEpochMilli() - System.currentTimeMillis();
        graceTimer = new CountDownTimer(Math.max(left, 0), 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000L;
                servingWait.setText(getString(R.string.console_awaiting_format,
                        String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L)));
            }

            @Override
            public void onFinish() {
                // Time's up: render again, which records the no-show and switches the dock.
                render();
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

    /**
     * The dock acts on whoever is being served. With someone at the counter: No-show, or
     * "Served · call #24". With nobody yet: just "Call #24". Paused still lets the owner work
     * through the line; closed stops everything.
     */
    private void bindDock(Ticket nowServing, boolean timedOut, List<Ticket> waiting, boolean closed) {
        Ticket next = waiting.isEmpty() ? null : waiting.get(0);
        // A timed-out slot is already a no-show, so only "Call next" is left.
        noShowButton.setVisibility(nowServing == null || timedOut ? View.GONE : View.VISIBLE);
        noShowButton.setEnabled(!closed);

        if (timedOut) {
            callNextButton.setText(next == null ? getString(R.string.console_call_next)
                    : getString(R.string.console_call_next_name_format,
                            next.getTicketNumber(), next.getHolderName()));
            callNextButton.setEnabled(!closed);
        } else if (nowServing == null) {
            callNextButton.setText(next == null ? getString(R.string.console_call_next)
                    : getString(R.string.console_call_format, next.getTicketNumber()));
            callNextButton.setEnabled(!closed && next != null);
        } else {
            callNextButton.setText(next == null ? getString(R.string.console_serve_action)
                    : getString(R.string.console_served_call_format, next.getTicketNumber()));
            callNextButton.setEnabled(!closed);
        }
    }

    /** "#22 Rosa Diaz marked served", above the dock. */
    private void announce(int message, Ticket ticket) {
        Snackbar.make(ticketList, getString(message, ticket.getTicketNumber(),
                ticket.getHolderName()), Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.console_tear)
                .show();
    }
}

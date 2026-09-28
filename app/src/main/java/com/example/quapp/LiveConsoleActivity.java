package com.example.quapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

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
    private TextView servingValue;
    private TextView waitingHeader;
    private MaterialButton callNextButton;
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

        callNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FakeData.callNext(queueId);
                render();
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

    private void cacheViews() {
        ticketList = findViewById(R.id.console_list);
        emptyState = findViewById(R.id.console_empty);
        emptyTitle = findViewById(R.id.console_empty_title);
        emptyBody = findViewById(R.id.console_empty_body);
        queueName = findViewById(R.id.console_queue_name);
        statusText = findViewById(R.id.console_status);
        servingValue = findViewById(R.id.console_serving_value);
        waitingHeader = findViewById(R.id.console_waiting_header);
        callNextButton = findViewById(R.id.console_call_next);
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
                        Snackbar.LENGTH_SHORT).show();
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

        if (nowServing == null) {
            servingValue.setText(R.string.console_none_serving);
        } else {
            servingValue.setText(getString(R.string.console_called_format,
                    nowServing.getTicketNumber(), nowServing.getHolderName()));
        }

        waitingHeader.setText(getString(R.string.console_waiting_count_format,
                waitingTickets.size()));

        // Paused still lets the owner work through the line; closed stops everything.
        callNextButton.setEnabled(!closed && !waitingTickets.isEmpty());
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
}

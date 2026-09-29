package com.example.quapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;

/**
 * The organizer's Live console (canvas 14): who's at the counter, the line, and the queue's
 * options. The server keeps the line; this screen asks for it every 5 seconds while it's open
 * (DECISIONS.md "You've been called reaches the phone by polling") and after every action.
 */
public class LiveConsoleActivity extends AppCompatActivity
        implements WaitingTicketAdapter.OnTicketActionListener {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_CONSOLE_QUEUE_ID";

    private static final long POLL_MS = 5_000L;

    private String queueId;
    @Nullable
    private Queue queue;
    @Nullable
    private Line line;
    /** An action is on its way to the server: the dock waits for the answer. */
    private boolean busy;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            load();
            handler.postDelayed(this, POLL_MS);
        }
    };

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
        queue = Queues.get(queueId);  // Your queues loaded it; the first poll brings the latest

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
                if (queue != null) {
                    showQueueOptions();
                }
            }
        });

        // The spotlight panel: 10dp corners with punches halfway down both sides.
        findViewById(R.id.console_now_serving).setBackground(
                TicketShapes.spotlightBackground(this));

        // Served (if someone is being served) and call the next number. After a time-out the
        // one at the counter didn't come, so it's recorded as a no-show instead of served.
        callNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (line == null) {
                    return;
                }
                if (line.isNowServingTimedOut()) {
                    act(api().noShow(queueId), null, 0);
                } else {
                    act(api().callNext(queueId), line.getNowServing(), R.string.console_marked_served);
                }
            }
        });

        noShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (line != null) {
                    act(api().noShow(queueId), line.getNowServing(), R.string.console_marked_no_show);
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
        handler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
        cancelGraceTimer();
    }

    private QuappApi api() {
        return ApiClient.api(this);
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

    // ---- Talking to the server ----------------------------------------------

    /** The queue (name, status, hours) and the line, each drawn as it arrives. */
    private void load() {
        Queues.fetch(this, queueId, new Queues.Loaded() {
            @Override
            public void onLoaded(Queue loaded) {
                queue = loaded;
                render();
            }

            @Override
            public void onFailed(ApiError error) {
                if (queue == null) {
                    // Nothing to show at all: go back (ApiCallback has said why)
                    finish();
                }
            }
        });
        api().line(queueId).enqueue(new ApiCallback<Line>(this) {
            @Override
            protected void onSuccess(@Nullable Line loaded) {
                // An answer that left before an action finished would undo what it shows
                if (!busy) {
                    line = loaded;
                    render();
                }
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                // Offline or the server is down: keep the line on screen and try next round
            }
        });
    }

    /**
     * Call next or No-show: the dock waits for the answer, which is the new line. `done` is
     * who was at the counter, announced with `message` once it worked.
     */
    private void act(Call<Line> call, @Nullable final Ticket done, final int message) {
        if (busy) {
            return;
        }
        setBusy(true);
        call.enqueue(new ApiCallback<Line>(this) {
            @Override
            protected void onSuccess(@Nullable Line updated) {
                line = updated;
                setBusy(false);
                if (done != null && message != 0) {
                    announce(message, done);
                }
                refreshQueue();
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                setBusy(false);
                showError(error);
                load();
            }
        });
    }

    /** After something that changes the numbers on the queue itself (waiting, now serving). */
    private void refreshQueue() {
        Queues.fetch(this, queueId, new Queues.Loaded() {
            @Override
            public void onLoaded(Queue loaded) {
                queue = loaded;
                render();
            }

            @Override
            public void onFailed(ApiError error) {
                // The next poll tries again
            }
        });
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        render();  // the dock's buttons wait while busy
    }

    private String message(ApiError error) {
        return error.is(ApiError.OFFLINE) ? getString(R.string.api_offline) : error.message;
    }

    private void showError(ApiError error) {
        Snackbar.make(ticketList, message(error), Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.console_tear)
                .show();
    }

    /** A waiting row's only action: take them out of the line, with a reason (canvas 57). */
    @Override
    public void onRemoveTicket(Ticket ticket) {
        ConsoleSheets.showRemove(this, ticket, new ConsoleSheets.RemoveListener() {
            @Override
            public void onRemove(final Ticket removed, Ticket.RemovalReason reason) {
                api().remove(removed.getId(), new QuappApi.RemoveBody(reason))
                        .enqueue(new ApiCallback<Void>(LiveConsoleActivity.this) {
                            @Override
                            protected void onSuccess(@Nullable Void nothing) {
                                announce(R.string.console_removed_format, removed);
                                load();
                            }

                            @Override
                            protected void onError(@NonNull ApiError error) {
                                showError(error);
                                load();
                            }
                        });
            }
        });
    }

    // ---- Queue options (canvas 18) -------------------------------------------------

    private void showQueueOptions() {
        ConsoleSheets.showOptions(this, queue, new ConsoleSheets.OptionsListener() {
            @Override
            public void onExtend() {
                showExtend();
            }

            @Override
            public void onEdit() {
                Intent intent = new Intent(LiveConsoleActivity.this, CreateQueueActivity.class);
                intent.putExtra(CreateQueueActivity.EXTRA_QUEUE_ID, queueId);
                startActivity(intent);
            }

            @Override
            public void onInsights() {
                Intent intent = new Intent(LiveConsoleActivity.this, QueueAnalyticsActivity.class);
                intent.putExtra(QueueAnalyticsActivity.EXTRA_QUEUE_ID, queueId);
                startActivity(intent);
            }

            @Override
            public void onShare() {
                startActivity(ShareQueueActivity.intent(LiveConsoleActivity.this, queueId));
            }

            @Override
            public void onPauseJoins() {
                changeStatus(api().pause(queueId));
            }

            @Override
            public void onResumeJoins() {
                changeStatus(api().resume(queueId));
            }

            @Override
            public void onClose() {
                confirmClose();
            }
        });
    }

    /** Extend closing time (canvas 19). */
    private void showExtend() {
        ConsoleSheets.showExtend(this, queue, waitingCount(), new ConsoleSheets.ExtendListener() {
            @Override
            public void onExtended(final LocalTime closesAt) {
                api().extend(queueId, new QuappApi.ExtendBody(closesAt))
                        .enqueue(new ApiCallback<Queue>(LiveConsoleActivity.this) {
                            @Override
                            protected void onSuccess(@Nullable Queue updated) {
                                queueChanged(updated);
                                Snackbar.make(ticketList, getString(R.string.extend_done_format,
                                        Format.time(LiveConsoleActivity.this, closesAt)),
                                        Snackbar.LENGTH_SHORT)
                                        .setAnchorView(R.id.console_tear)
                                        .show();
                            }

                            @Override
                            protected void onError(@NonNull ApiError error) {
                                showError(error);
                            }
                        });
            }
        });
    }

    /**
     * Closing releases everyone in line, so it asks first (canvas 18b), with the number of
     * people on the red button. Pausing is easy to undo, so it doesn't.
     */
    private void confirmClose() {
        int waiting = waitingCount();

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.console_close_title_format, queue.getName()))
                .setMessage(waiting > 0
                        ? getResources().getQuantityString(R.plurals.console_close_release_message, waiting, waiting)
                        : getString(R.string.console_close_empty_message))
                .setNegativeButton(R.string.console_keep_open, null)
                .setPositiveButton(waiting > 0
                                ? getResources().getQuantityString(R.plurals.console_close_release_action, waiting, waiting)
                                : getString(R.string.console_close_confirm),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                changeStatus(api().close(queueId));
                            }
                        })
                .show();
        // The one action that can't be undone reads red.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                ContextCompat.getColor(this, R.color.err));
    }

    /** Pause, resume or close: the answer is the queue in its new state. */
    private void changeStatus(Call<Queue> call) {
        call.enqueue(new ApiCallback<Queue>(this) {
            @Override
            protected void onSuccess(@Nullable Queue updated) {
                queueChanged(updated);
                load();  // closing releases the line
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                showError(error);
            }
        });
    }

    private void queueChanged(@Nullable Queue updated) {
        if (updated != null) {
            Queues.put(updated);
            queue = updated;
        }
        render();
    }

    private int waitingCount() {
        return line == null ? 0 : line.getWaiting().size();
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
        final View addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = Forms.text(nameInput);
                if (!Forms.check(nameLayout, !Validation.isBlank(name),
                        getString(R.string.console_walk_in_error))) {
                    return;
                }

                addButton.setEnabled(false);
                api().walkIn(queueId, new QuappApi.WalkInBody(name))
                        .enqueue(new ApiCallback<Ticket>(LiveConsoleActivity.this) {
                            @Override
                            protected void onSuccess(@Nullable Ticket ticket) {
                                dialog.dismiss();
                                Snackbar.make(ticketList, getString(R.string.console_walk_in_added,
                                        ticket.getTicketNumber(), ticket.getHolderName()),
                                        Snackbar.LENGTH_SHORT).setAnchorView(R.id.console_tear).show();
                                load();
                            }

                            @Override
                            protected void onError(@NonNull ApiError error) {
                                addButton.setEnabled(true);
                                nameLayout.setError(message(error));
                            }
                        });
            }
        });
    }

    // ---- Rendering ----------------------------------------------------------

    private void render() {
        if (queue == null) {
            return;  // the first answer draws it
        }
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
        walkInButton.setEnabled(!closed);

        if (line == null) {
            // The line hasn't arrived yet: the header says so, and the dock waits for it
            waitingHeader.setText(R.string.console_loading);
            bindNowServing(null, false);
            noShowButton.setVisibility(View.GONE);
            callNextButton.setEnabled(false);
            return;
        }

        List<Ticket> waitingTickets = line.getWaiting();
        Ticket nowServing = line.getNowServing();
        boolean timedOut = line.isNowServingTimedOut();

        bindNowServing(nowServing, timedOut);

        waitingHeader.setText(getString(R.string.console_up_next_format, waitingTickets.size()));
        bindDock(nowServing, timedOut, waitingTickets, closed);

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

    private void bindNowServing(@Nullable Ticket nowServing, boolean timedOut) {
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
        // The server sends the phone masked already ("0917 ••• 0002")
        servingPhone.setText(nowServing.isWalkIn() ? getString(R.string.console_walk_in_label)
                : nowServing.getHolderPhone());

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
            setWaitIcon(R.drawable.ic_timer, R.color.err_on_spotlight);
            return;
        }
        // Canvas 29: they tapped "I'm here", so the countdown stops.
        if (line.getNowServingHereAt() != null) {
            servingWait.setText(getString(R.string.console_confirmed_format,
                    Format.time(this, line.getNowServingHereAt())));
            servingWait.setTextColor(ContextCompat.getColor(this, R.color.ok_on_spotlight));
            setWaitIcon(R.drawable.ic_circle_check, R.color.ok_on_spotlight);
            return;
        }
        servingWait.setTextColor(ContextCompat.getColor(this, R.color.on_spotlight));
        setWaitIcon(R.drawable.ic_timer, R.color.on_spotlight_muted);
        long left = nowServing.getCalledAt().toEpochMilli() + CalledActivity.GRACE_PERIOD_MS
                - System.currentTimeMillis();
        graceTimer = new CountDownTimer(Math.max(left, 0), 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000L;
                servingWait.setText(getString(R.string.console_awaiting_format,
                        String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L)));
            }

            @Override
            public void onFinish() {
                // Time's up: the server now says timed out, and the dock switches
                load();
            }
        };
        graceTimer.start();
    }

    /** The grace line's icon follows its state: a timer while waiting, a check once confirmed. */
    private void setWaitIcon(int icon, int color) {
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(servingWait, icon, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(servingWait,
                ColorStateList.valueOf(ContextCompat.getColor(this, color)));
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
     * through the line; closed stops everything. Both wait while an action is on its way.
     */
    private void bindDock(Ticket nowServing, boolean timedOut, List<Ticket> waiting, boolean closed) {
        Ticket next = waiting.isEmpty() ? null : waiting.get(0);
        // After a time-out, Call next records the no-show itself, so No-show isn't offered.
        noShowButton.setVisibility(nowServing == null || timedOut ? View.GONE : View.VISIBLE);
        noShowButton.setEnabled(!closed && !busy);

        if (timedOut) {
            callNextButton.setText(next == null ? getString(R.string.console_call_next)
                    : getString(R.string.console_call_next_name_format,
                            next.getTicketNumber(), next.getHolderName()));
            callNextButton.setEnabled(!closed && !busy);
        } else if (nowServing == null) {
            callNextButton.setText(next == null ? getString(R.string.console_call_next)
                    : getString(R.string.console_call_format, next.getTicketNumber()));
            callNextButton.setEnabled(!closed && !busy && next != null);
        } else {
            callNextButton.setText(next == null ? getString(R.string.console_serve_action)
                    : getString(R.string.console_served_call_format, next.getTicketNumber()));
            callNextButton.setEnabled(!closed && !busy);
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

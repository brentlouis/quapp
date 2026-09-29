package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Join confirm (canvas 07). Shows the number you'll be handed and who you're joining as.
 * The account's name and phone go on the ticket unless you tap Edit; an account that never
 * registered a name goes straight to the fields.
 */
public class JoinQueueActivity extends AppCompatActivity {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_JOIN_QUEUE_ID";
    /** Where Queue detail's proximity check found the queuer; absent when the queue doesn't check. */
    public static final String EXTRA_LATITUDE = "com.example.quapp.EXTRA_JOIN_LATITUDE";
    public static final String EXTRA_LONGITUDE = "com.example.quapp.EXTRA_JOIN_LONGITUDE";

    private static final String STATE_EDITING = "editing";

    private Queue queue;
    private View identityCard;
    private View fields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_queue);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.join_root));

        String queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        queue = Queues.get(queueId);  // Queue detail loaded it just before

        if (queue == null) {
            finish();
            return;
        }

        identityCard = findViewById(R.id.join_identity);
        fields = findViewById(R.id.join_fields);
        findViewById(R.id.join_dock).setBackground(TicketShapes.stubDockBackground(this));

        bindQueue();
        bindIdentity(savedInstanceState != null && savedInstanceState.getBoolean(STATE_EDITING));

        findViewById(R.id.join_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.join_report).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(ReportQueueActivity.intent(JoinQueueActivity.this, queue.getId()));
            }
        });

        findViewById(R.id.join_edit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFields();
            }
        });

        findViewById(R.id.join_submit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitJoin();
            }
        });
    }

    /** Remember Edit across rotation; the fields keep their own text. */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_EDITING, fields.getVisibility() == View.VISIBLE);
    }

    private void bindQueue() {
        ((TextView) findViewById(R.id.join_queue_name)).setText(queue.getName());

        // Joining puts you at the back: everyone waiting now is ahead of you.
        int ahead = queue.getPeopleWaiting();
        ((TextView) findViewById(R.id.join_number)).setText(
                getString(R.string.ticket_number_format, queue.getNextTicketNumber()));
        ((TextView) findViewById(R.id.join_ahead)).setText(ahead > 0
                ? getString(R.string.join_ahead_format, ahead, queue.getEstimatedWaitMinutes())
                : getString(R.string.join_ahead_next_format, queue.getEstimatedWaitMinutes()));

        findViewById(R.id.join_grace_note).setVisibility(
                queue.isGracePeriodEnabled() ? View.VISIBLE : View.GONE);
    }

    /** Fills the "Joining as" card and the fields from the account. */
    private void bindIdentity(boolean editing) {
        Session session = new Session(this);
        String name = session.getName();
        String phone = session.getPhone();

        ((TextView) findViewById(R.id.join_identity_phone)).setText(Format.spacedPhone(phone));
        TextInputEditText nameInput = findViewById(R.id.join_name_input);
        TextInputEditText phoneInput = findViewById(R.id.join_phone_input);
        if (Forms.text(phoneInput).isEmpty()) {
            phoneInput.setText(phone);
        }

        if (name == null || editing) {
            showFields();
            return;
        }
        ((TextView) findViewById(R.id.join_identity_name)).setText(name);
        ((TextView) findViewById(R.id.join_initials)).setText(Format.initials(name));
        if (Forms.text(nameInput).isEmpty()) {
            nameInput.setText(name);
        }
    }

    private void showFields() {
        identityCard.setVisibility(View.GONE);
        fields.setVisibility(View.VISIBLE);
    }

    private void submitJoin() {
        TextInputEditText nameInput = findViewById(R.id.join_name_input);
        TextInputEditText phoneInput = findViewById(R.id.join_phone_input);

        String holderName = Forms.text(nameInput);
        String holderPhone = Validation.normalizePhone(Forms.text(phoneInput));

        boolean valid = Forms.check(findViewById(R.id.join_name_layout),
                !Validation.isBlank(holderName), getString(R.string.join_name_error));
        valid &= Forms.check(findViewById(R.id.join_phone_layout),
                Validation.isValidPhone(holderPhone), getString(R.string.join_phone_error));

        if (!valid) {
            // The errors are on the fields, so make sure they're on screen.
            showFields();
            return;
        }

        Double latitude = getIntent().hasExtra(EXTRA_LATITUDE)
                ? getIntent().getDoubleExtra(EXTRA_LATITUDE, 0) : null;
        Double longitude = getIntent().hasExtra(EXTRA_LONGITUDE)
                ? getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0) : null;

        // The server puts the ticket at the back of the line, or says why it can't
        setBusy(true);
        ActiveTicketStore.join(this, queue.getId(), latitude, longitude, holderName, holderPhone,
                new ActiveTicketStore.Done<Ticket>() {
                    @Override
                    public void onDone(@Nullable Ticket ticket) {
                        startActivity(ActiveTicketActivity.intent(JoinQueueActivity.this, ticket.getId()));
                        // Once, on Android 13+: ask for notifications on top of the new ticket (canvas 20).
                        if (NotificationPermissionActivity.shouldAsk(JoinQueueActivity.this)) {
                            startActivity(NotificationPermissionActivity.intent(
                                    JoinQueueActivity.this, ticket.getId()));
                        }
                        finish();
                    }

                    @Override
                    public void onFailed(@NonNull ApiError error) {
                        setBusy(false);
                        refused(error);
                    }
                });
    }

    /**
     * Why the server said no. Most reasons are also checked on Queue detail before this screen
     * opens, so these are the rare cases where something changed in between (the queue paused,
     * a cooldown started): say so, and go back to Queue detail, which now shows it.
     */
    private void refused(ApiError error) {
        String ticketId = error.extraString("ticket_id");
        if (error.is("ALREADY_IN_LINE") && ticketId != null) {
            startActivity(ActiveTicketActivity.intent(this, ticketId));
            finish();
            return;
        }
        if (error.is("INVALID_INPUT") && error.fieldMessage("holder_phone") != null) {
            showFields();
            ((TextInputLayout) findViewById(R.id.join_phone_layout))
                    .setError(error.fieldMessage("holder_phone"));
            return;
        }
        Snackbar.make(findViewById(R.id.join_root), error.is(ApiError.OFFLINE)
                        ? getString(R.string.api_offline) : error.message, Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.join_dock)
                .show();
    }

    /** While waiting for the server: the button says so and can't be tapped twice. */
    private void setBusy(boolean busy) {
        MaterialButton submit = findViewById(R.id.join_submit_button);
        submit.setEnabled(!busy);
        submit.setText(busy ? R.string.join_busy : R.string.join_action);
    }
}

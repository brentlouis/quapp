package com.example.quapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.time.Instant;
import java.util.Map;

public class QueueDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_QUEUE_ID";

    /** Where the proximity check stands for this visit (canvas 44, 45, 48). */
    private enum LocationState {
        /** Not checked yet, or passed: Join works as usual. */
        UNCHECKED,
        CHECKING,
        TOO_FAR,
        /** Permission refused, or the phone's Location switch is off. */
        OFF
    }

    private String queueId;
    private LocationState locationState = LocationState.UNCHECKED;
    /** How far away the last check found the queuer, for "You're 4.2 km away". */
    private float distanceMeters;

    /**
     * Android's own permission dialog. Registered as a field because an Activity must register
     * for results before it starts; the callback runs when the dialog closes.
     */
    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    if (ProximityCheck.hasPermission(QueueDetailActivity.this)) {
                        checkLocation();
                    } else {
                        setLocationState(LocationState.OFF);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_detail);
        SystemBars.applyPadding(findViewById(R.id.detail_root));

        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);

        if (FakeData.queueById(queueId) == null) {
            finish();
            return;
        }

        ImageButton backButton = findViewById(R.id.detail_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.detail_directions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Directions.open(QueueDetailActivity.this, FakeData.queueById(queueId));
            }
        });

        findViewById(R.id.detail_browse_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(QueuerHomeActivity.intent(QueueDetailActivity.this,
                        QueuerHomeActivity.TAB_BROWSE));
                finish();
            }
        });

        // Tear-off stub: raised paper with its top corners bitten out.
        findViewById(R.id.detail_dock).setBackground(TicketShapes.stubDockBackground(this));
    }

    /** Re-bound on every return: the ticket or cooldown may have changed while away. */
    @Override
    protected void onResume() {
        super.onResume();

        Queue queue = FakeData.queueById(queueId);
        if (queue == null) {
            finish();
            return;
        }

        // Back from Settings with location allowed and on: Join works again.
        if (locationState == LocationState.OFF && ProximityCheck.hasPermission(this)
                && ProximityCheck.isLocationOn(this)) {
            locationState = LocationState.UNCHECKED;
        }

        bindQueue(queue);
        bindJoinButton(queue);
    }

    /**
     * Checked in priority order — the first one that applies decides the button:
     * already holding this queue's ticket, queue not open, holding a ticket for a queue
     * whose hours overlap this one, on cooldown, or free to join.
     */
    private void bindJoinButton(final Queue queue) {
        MaterialButton joinButton = findViewById(R.id.detail_join_button);
        TextView notice = findViewById(R.id.detail_notice);
        View youWillBe = findViewById(R.id.detail_you_will_be);
        final Ticket ticket = ActiveTicketStore.ticketForQueue(queue.getId());

        notice.setVisibility(View.GONE);
        youWillBe.setVisibility(View.GONE);
        findViewById(R.id.detail_location_note).setVisibility(View.GONE);
        showCooldown(false);
        joinButton.setEnabled(false);
        joinButton.setOnClickListener(null);

        if (ticket != null) {
            joinButton.setEnabled(true);
            joinButton.setText(R.string.detail_view_ticket_action);
            joinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(ActiveTicketActivity.intent(QueueDetailActivity.this, ticket.getId()));
                }
            });
            return;
        }

        if (queue.getStatus() == Queue.Status.CLOSED) {
            joinButton.setText(R.string.detail_closed_action);
            return;
        }

        if (queue.getStatus() == Queue.Status.PAUSED) {
            joinButton.setText(R.string.detail_paused_action);
            showNotice(notice, queue.getPausedAt() == null ? getString(R.string.detail_paused_notice)
                    : getString(R.string.detail_paused_since_notice,
                            Format.time(this, queue.getPausedAt())));
            return;
        }

        // Several tickets are fine, as long as no two queues are open at the same time.
        Ticket clash = ActiveTicketStore.overlapping(queue);
        if (clash != null) {
            joinButton.setText(R.string.detail_overlap_action);
            showNotice(notice, getString(R.string.detail_overlap_notice, clash.getQueueName()));
            return;
        }

        if (queue.isNoShowCooldownEnabled() && Cooldown.isActive()) {
            bindCooldown();
            joinButton.setText(getString(R.string.detail_cooldown_join_format,
                    Cooldown.remainingMinutes()));
            return;
        }

        if (queue.isProximityCheckEnabled() && locationState != LocationState.UNCHECKED) {
            bindLocationState(queue, joinButton);
            return;
        }

        // Free to join: the stub shows the number you'd be handed.
        youWillBe.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.detail_next_number)).setText(
                getString(R.string.ticket_number_format, FakeData.nextTicketNumber(queue.getId())));
        joinButton.setEnabled(true);
        // Upcoming queues take joins too: you hold a number before it opens.
        joinButton.setText(queue.getStatus() == Queue.Status.UPCOMING
                ? R.string.detail_join_early_action : R.string.detail_join_action);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!queue.isProximityCheckEnabled()) {
                    openJoin();
                } else if (ProximityCheck.hasPermission(QueueDetailActivity.this)) {
                    checkLocation();
                } else {
                    showLocationSheet(queue);
                }
            }
        });
    }

    private void openJoin() {
        Intent intent = new Intent(this, JoinQueueActivity.class);
        intent.putExtra(JoinQueueActivity.EXTRA_QUEUE_ID, queueId);
        startActivity(intent);
    }

    // ---- Proximity check (canvas 44, 45, 48) ------------------------------------

    /** Explains the check before Android's permission dialog. "Not now" just closes it. */
    private void showLocationSheet(Queue queue) {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        View content = getLayoutInflater().inflate(R.layout.sheet_location, null);
        ((TextView) content.findViewById(R.id.location_sheet_body)).setText(getString(
                R.string.location_sheet_body,
                Format.distance(this, queue.getJoinRadiusMeters()), queue.getVenue()));
        content.findViewById(R.id.location_sheet_allow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sheet.dismiss();
                locationPermission.launch(ProximityCheck.PERMISSIONS);
            }
        });
        content.findViewById(R.id.location_sheet_not_now).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sheet.dismiss();
            }
        });
        sheet.setContentView(content);
        sheet.show();
    }

    /** Reads the location once. Inside the radius goes on to Join; outside shows how far. */
    private void checkLocation() {
        if (!ProximityCheck.isLocationOn(this)) {
            setLocationState(LocationState.OFF);
            return;
        }
        setLocationState(LocationState.CHECKING);
        ProximityCheck.locate(this, new ProximityCheck.Callback() {
            @Override
            public void onLocated(@Nullable Location location) {
                Queue queue = FakeData.queueById(queueId);
                if (isFinishing() || queue == null) {
                    return;
                }
                if (location == null) {
                    setLocationState(LocationState.UNCHECKED);
                    Snackbar.make(findViewById(R.id.detail_root), R.string.detail_location_failed,
                            Snackbar.LENGTH_LONG).setAnchorView(R.id.detail_dock).show();
                    return;
                }
                distanceMeters = ProximityCheck.distanceMeters(location, queue);
                if (distanceMeters <= queue.getJoinRadiusMeters()) {
                    setLocationState(LocationState.UNCHECKED);
                    openJoin();
                } else {
                    setLocationState(LocationState.TOO_FAR);
                }
            }
        });
    }

    private void setLocationState(LocationState state) {
        locationState = state;
        Queue queue = FakeData.queueById(queueId);
        if (queue != null) {
            bindJoinButton(queue);
        }
    }

    /** The dock and the note for a check that's running, failed or found you too far. */
    private void bindLocationState(Queue queue, MaterialButton joinButton) {
        String radius = Format.distance(this, queue.getJoinRadiusMeters());
        switch (locationState) {
            case CHECKING:
                joinButton.setText(R.string.detail_checking_location);
                break;
            case TOO_FAR:
                // Directions stays next to the venue; the dock is one full-width Check again.
                showLocationNote(getString(R.string.detail_too_far_title,
                                Format.distance(this, distanceMeters)),
                        getString(R.string.detail_too_far_body, radius));
                joinButton.setEnabled(true);
                joinButton.setText(R.string.detail_check_again);
                joinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        checkLocation();
                    }
                });
                break;
            case OFF:
            default:
                showLocationNote(getString(R.string.detail_location_off_title),
                        getString(R.string.detail_location_off_body, radius));
                findViewById(R.id.detail_browse_button).setVisibility(View.VISIBLE);
                joinButton.setEnabled(true);
                joinButton.setText(R.string.detail_location_on_action);
                joinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        turnOnLocation();
                    }
                });
                break;
        }
    }

    private void showLocationNote(String title, String body) {
        findViewById(R.id.detail_location_note).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.detail_location_title)).setText(title);
        ((TextView) findViewById(R.id.detail_location_body)).setText(body);
    }

    /**
     * Whichever is in the way: Quapp's permission, or the phone's Location switch. Android only
     * shows its permission dialog again if the user hasn't refused twice; after that, the app's
     * settings page is the only place to allow it.
     */
    private void turnOnLocation() {
        if (!ProximityCheck.hasPermission(this)) {
            boolean canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    ProximityCheck.PERMISSIONS[0]);
            if (canAskAgain) {
                locationPermission.launch(ProximityCheck.PERMISSIONS);
            } else {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null)));
            }
        } else {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    /**
     * Canvas 27: how many slots were missed, the minutes left in the big count and the time it
     * ends. The dock offers other queues, since Join can't help yet.
     */
    private void bindCooldown() {
        showCooldown(true);
        ((TextView) findViewById(R.id.detail_cooldown_body)).setText(getResources().getQuantityString(
                R.plurals.detail_cooldown_body, Cooldown.NO_SHOW_LIMIT, Cooldown.NO_SHOW_LIMIT));
        ((TextView) findViewById(R.id.detail_cooldown_minutes)).setText(
                String.valueOf(Cooldown.remainingMinutes()));
        ((TextView) findViewById(R.id.detail_cooldown_at)).setText(getString(
                R.string.detail_cooldown_at_format,
                Format.time(this, Instant.now().plusMillis(Cooldown.remainingMs()))));
    }

    private void showCooldown(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        findViewById(R.id.detail_cooldown).setVisibility(visibility);
        findViewById(R.id.detail_cooldown_others).setVisibility(visibility);
        findViewById(R.id.detail_browse_button).setVisibility(visibility);
    }

    private void showNotice(TextView notice, String text) {
        notice.setText(text);
        notice.setVisibility(View.VISIBLE);
    }

    private void bindQueue(Queue queue) {
        ((TextView) findViewById(R.id.detail_name)).setText(queue.getName());
        ((TextView) findViewById(R.id.detail_venue)).setText(getString(R.string.browse_venue_format,
                queue.getVenue(), queue.getMunicipality()));
        // The serial line: the category printed like a ticket serial (the Label style caps it).
        ((TextView) findViewById(R.id.detail_category)).setText(queue.getCategory().label);
        // The full details when the organizer wrote them, else the one-line description.
        ((TextView) findViewById(R.id.detail_description)).setText(queue.getDetails() != null
                ? queue.getDetails() : queue.getShortDescription());

        TextView bring = findViewById(R.id.detail_bring);
        bring.setVisibility(queue.getBring() == null ? View.GONE : View.VISIBLE);
        if (queue.getBring() != null) {
            bring.setText(Html.fromHtml(getString(R.string.detail_bring_format,
                    "<b>" + TextUtils.htmlEncode(queue.getBring()) + "</b>"),
                    Html.FROM_HTML_MODE_LEGACY));
        }

        bindOrganizer(queue);

        bindStatus(queue.getStatus());

        LinearLayout slip = findViewById(R.id.detail_slip);
        ReceiptSlip.clear(slip);
        ReceiptSlip.addRow(slip, getString(R.string.detail_eta_label),
                queue.getStatus() == Queue.Status.CLOSED ? getString(R.string.browse_eta_none)
                        : getString(R.string.detail_eta_value_format, queue.getEstimatedWaitMinutes()));
        ReceiptSlip.addRow(slip, getString(R.string.detail_waiting_label),
                String.valueOf(queue.getPeopleWaiting()));
        ReceiptSlip.addRow(slip, getString(R.string.detail_hours_slip_label),
                Format.schedule(this, queue));

        bindVerification(queue);
    }

    /**
     * "Organized by … · Verified" with the badge, or "· Unverified" and a note to check the
     * organizer's own announcement (canvas 53). Either way, tapping it explains (52).
     */
    private void bindOrganizer(final Queue queue) {
        ((TextView) findViewById(R.id.detail_organizer)).setText(
                getString(R.string.detail_organized_by, queue.getOrganizerName()));
        boolean verified = queue.isOrganizerVerified();
        findViewById(R.id.detail_verified_icon).setVisibility(verified ? View.VISIBLE : View.GONE);
        TextView label = findViewById(R.id.detail_verified_label);
        label.setText(verified ? R.string.detail_verified : R.string.detail_unverified);
        label.setTextColor(ContextCompat.getColor(this, verified ? R.color.ok : R.color.ink_muted));

        TextView note = findViewById(R.id.detail_unverified_note);
        note.setVisibility(verified ? View.GONE : View.VISIBLE);
        note.setText(Html.fromHtml(getString(R.string.detail_unverified_note),
                Html.FROM_HTML_MODE_LEGACY));

        findViewById(R.id.detail_organizer_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TrustSheets.showOrganizer(QueueDetailActivity.this, queue);
            }
        });
    }

    /** Open is green on its soft ground, paused amber, upcoming and closed plain grey. */
    private void bindStatus(Queue.Status status) {
        TextView pill = findViewById(R.id.detail_status);
        int text;
        int color;
        int ground;
        switch (status) {
            case PAUSED:
                text = R.string.detail_status_paused;
                color = R.color.warn;
                ground = R.color.warn_soft;
                break;
            case UPCOMING:
                text = R.string.detail_status_upcoming;
                color = R.color.ink_muted;
                ground = R.color.paper_sunk;
                break;
            case CLOSED:
                text = R.string.detail_status_closed;
                color = R.color.ink_muted;
                ground = R.color.paper_sunk;
                break;
            case OPEN:
            default:
                text = R.string.detail_status_open;
                color = R.color.ok;
                ground = R.color.ok_soft;
                break;
        }
        pill.setText(text);
        pill.setTextColor(ContextCompat.getColor(this, color));
        ViewCompat.setBackgroundTintList(pill,
                ColorStateList.valueOf(ContextCompat.getColor(this, ground)));
    }

    /**
     * The checks always appear in the same order. Ones this queue uses say ON; SMS confirmation
     * isn't built (future work), so it always shows as PLANNED.
     */
    private void bindVerification(Queue queue) {
        View proximity = findViewById(R.id.detail_rule_proximity);
        proximity.setVisibility(queue.isProximityCheckEnabled() ? View.VISIBLE : View.GONE);
        if (queue.isProximityCheckEnabled()) {
            fillRule(proximity, R.drawable.ic_navigation,
                    getString(R.string.detail_proximity_title_format, Format.distance(this, queue.getJoinRadiusMeters())),
                    getString(R.string.detail_verification_proximity_body), true);
        }
        bindRule(R.id.detail_rule_grace, queue.isGracePeriodEnabled(), R.drawable.ic_timer,
                R.string.requirement_grace_title, getString(R.string.detail_verification_grace));
        bindRule(R.id.detail_rule_penalty, queue.isNoShowCooldownEnabled(), R.drawable.ic_block,
                R.string.requirement_penalty_title, getString(R.string.detail_verification_penalty));

        View otp = findViewById(R.id.detail_rule_otp);
        fillRule(otp, R.drawable.ic_sms, getString(R.string.requirement_otp_title),
                getString(R.string.detail_verification_otp_planned), false);
    }

    private void bindRule(int includeId, boolean enabled, int iconRes, int titleRes, String body) {
        View row = findViewById(includeId);
        row.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (enabled) {
            fillRule(row, iconRes, getString(titleRes), body, true);
        }
    }

    private void fillRule(View row, int iconRes, String title, String body, boolean on) {
        ImageView icon = row.findViewById(R.id.requirement_icon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(this, on ? R.color.ok : R.color.ink_muted));
        ((TextView) row.findViewById(R.id.requirement_title)).setText(title);
        ((TextView) row.findViewById(R.id.requirement_body)).setText(body);

        TextView state = row.findViewById(R.id.requirement_state);
        state.setText(on ? R.string.detail_rule_on : R.string.detail_rule_planned);
        state.setTextColor(ContextCompat.getColor(this, on ? R.color.ok : R.color.ink_muted));
        ViewCompat.setBackgroundTintList(state, ColorStateList.valueOf(
                ContextCompat.getColor(this, on ? R.color.ok_soft : R.color.paper_sunk)));
    }
}

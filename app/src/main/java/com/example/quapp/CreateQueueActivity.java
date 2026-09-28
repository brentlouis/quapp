package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Creates a queue, or edits one when started with {@link #EXTRA_QUEUE_ID}.
 * One screen for both, because the form is identical — only the title,
 * the button label and the starting values change.
 */
public class CreateQueueActivity extends AppCompatActivity {

    /** Optional. Present = edit that queue; absent = create a new one. */
    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_EDIT_QUEUE_ID";

    private Queue existingQueue;

    private TextInputEditText nameInput;
    private TextInputEditText venueInput;
    private MaterialAutoCompleteTextView municipalityInput;
    private MaterialAutoCompleteTextView categoryInput;
    private TextInputEditText hoursInput;
    private TextInputEditText descriptionInput;

    private MaterialSwitch otpSwitch;
    private MaterialSwitch graceSwitch;
    private MaterialSwitch penaltySwitch;
    private MaterialSwitch proximitySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_queue);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.create_root));

        String queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        if (queueId != null) {
            existingQueue = FakeData.queueById(queueId);
            if (existingQueue == null) {
                finish();
                return;
            }
        }

        nameInput = findViewById(R.id.create_name_input);
        venueInput = findViewById(R.id.create_venue_input);
        municipalityInput = findViewById(R.id.create_municipality_input);
        categoryInput = findViewById(R.id.create_category_input);
        hoursInput = findViewById(R.id.create_hours_input);
        descriptionInput = findViewById(R.id.create_description_input);

        otpSwitch = setUpToggle(R.id.create_toggle_otp,
                R.string.create_otp_label, R.string.create_otp_summary, false);

        graceSwitch = setUpToggle(R.id.create_toggle_grace,
                R.string.create_grace_label, R.string.create_grace_summary, true);

        penaltySwitch = setUpToggle(R.id.create_toggle_penalty,
                R.string.create_penalty_label, R.string.create_penalty_summary, false);

        proximitySwitch = setUpToggle(R.id.create_toggle_proximity,
                R.string.create_proximity_label, R.string.create_proximity_summary, false);

        MaterialButton submitButton = findViewById(R.id.create_submit_button);

        if (existingQueue != null) {
            TextView title = findViewById(R.id.create_title_text);
            TextView subtitle = findViewById(R.id.create_subtitle_text);
            title.setText(R.string.create_edit_title);
            subtitle.setText(R.string.create_edit_subtitle);
            submitButton.setText(R.string.create_save_action);

            // After rotation the views restore their own state; prefilling again would undo edits.
            if (savedInstanceState == null) {
                prefill(existingQueue);
            }
        }

        findViewById(R.id.create_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitQueue();
            }
        });
    }

    private MaterialSwitch setUpToggle(int includeId, int labelRes, int summaryRes,
                                       boolean checkedByDefault) {
        View row = findViewById(includeId);

        TextView label = row.findViewById(R.id.toggle_label);
        TextView summary = row.findViewById(R.id.toggle_summary);
        final MaterialSwitch toggle = row.findViewById(R.id.toggle_switch);

        label.setText(labelRes);
        summary.setText(summaryRes);
        toggle.setChecked(checkedByDefault);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle.toggle();
            }
        });

        return toggle;
    }

    private void prefill(Queue queue) {
        nameInput.setText(queue.getName());
        venueInput.setText(queue.getVenue());
        // false = don't filter the dropdown down to just this value.
        municipalityInput.setText(queue.getMunicipality(), false);
        categoryInput.setText(queue.getCategory(), false);
        hoursInput.setText(queue.getServiceHours());
        descriptionInput.setText(queue.getDescription());

        otpSwitch.setChecked(queue.isSmsOtpEnabled());
        graceSwitch.setChecked(queue.isGracePeriodEnabled());
        penaltySwitch.setChecked(queue.isNoShowPenaltyEnabled());
        proximitySwitch.setChecked(queue.isProximityCheckEnabled());
    }

    private void submitQueue() {
        String name = Forms.text(nameInput);
        String venue = Forms.text(venueInput);
        String municipality = Forms.text(municipalityInput);
        String category = Forms.text(categoryInput);
        String hours = Forms.text(hoursInput);
        String description = Forms.text(descriptionInput);

        // Checked in on-screen order, and all of them, so every problem shows at once.
        boolean valid = required(R.id.create_name_layout, name);
        valid &= required(R.id.create_venue_layout, venue);
        valid &= required(R.id.create_municipality_layout, municipality);
        valid &= required(R.id.create_category_layout, category);
        valid &= required(R.id.create_hours_layout, hours);

        if (!valid) {
            return;
        }

        // Editing keeps id, location and open/paused/closed status; creating starts fresh.
        Queue.Builder builder = existingQueue != null
                ? existingQueue.toBuilder()
                : new Queue.Builder()
                        .setId(FakeData.newQueueId())
                        .setLocation(FakeData.defaultLatitude(), FakeData.defaultLongitude())
                        .setStatus(Queue.Status.OPEN);

        Queue queue = builder
                .setName(name)
                .setVenue(venue)
                .setMunicipality(municipality)
                .setCategory(category)
                .setServiceHours(hours)
                .setDescription(description)
                .setSmsOtpEnabled(otpSwitch.isChecked())
                .setGracePeriodEnabled(graceSwitch.isChecked())
                .setNoShowPenaltyEnabled(penaltySwitch.isChecked())
                .setProximityCheckEnabled(proximitySwitch.isChecked())
                .build();

        FakeData.saveQueue(queue);
        finish();
    }

    private boolean required(int layoutId, String value) {
        TextInputLayout layout = findViewById(layoutId);
        return Forms.check(layout, !Validation.isBlank(value),
                getString(R.string.create_required_error));
    }
}

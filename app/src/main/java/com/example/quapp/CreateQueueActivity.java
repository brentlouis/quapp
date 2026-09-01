package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

public class CreateQueueActivity extends AppCompatActivity {

    private MaterialSwitch otpSwitch;
    private MaterialSwitch graceSwitch;
    private MaterialSwitch penaltySwitch;
    private MaterialSwitch proximitySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_queue);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.create_root));

        otpSwitch = setUpToggle(R.id.create_toggle_otp,
                R.string.create_otp_label, R.string.create_otp_summary, false);

        graceSwitch = setUpToggle(R.id.create_toggle_grace,
                R.string.create_grace_label, R.string.create_grace_summary, true);

        penaltySwitch = setUpToggle(R.id.create_toggle_penalty,
                R.string.create_penalty_label, R.string.create_penalty_summary, false);

        proximitySwitch = setUpToggle(R.id.create_toggle_proximity,
                R.string.create_proximity_label, R.string.create_proximity_summary, false);

        findViewById(R.id.create_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        MaterialButton submitButton = findViewById(R.id.create_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitQueue(view);
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

    private void submitQueue(View anchor) {
        // TODO: read fields, validate, persist. Navigation-only for now.
        Snackbar.make(anchor, R.string.create_action, Snackbar.LENGTH_SHORT).show();
        finish();
    }
}
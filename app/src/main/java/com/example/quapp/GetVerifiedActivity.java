package com.example.quapp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


/**
 * Get verified (canvas 50). The organizer names the organization, its type, their position and
 * the office's public number; the admin calls that number (DECISIONS.md "Organizer verification:
 * optional, checked by phone, by hand"). Submitting sets the account to pending (canvas 51).
 */
public class GetVerifiedActivity extends AppCompatActivity {

    private static final String STATE_TYPE = "type";

    @Nullable
    private VerificationRequest.OrganizationType type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_verified);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.verify_root));

        if (savedInstanceState != null && savedInstanceState.getString(STATE_TYPE) != null) {
            type = VerificationRequest.OrganizationType.valueOf(savedInstanceState.getString(STATE_TYPE));
        }
        bindType();

        // Most organizers post under the name they already use.
        TextInputEditText org = findViewById(R.id.verify_org_input);
        if (savedInstanceState == null) {
            org.setText(new Session(this).postingAs());
        }

        findViewById(R.id.verify_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.verify_type).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickType();
            }
        });

        findViewById(R.id.verify_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TYPE, type == null ? null : type.name());
    }

    private void pickType() {
        final VerificationRequest.OrganizationType[] types = VerificationRequest.OrganizationType.values();
        String[] labels = new String[types.length];
        int checked = -1;
        for (int i = 0; i < types.length; i++) {
            labels[i] = getString(types[i].label);
            if (types[i] == type) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.verify_type_label)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        type = types[which];
                        findViewById(R.id.verify_type_error).setVisibility(View.GONE);
                        bindType();
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void bindType() {
        ((TextView) findViewById(R.id.verify_type)).setText(type == null ? null : getString(type.label));
    }

    private void submit() {
        String organization = Forms.text(findViewById(R.id.verify_org_input));
        String position = Forms.text(findViewById(R.id.verify_position_input));
        String phone = Forms.text(findViewById(R.id.verify_phone_input));

        // Every problem at once, top to bottom.
        boolean valid = Forms.check(findViewById(R.id.verify_org_layout),
                !Validation.isBlank(organization), getString(R.string.verify_org_error));
        findViewById(R.id.verify_type_error).setVisibility(type == null ? View.VISIBLE : View.GONE);
        valid &= type != null;
        valid &= Forms.check(findViewById(R.id.verify_position_layout),
                !Validation.isBlank(position), getString(R.string.verify_position_error));
        valid &= Forms.check(findViewById(R.id.verify_phone_layout),
                Validation.isValidOfficePhone(phone), getString(R.string.verify_phone_error));
        if (!valid) {
            return;
        }

        final View button = findViewById(R.id.verify_submit);
        button.setEnabled(false);
        ApiClient.api(this).askToBeVerified(new QuappApi.VerificationBody(organization, type,
                position, phone)).enqueue(new ApiCallback<VerificationRequest>(this) {
            @Override
            protected void onSuccess(@Nullable VerificationRequest request) {
                // The account is PENDING now; Profile asks for it again when it shows
                Toast.makeText(GetVerifiedActivity.this, R.string.verify_sent, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                button.setEnabled(true);
                if (error.is("INVALID_INPUT") && error.fieldMessage("office_phone") != null) {
                    ((TextInputLayout) findViewById(R.id.verify_phone_layout))
                            .setError(error.fieldMessage("office_phone"));
                    return;
                }
                super.onError(error);  // the Snackbar
            }
        });
    }
}

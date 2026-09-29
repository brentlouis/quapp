package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    /** The server's limit (routers/auth.py MAX_ACCOUNTS_PER_DEVICE), for the limit's wording. */
    private static final int MAX_ACCOUNTS_PER_DEVICE = 2;

    private MaterialButton submitButton;
    private View.OnClickListener backToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.register_root));

        // The length rule shows before anyone gets it wrong; an error replaces it while shown.
        TextInputLayout passwordLayout = findViewById(R.id.register_password_layout);
        passwordLayout.setHelperText(
                getString(R.string.register_password_helper, Validation.MIN_PASSWORD_LENGTH));

        backToLogin = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        };
        findViewById(R.id.register_back).setOnClickListener(backToLogin);

        submitButton = findViewById(R.id.register_submit_button);
        MaterialButton loginButton = findViewById(R.id.register_login_button);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitRegistration();
            }
        });

        loginButton.setOnClickListener(backToLogin);
    }

    /**
     * Canvas 56: this phone already made the most accounts it can (the server said
     * DEVICE_LIMIT). The form stays visible but does nothing; the one button goes back to Log in.
     */
    private void showDeviceLimit() {
        int max = MAX_ACCOUNTS_PER_DEVICE;
        findViewById(R.id.register_limit).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.register_limit_title)).setText(
                getResources().getQuantityString(R.plurals.register_limit_title, max, max));
        ((TextView) findViewById(R.id.register_limit_body)).setText(
                getResources().getQuantityString(R.plurals.register_limit_body, max, max));
        for (int id : new int[]{R.id.register_name_layout, R.id.register_phone_layout,
                R.id.register_password_layout, R.id.register_confirm_layout}) {
            findViewById(id).setEnabled(false);
        }
        findViewById(R.id.register_login_row).setVisibility(View.GONE);
        submitButton.setEnabled(true);
        submitButton.setText(R.string.register_limit_action);
        submitButton.setOnClickListener(backToLogin);
    }

    private void submitRegistration() {
        TextInputEditText nameInput = findViewById(R.id.register_name_input);
        TextInputEditText phoneInput = findViewById(R.id.register_phone_input);
        TextInputEditText passwordInput = findViewById(R.id.register_password_input);
        TextInputEditText confirmInput = findViewById(R.id.register_confirm_input);

        String name = Forms.text(nameInput);
        String phone = Validation.normalizePhone(Forms.text(phoneInput));
        String password = Forms.text(passwordInput);
        String confirm = Forms.text(confirmInput);

        // Checked here first, so a typo doesn't need a round trip to the server
        boolean valid = Forms.check(findViewById(R.id.register_name_layout),
                !Validation.isBlank(name), getString(R.string.register_name_error));
        valid &= Forms.check(findViewById(R.id.register_phone_layout),
                Validation.isValidPhone(phone), getString(R.string.register_phone_error));
        valid &= Forms.check(findViewById(R.id.register_password_layout),
                Validation.isValidPassword(password),
                getString(R.string.register_password_error, Validation.MIN_PASSWORD_LENGTH));
        valid &= Forms.check(findViewById(R.id.register_confirm_layout),
                confirm.equals(password), getString(R.string.register_confirm_error));
        if (!valid) {
            return;
        }

        setBusy(true);
        Session session = new Session(this);
        ApiClient.api(this).register(new QuappApi.RegisterBody(name, phone, password,
                        session.installId()))
                .enqueue(new ApiCallback<QuappApi.AuthResponse>(this) {
                    @Override
                    protected void onSuccess(@Nullable QuappApi.AuthResponse auth) {
                        Session session = new Session(RegisterActivity.this);
                        session.signIn(auth.token, auth.user);
                        // homeIntent clears Login and Register off the stack
                        startActivity(session.homeIntent(RegisterActivity.this));
                    }

                    @Override
                    protected void onError(@NonNull ApiError error) {
                        setBusy(false);
                        if (error.is("DEVICE_LIMIT")) {
                            showDeviceLimit();
                        } else if (error.is("PHONE_TAKEN")) {
                            ((TextInputLayout) findViewById(R.id.register_phone_layout))
                                    .setError(getString(R.string.register_phone_taken));
                        } else if (error.is("INVALID_INPUT") && error.fieldMessage("phone") != null) {
                            ((TextInputLayout) findViewById(R.id.register_phone_layout))
                                    .setError(error.fieldMessage("phone"));
                        } else {
                            super.onError(error);
                        }
                    }
                });
    }

    /** While waiting for the server: the button says so and can't be tapped twice. */
    private void setBusy(boolean busy) {
        submitButton.setEnabled(!busy);
        submitButton.setText(busy ? R.string.register_busy : R.string.register_action);
    }
}

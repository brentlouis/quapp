package com.example.quapp;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.register_root));
        androidx.core.view.ViewCompat.setAccessibilityHeading(findViewById(R.id.register_heading), true);

        // The length rule shows before anyone gets it wrong; an error replaces it while shown.
        TextInputLayout passwordLayout = findViewById(R.id.register_password_layout);
        passwordLayout.setHelperText(
                getString(R.string.register_password_helper, Validation.MIN_PASSWORD_LENGTH));

        View.OnClickListener backToLogin = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        };
        findViewById(R.id.register_back).setOnClickListener(backToLogin);

        MaterialButton submitButton = findViewById(R.id.register_submit_button);
        MaterialButton loginButton = findViewById(R.id.register_login_button);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitRegistration();
            }
        });

        loginButton.setOnClickListener(v -> {
            if (getIntent().getBooleanExtra("from_welcome", false)) {
                startActivity(new android.content.Intent(this, LoginActivity.class));
            }
            finish();
        });

        AuthForms.bind(this, new int[]{R.id.register_name_layout, R.id.register_phone_layout,
                R.id.register_password_layout, R.id.register_confirm_layout},
                new int[]{R.id.register_password_input, R.id.register_confirm_input}, this::submitRegistration);
    }

    private void submitRegistration() {
        if (!findViewById(R.id.register_submit_button).isEnabled()) return;
        TextInputEditText nameInput = findViewById(R.id.register_name_input);
        TextInputEditText phoneInput = findViewById(R.id.register_phone_input);
        TextInputEditText passwordInput = findViewById(R.id.register_password_input);
        TextInputEditText confirmInput = findViewById(R.id.register_confirm_input);

        String name = Forms.text(nameInput);
        String phone = Validation.normalizePhone(Forms.text(phoneInput));
        String password = AuthForms.password(passwordInput);
        String confirm = AuthForms.password(confirmInput);

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
            AuthForms.revealError(this, R.id.register_name_layout, R.id.register_phone_layout,
                    R.id.register_password_layout, R.id.register_confirm_layout);
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
                        TextInputLayout phoneLayout = findViewById(R.id.register_phone_layout);
                        if (error.is("DEVICE_LIMIT")) {
                            showDeviceLimit();
                        } else if (error.is("PHONE_TAKEN")) {
                            phoneLayout.setError(getString(R.string.register_phone_taken));
                        } else if (error.is("INVALID_INPUT") && error.fieldMessage("phone") != null) {
                            phoneLayout.setError(error.fieldMessage("phone"));
                        } else {
                            super.onError(error);
                        }
                    }
                });
    }

    /**
     * Canvas 56: the server allows 2 accounts per install (routers/auth.py
     * MAX_ACCOUNTS_PER_DEVICE) and this phone has made both. The way on is Log in.
     */
    private void showDeviceLimit() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.register_device_limit_title)
                .setMessage(R.string.register_device_limit_body)
                .setNegativeButton(R.string.register_device_limit_cancel, null)
                .setPositiveButton(R.string.auth_sign_in, (dialog, which) -> {
                    startActivity(new android.content.Intent(this, LoginActivity.class));
                    finish();
                })
                .show();
    }

    /** While waiting for the server: the button says so and can't be tapped twice. */
    private void setBusy(boolean busy) {
        MaterialButton submitButton = findViewById(R.id.register_submit_button);
        submitButton.setEnabled(!busy);
        submitButton.setText(busy ? R.string.creating_account : R.string.auth_create);
    }

    @Override protected void onStop() {
        AuthForms.clearPasswords(this, R.id.register_password_input, R.id.register_confirm_input);
        super.onStop();
    }
}

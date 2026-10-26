package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // The form and actions scroll above the keyboard.
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.login_root));
        androidx.core.view.ViewCompat.setAccessibilityHeading(findViewById(R.id.login_heading), true);

        MaterialButton submitButton = findViewById(R.id.login_submit_button);
        MaterialButton registerButton = findViewById(R.id.login_register_button);
        MaterialButton forgotButton = findViewById(R.id.login_forgot_button);
        findViewById(R.id.login_back).setOnClickListener(v -> finish());
        AuthForms.bind(this, new int[]{R.id.login_phone_layout, R.id.login_password_layout},
                new int[]{R.id.login_password_input}, this::submitLogin);

        // The last number used on this phone, so signing back in is just the password
        TextInputEditText phoneInput = findViewById(R.id.login_phone_input);
        String lastPhone = new Session(this).getPhone();
        if (savedInstanceState == null && !lastPhone.isEmpty()) {
            phoneInput.setText(lastPhone);
        }

        forgotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitLogin();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void submitLogin() {
        if (!findViewById(R.id.login_submit_button).isEnabled()) return;
        TextInputEditText phoneInput = findViewById(R.id.login_phone_input);
        TextInputEditText passwordInput = findViewById(R.id.login_password_input);

        String phone = Validation.normalizePhone(Forms.text(phoneInput));
        String password = AuthForms.password(passwordInput);

        boolean valid = Forms.check(findViewById(R.id.login_phone_layout),
                Validation.isValidPhone(phone), getString(R.string.login_phone_error));
        valid &= Forms.check(findViewById(R.id.login_password_layout),
                !Validation.isBlank(password), getString(R.string.login_password_error));

        if (!valid) {
            AuthForms.revealError(this, R.id.login_phone_layout, R.id.login_password_layout);
            return;
        }

        setBusy(true);
        final String finalPhone = phone;
        final TextInputLayout passwordLayout = findViewById(R.id.login_password_layout);
        ApiClient.api(this).login(new QuappApi.LoginBody(phone, password))
                .enqueue(new ApiCallback<QuappApi.AuthResponse>(this) {
                    @Override
                    protected void onSuccess(@Nullable QuappApi.AuthResponse auth) {
                        Session session = new Session(LoginActivity.this);
                        session.signIn(auth.token, auth.user);
                        startActivity(session.homeIntent(LoginActivity.this));
                    }

                    @Override
                    protected void onError(@NonNull ApiError error) {
                        setBusy(false);
                        if (error.is("WRONG_CREDENTIALS")) {
                            passwordLayout.setError(getString(R.string.login_wrong));
                        } else if (error.is("SUSPENDED")) {
                            // A suspended account doesn't get in; it sees why (canvas 55)
                            startActivity(AccountSuspendedActivity.intent(LoginActivity.this, finalPhone,
                                    error.extraString("suspended_reason"),
                                    error.extraString("suspended_at")));
                        } else {
                            super.onError(error);
                        }
                    }
                });
    }

    /** While waiting for the server: the button says so and can't be tapped twice. */
    private void setBusy(boolean busy) {
        MaterialButton submitButton = findViewById(R.id.login_submit_button);
        submitButton.setEnabled(!busy);
        submitButton.setText(busy ? R.string.signing_in : R.string.auth_sign_in);
    }

    @Override protected void onStop() {
        AuthForms.clearPasswords(this, R.id.login_password_input);
        super.onStop();
    }
}

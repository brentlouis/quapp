package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

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

        final MaterialButton submitButton = findViewById(R.id.login_submit_button);
        submitButton.setEnabled(false);
        submitButton.setText(R.string.signing_in);

        final String finalPhone = phone;
        FakeData.loginAsync(this, phone, password, new FakeData.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (user.getStatus() == User.Status.SUSPENDED) {
                    submitButton.setEnabled(true);
                    submitButton.setText(R.string.auth_sign_in_demo);
                    startActivity(AccountSuspendedActivity.intent(LoginActivity.this, finalPhone));
                    return;
                }
                Session session = new Session(LoginActivity.this);
                startActivity(session.homeIntent(LoginActivity.this));
            }

            @Override
            public void onError(Throwable error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                submitButton.setEnabled(true);
                submitButton.setText(R.string.auth_sign_in_demo);
                QuappMotion.snackbar(
                        findViewById(R.id.login_root), R.string.auth_demo_error,
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override protected void onStop() {
        AuthForms.clearPasswords(this, R.id.login_password_input);
        super.onStop();
    }
}

package com.example.quapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
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

        final MaterialButton submitButton = findViewById(R.id.register_submit_button);
        submitButton.setEnabled(false);
        submitButton.setText(R.string.creating_account);

        FakeData.registerAsync(this, name, phone, new FakeData.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Session session = new Session(RegisterActivity.this);
                startActivity(session.homeIntent(RegisterActivity.this));
            }

            @Override
            public void onError(Throwable error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                submitButton.setEnabled(true);
                submitButton.setText(R.string.auth_create_demo);
                QuappMotion.snackbar(
                        findViewById(R.id.register_root), R.string.auth_demo_error,
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override protected void onStop() {
        AuthForms.clearPasswords(this, R.id.register_password_input, R.id.register_confirm_input);
        super.onStop();
    }
}

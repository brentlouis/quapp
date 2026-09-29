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
        // The dock sits above the keyboard instead of behind it.
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.login_root));

        MaterialButton submitButton = findViewById(R.id.login_submit_button);
        MaterialButton registerButton = findViewById(R.id.login_register_button);
        MaterialButton forgotButton = findViewById(R.id.login_forgot_button);

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
        TextInputEditText phoneInput = findViewById(R.id.login_phone_input);
        TextInputEditText passwordInput = findViewById(R.id.login_password_input);

        String phone = Validation.normalizePhone(Forms.text(phoneInput));
        String password = Forms.text(passwordInput);

        boolean valid = Forms.check(findViewById(R.id.login_phone_layout),
                Validation.isValidPhone(phone), getString(R.string.login_phone_error));
        valid &= Forms.check(findViewById(R.id.login_password_layout),
                !Validation.isBlank(password), getString(R.string.login_password_error));

        if (!valid) {
            return;
        }

        // A suspended account doesn't get in; it sees why (canvas 55).
        if (FakeData.suspendedAccount(phone) != null) {
            startActivity(AccountSuspendedActivity.intent(this, phone));
            return;
        }

        // No backend yet, so any other well-formed login is accepted. The password is never stored.
        Session session = new Session(this);
        session.logIn(phone);
        startActivity(session.homeIntent(this));
    }
}

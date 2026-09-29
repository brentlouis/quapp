package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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

        loginButton.setOnClickListener(backToLogin);

        bindDeviceLimit(submitButton, backToLogin);
    }

    /**
     * Canvas 56: this phone already made the most accounts it can. The form stays visible but
     * does nothing; the one button goes back to Log in. The server enforces the same limit by
     * install id; this is the local stand-in.
     */
    private void bindDeviceLimit(MaterialButton submitButton, View.OnClickListener backToLogin) {
        Session session = new Session(this);
        if (!session.deviceAccountLimitReached()) {
            return;
        }
        int max = Session.MAX_ACCOUNTS_PER_DEVICE;
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

        Session session = new Session(this);
        session.register(name, phone);
        // homeIntent clears Login and Register off the stack; Back won't return to either.
        startActivity(session.homeIntent(this));
    }
}

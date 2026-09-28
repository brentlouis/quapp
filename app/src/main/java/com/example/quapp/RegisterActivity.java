package com.example.quapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.register_root));

        MaterialButton submitButton = findViewById(R.id.register_submit_button);
        MaterialButton loginButton = findViewById(R.id.register_login_button);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitRegistration();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
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

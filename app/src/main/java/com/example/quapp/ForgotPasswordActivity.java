package com.example.quapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Forgot password. There's no SMS gateway, so there are no reset codes: this screen explains
 * the manual route and opens an email to support (DECISIONS.md "Forgot password goes through
 * support").
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        SystemBars.applyPadding(findViewById(R.id.forgot_root));

        View.OnClickListener back = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        };
        findViewById(R.id.forgot_back).setOnClickListener(back);
        findViewById(R.id.forgot_back_button).setOnClickListener(back);

        findViewById(R.id.forgot_email_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emailSupport();
            }
        });
    }

    private void emailSupport() {
        Support.email(this, getString(R.string.forgot_email_subject),
                getString(R.string.forgot_email_body));
    }
}

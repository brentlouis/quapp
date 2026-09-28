package com.example.quapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

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

    /**
     * ACTION_SENDTO with a mailto: address only matches email apps, not every app that can
     * share text. The subject and body are filled in so the user only types their details.
     */
    private void emailSupport() {
        String address = getString(R.string.forgot_support_email);
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.forgot_email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.forgot_email_body));

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No email app installed: show the address so they can write from anywhere.
            Snackbar.make(findViewById(R.id.forgot_root),
                    getString(R.string.forgot_no_email_app, address),
                    Snackbar.LENGTH_LONG).show();
        }
    }
}

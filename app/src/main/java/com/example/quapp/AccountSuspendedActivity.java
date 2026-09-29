package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Account suspended (canvas 55). Login shows this instead of letting a suspended account in
 * (DECISIONS.md "Suspended accounts are blocked at login"). Appeals go to support by email.
 */
public class AccountSuspendedActivity extends AppCompatActivity {

    public static final String EXTRA_PHONE = "com.example.quapp.EXTRA_SUSPENDED_PHONE";

    public static Intent intent(Context context, String phone) {
        Intent intent = new Intent(context, AccountSuspendedActivity.class);
        intent.putExtra(EXTRA_PHONE, phone);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_suspended);
        SystemBars.applyPadding(findViewById(R.id.suspended_root));

        final String phone = getIntent().getStringExtra(EXTRA_PHONE);
        User user = phone == null ? null : FakeData.suspendedAccount(phone);
        if (user == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.suspended_reason)).setText(user.getSuspendedReason());
        ((TextView) findViewById(R.id.suspended_since)).setText(user.getSuspendedAt() == null ? null
                : user.getSuspendedAt().atZone(Format.MANILA).format(
                        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())));

        findViewById(R.id.suspended_email).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Support.email(AccountSuspendedActivity.this,
                        getString(R.string.suspended_email_subject),
                        getString(R.string.suspended_email_body, Format.spacedPhone(phone)));
            }
        });

        findViewById(R.id.suspended_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}

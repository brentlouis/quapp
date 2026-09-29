package com.example.quapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Account suspended (canvas 55). Login shows this instead of letting a suspended account in
 * (DECISIONS.md "Suspended accounts are blocked at login"). Appeals go to support by email.
 */
public class AccountSuspendedActivity extends AppCompatActivity {

    public static final String EXTRA_PHONE = "com.example.quapp.EXTRA_SUSPENDED_PHONE";
    public static final String EXTRA_REASON = "com.example.quapp.EXTRA_SUSPENDED_REASON";
    /** When, as the server sent it ("2026-09-28T09:00:00+08:00"). */
    public static final String EXTRA_SINCE = "com.example.quapp.EXTRA_SUSPENDED_SINCE";

    /** From the server's SUSPENDED error: its suspended_reason and suspended_at. */
    public static Intent intent(Context context, String phone, @Nullable String reason,
                                @Nullable String since) {
        Intent intent = new Intent(context, AccountSuspendedActivity.class);
        intent.putExtra(EXTRA_PHONE, phone);
        intent.putExtra(EXTRA_REASON, reason);
        intent.putExtra(EXTRA_SINCE, since);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  // started from outside a screen
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_suspended);
        SystemBars.applyPadding(findViewById(R.id.suspended_root));

        final String phone = getIntent().getStringExtra(EXTRA_PHONE);
        String since = getIntent().getStringExtra(EXTRA_SINCE);

        ((TextView) findViewById(R.id.suspended_reason)).setText(
                getIntent().getStringExtra(EXTRA_REASON));
        ((TextView) findViewById(R.id.suspended_since)).setText(since == null ? null
                : OffsetDateTime.parse(since).atZoneSameInstant(Format.MANILA).format(
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

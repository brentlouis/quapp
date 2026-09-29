package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Help and support (canvas 49), from the Profile tab on both sides. Three topics explained in a
 * sheet each, an email to support, and Report a queue (DECISIONS.md "Help and support").
 */
public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        SystemBars.applyPadding(findViewById(R.id.help_root));

        findViewById(R.id.help_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        topic(R.id.help_queues_row, R.drawable.ic_list_checks, R.string.help_queues_title,
                R.string.help_queues_subtitle, R.string.help_queues_body);
        topic(R.id.help_checks_row, R.drawable.ic_badge_check, R.string.help_checks_title,
                R.string.help_checks_subtitle, R.string.help_checks_body);
        topic(R.id.help_moving_row, R.drawable.ic_alarm_plus, R.string.help_moving_title,
                R.string.help_moving_subtitle, R.string.help_moving_body);

        View email = findViewById(R.id.help_email_row);
        ListRow.bind(email, R.drawable.ic_mail, getString(R.string.help_email_title),
                getString(R.string.help_email_subtitle));
        email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Support.email(HelpActivity.this, getString(R.string.help_email_subject),
                        getString(R.string.help_email_body));
            }
        });

        View report = findViewById(R.id.help_report_row);
        ListRow.bind(report, R.drawable.ic_flag, getString(R.string.help_report_title),
                getString(R.string.help_report_subtitle));
        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // No queue given: the report screen asks which one.
                startActivity(new Intent(HelpActivity.this, ReportQueueActivity.class));
            }
        });
    }

    /** A row that opens its explanation in a sheet. */
    private void topic(int rowId, @DrawableRes int icon, @StringRes final int title,
                       @StringRes int subtitle, @StringRes final int body) {
        View row = findViewById(rowId);
        ListRow.bind(row, icon, getString(title), getString(subtitle));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BottomSheetDialog sheet = new BottomSheetDialog(HelpActivity.this);
                View content = getLayoutInflater().inflate(R.layout.sheet_help, null);
                ((TextView) content.findViewById(R.id.help_sheet_title)).setText(title);
                ((TextView) content.findViewById(R.id.help_sheet_body)).setText(body);
                sheet.setContentView(content);
                sheet.show();
            }
        });
    }
}

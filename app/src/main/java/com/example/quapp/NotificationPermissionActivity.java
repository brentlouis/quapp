package com.example.quapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Notification permission (canvas 20). Shown once, over the new ticket, right after the first
 * join on Android 13+ (older versions allow notifications without asking). Either button
 * closes it and leaves the queuer on their ticket.
 */
public class NotificationPermissionActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "com.example.quapp.EXTRA_PERMISSION_TICKET_ID";

    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean granted) {
                    // Allowed or not, the ticket is underneath; no second nag.
                    finish();
                }
            });

    /** Whether to show this after a join: Android 13+, not allowed yet, never asked. */
    public static boolean shouldAsk(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !CalledNotifier.canPost(context)
                && !new Session(context).wasAskedForNotifications();
    }

    public static Intent intent(Context context, String ticketId) {
        Intent intent = new Intent(context, NotificationPermissionActivity.class);
        intent.putExtra(EXTRA_TICKET_ID, ticketId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_permission);
        SystemBars.applyPadding(findViewById(R.id.notify_root));
        new Session(this).setAskedForNotifications();

        findViewById(R.id.notify_preview).setBackground(TicketShapes.spotlightBackground(this));

        // The preview shows the number they were just handed.
        Ticket ticket = ActiveTicketStore.ticket(getIntent().getStringExtra(EXTRA_TICKET_ID));
        String number = getString(R.string.ticket_number_format,
                ticket == null ? 0 : ticket.getTicketNumber());
        ((TextView) findViewById(R.id.notify_preview_title)).setText(
                getString(R.string.notification_called_title, number));

        ((TextView) findViewById(R.id.notify_body)).setText(Html.fromHtml(
                getString(R.string.notify_body), Html.FROM_HTML_MODE_LEGACY));

        findViewById(R.id.notify_allow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    finish();
                }
            }
        });

        findViewById(R.id.notify_not_now).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}

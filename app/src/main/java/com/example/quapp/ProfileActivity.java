package com.example.quapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ProfileActivity extends AppCompatActivity {

    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        SystemBars.applyPadding(findViewById(R.id.profile_root));

        session = new Session(this);

        findViewById(R.id.profile_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        bindUser();
        bindRows();

        findViewById(R.id.profile_logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmLogout();
            }
        });
    }

    private void bindUser() {
        TextView name = findViewById(R.id.profile_name);
        TextView phone = findViewById(R.id.profile_phone);
        TextView role = findViewById(R.id.profile_role);

        name.setText(session.getName() == null
                ? getString(R.string.profile_name_fallback) : session.getName());
        phone.setText(session.getPhone());
        role.setText(session.getRole() == Session.Role.OWNER
                ? R.string.profile_role_owner : R.string.profile_role_queuer);
    }

    private void bindRows() {
        final boolean isOwner = session.getRole() == Session.Role.OWNER;

        // History is a queuer's record of queues they joined; owners have Insights instead.
        View historyRow = findViewById(R.id.profile_history_row);
        historyRow.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        historyRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileActivity.this, HistoryActivity.class));
            }
        });

        TextView switchRow = findViewById(R.id.profile_switch_row);
        switchRow.setText(isOwner
                ? R.string.profile_switch_to_queuer : R.string.profile_switch_to_owner);
        switchRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                session.setRole(isOwner ? Session.Role.QUEUER : Session.Role.OWNER);
                startActivity(session.homeIntent(ProfileActivity.this));
            }
        });
    }

    /** Warns specifically when logging out would also throw away a place in line. */
    private void confirmLogout() {
        String message = ActiveTicketStore.hasLiveTicket()
                ? getString(R.string.profile_logout_ticket_message,
                        ActiveTicketStore.getTicket().getQueueName())
                : getString(R.string.profile_logout_message);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_logout_title)
                .setMessage(message)
                .setNegativeButton(R.string.profile_cancel, null)
                .setPositiveButton(R.string.profile_logout_action,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                logOut();
                            }
                        })
                .show();
    }

    private void logOut() {
        ActiveTicketStore.clearTicket();
        FakeData.clearHistory();
        session.logOut();
        // homeIntent clears the back stack, so Back from Login can't return to this account.
        startActivity(session.homeIntent(this));
    }
}

package com.example.quapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Profile. The last tab on both homes; the rows adapt to the current role. */
public class ProfileFragment extends Fragment {

    private Session session;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = new Session(requireContext());

        // A tab is never "pushed", so there's nothing to go back to.
        view.findViewById(R.id.profile_back).setVisibility(View.GONE);

        bindUser(view);
        bindRows(view);

        view.findViewById(R.id.profile_logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmLogout();
            }
        });
    }

    private void bindUser(View view) {
        TextView name = view.findViewById(R.id.profile_name);
        TextView phone = view.findViewById(R.id.profile_phone);
        TextView role = view.findViewById(R.id.profile_role);

        name.setText(session.getName() == null
                ? getString(R.string.profile_name_fallback) : session.getName());
        phone.setText(session.getPhone());
        role.setText(session.getRole() == Session.Role.OWNER
                ? R.string.profile_role_owner : R.string.profile_role_queuer);
    }

    private void bindRows(View view) {
        final boolean isOwner = session.getRole() == Session.Role.OWNER;

        // History is a queuer's record of queues they joined; owners have Insights instead.
        View historyRow = view.findViewById(R.id.profile_history_row);
        historyRow.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        historyRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireContext(), HistoryActivity.class));
            }
        });

        TextView switchRow = view.findViewById(R.id.profile_switch_row);
        switchRow.setText(isOwner
                ? R.string.profile_switch_to_queuer : R.string.profile_switch_to_owner);
        switchRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                session.setRole(isOwner ? Session.Role.QUEUER : Session.Role.OWNER);
                startActivity(session.homeIntent(requireContext()));
            }
        });
    }

    /** Warns specifically when logging out would also throw away a place in line. */
    private void confirmLogout() {
        String message = ActiveTicketStore.hasLiveTicket()
                ? getString(R.string.profile_logout_ticket_message,
                        ActiveTicketStore.getTicket().getQueueName())
                : getString(R.string.profile_logout_message);

        new MaterialAlertDialogBuilder(requireContext())
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
        startActivity(session.homeIntent(requireContext()));
    }
}

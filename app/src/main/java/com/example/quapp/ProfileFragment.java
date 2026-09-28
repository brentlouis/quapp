package com.example.quapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

/** Profile. The last tab on both homes; the rows adapt to the current role. */
public class ProfileFragment extends Fragment {

    private Session session;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = new Session(requireContext());

        bindUser(view);
        bindSwitchRow(view);
        bindLogoutRow(view);

        view.findViewById(R.id.profile_history_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireContext(), HistoryActivity.class));
            }
        });
    }

    /** The record changes when a ticket finishes, so refresh on every return. */
    @Override
    public void onResume() {
        super.onResume();
        bindRecord();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            bindRecord();
        }
    }

    private void bindUser(View view) {
        String name = session.getName() == null
                ? getString(R.string.profile_name_fallback) : session.getName();
        ((TextView) view.findViewById(R.id.profile_name)).setText(name);
        ((TextView) view.findViewById(R.id.profile_initials)).setText(initials(name));
        ((TextView) view.findViewById(R.id.profile_phone)).setText(spacedPhone(session.getPhone()));
    }

    /**
     * Served and no-show counts plus the history row. Queuers only: History is a record of
     * queues you joined, and an owner's numbers live on the Today tab.
     */
    private void bindRecord() {
        View view = getView();
        if (view == null) {
            return;
        }
        boolean isOwner = session.getRole() == Session.Role.OWNER;
        int visibility = isOwner ? View.GONE : View.VISIBLE;
        view.findViewById(R.id.profile_stats).setVisibility(visibility);
        view.findViewById(R.id.profile_history_row).setVisibility(visibility);
        view.findViewById(R.id.profile_history_divider).setVisibility(visibility);
        if (isOwner) {
            return;
        }

        List<Ticket> history = FakeData.history();
        int served = 0;
        int noShows = 0;
        for (Ticket ticket : history) {
            if (ticket.getStatus() == Ticket.Status.SERVED) {
                served++;
            } else if (ticket.getStatus() == Ticket.Status.NO_SHOW) {
                noShows++;
            }
        }
        ((TextView) view.findViewById(R.id.profile_served)).setText(String.valueOf(served));
        ((TextView) view.findViewById(R.id.profile_no_shows)).setText(String.valueOf(noShows));

        ListRow.bind(view.findViewById(R.id.profile_history_row), R.drawable.ic_history,
                getString(R.string.profile_history_action),
                getResources().getQuantityString(R.plurals.profile_history_count,
                        history.size(), history.size()));
    }

    private void bindSwitchRow(View view) {
        final boolean isOwner = session.getRole() == Session.Role.OWNER;
        View switchRow = view.findViewById(R.id.profile_switch_row);
        ListRow.bind(switchRow, R.drawable.ic_switch,
                getString(isOwner ? R.string.profile_switch_to_queuer : R.string.profile_switch_to_owner),
                getString(isOwner ? R.string.profile_switch_to_queuer_hint : R.string.profile_switch_to_owner_hint));
        switchRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                session.setRole(isOwner ? Session.Role.QUEUER : Session.Role.OWNER);
                startActivity(session.homeIntent(requireContext()));
            }
        });
    }

    /** Log out is a row like the others, in err red and without a chevron: it doesn't open a screen. */
    private void bindLogoutRow(View view) {
        View logoutRow = view.findViewById(R.id.profile_logout_row);
        ListRow.bind(logoutRow, R.drawable.ic_log_out, getString(R.string.profile_logout_action), null);
        int err = ContextCompat.getColor(requireContext(), R.color.err);
        ((TextView) logoutRow.findViewById(R.id.row_title)).setTextColor(err);
        ((ImageView) logoutRow.findViewById(R.id.row_icon)).setColorFilter(err);
        logoutRow.findViewById(R.id.row_chevron).setVisibility(View.GONE);
        logoutRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmLogout();
            }
        });
    }

    /** "Maria Santos" → "MS"; one word gives one letter. */
    private static String initials(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        result.append(words[0].charAt(0));
        if (words.length > 1) {
            result.append(words[words.length - 1].charAt(0));
        }
        return result.toString().toUpperCase(Locale.ROOT);
    }

    /** "09171234567" → "0917 123 4567", the way people read a PH mobile number aloud. */
    private static String spacedPhone(String phone) {
        if (phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 4) + " " + phone.substring(4, 7) + " " + phone.substring(7);
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

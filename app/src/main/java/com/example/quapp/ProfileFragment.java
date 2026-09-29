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
import com.google.android.material.snackbar.Snackbar;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Profile. The last tab on both homes; the rows adapt to the current role. Organizers also see
 * where their verification stands: Get verified, pending (canvas 51), or the badge.
 */
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
        bindHelpRow(view);
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
        bindVerification();
        refreshAccount();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            bindRecord();
            bindVerification();
        }
    }

    private void bindUser(View view) {
        String name = session.getName() == null
                ? getString(R.string.profile_name_fallback) : session.getName();
        ((TextView) view.findViewById(R.id.profile_name)).setText(name);
        ((TextView) view.findViewById(R.id.profile_initials)).setText(Format.initials(name));
        String phone = Format.spacedPhone(session.getPhone());
        // Organizers: "0918 222 3141 · City Health Office" (canvas 51).
        String organization = session.getOrganizationName();
        ((TextView) view.findViewById(R.id.profile_phone)).setText(
                session.getRole() == Session.Role.OWNER && organization != null
                        ? getString(R.string.profile_phone_org_format, phone, organization)
                        : phone);
    }

    /**
     * The badge can change while the app is closed (the admin approves or revokes it), so the
     * account is asked for again each time Profile shows.
     */
    private void refreshAccount() {
        ApiClient.api(requireContext()).me().enqueue(new ApiCallback<User>(requireContext()) {
            @Override
            protected void onSuccess(@Nullable User user) {
                if (user == null || !isAdded()) {
                    return;
                }
                session.update(user);
                bindUser(requireView());
                bindVerification();
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                // Offline: the last copy stays on screen
            }
        });
    }

    /**
     * Organizers only. Not asked (or turned down): a Get verified row. Asked: the pending card
     * with the number the admin will call. Verified: the badge, no chevron. The admin decides
     * on the server's admin page.
     */
    private void bindVerification() {
        View view = getView();
        if (view == null) {
            return;
        }
        View row = view.findViewById(R.id.profile_verify_row);
        View divider = view.findViewById(R.id.profile_verify_divider);
        View pending = view.findViewById(R.id.profile_pending);
        boolean isOwner = session.getRole() == Session.Role.OWNER;
        VerificationStatus status = session.getVerification();

        pending.setVisibility(isOwner && status == VerificationStatus.PENDING ? View.VISIBLE : View.GONE);
        boolean showRow = isOwner && status != VerificationStatus.PENDING;
        row.setVisibility(showRow ? View.VISIBLE : View.GONE);
        divider.setVisibility(showRow ? View.VISIBLE : View.GONE);
        if (!isOwner) {
            return;
        }

        if (status == VerificationStatus.PENDING) {
            bindPendingRequest();
        }

        View chevron = row.findViewById(R.id.row_chevron);
        ImageView icon = row.findViewById(R.id.row_icon);
        if (status == VerificationStatus.VERIFIED) {
            ListRow.bind(row, R.drawable.ic_badge_check, getString(R.string.profile_verified),
                    session.getOrganizationName());
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.ok));
            chevron.setVisibility(View.GONE);
            row.setOnClickListener(null);
            row.setClickable(true);
        } else {
            ListRow.bind(row, R.drawable.ic_badge_check, getString(R.string.profile_get_verified),
                    getString(status == VerificationStatus.REJECTED
                            ? R.string.profile_get_verified_rejected
                            : status == VerificationStatus.REVOKED
                                    ? R.string.profile_get_verified_revoked
                                    : R.string.profile_get_verified_hint));
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.ink_muted));
            chevron.setVisibility(View.VISIBLE);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(requireContext(), GetVerifiedActivity.class));
                }
            });
        }
    }

    /** The pending card: when they asked and the number the admin will call. */
    private void bindPendingRequest() {
        ApiClient.api(requireContext()).myVerification().enqueue(
                new ApiCallback<VerificationRequest>(requireContext()) {
                    @Override
                    protected void onSuccess(@Nullable VerificationRequest request) {
                        View view = getView();
                        if (request == null || view == null) {
                            return;
                        }
                        ((TextView) view.findViewById(R.id.profile_pending_body)).setText(getString(
                                R.string.profile_verification_pending_body,
                                request.getCreatedAt().atZone(Format.MANILA).format(
                                        DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
                                request.getOfficePhone()));
                    }

                    @Override
                    protected void onError(@NonNull ApiError error) {
                        // The card's heading still says it's pending
                    }
                });
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

        // The row works before the count arrives; the counts come from the server's history
        ListRow.bind(view.findViewById(R.id.profile_history_row), R.drawable.ic_history,
                getString(R.string.profile_history_action), null);
        ApiClient.api(requireContext()).myTickets(false).enqueue(
                new ApiCallback<List<Ticket>>(requireContext()) {
                    @Override
                    protected void onSuccess(@Nullable List<Ticket> history) {
                        if (isAdded() && history != null) {
                            bindHistory(view, history);
                        }
                    }

                    @Override
                    protected void onError(@NonNull ApiError error) {
                        // Quietly keep what's shown: the profile works without the counts
                    }
                });
    }

    private void bindHistory(View view, List<Ticket> history) {
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

    /** Help and support (canvas 49), on both sides. */
    private void bindHelpRow(View view) {
        View helpRow = view.findViewById(R.id.profile_help_row);
        ListRow.bind(helpRow, R.drawable.ic_info, getString(R.string.help_title),
                getString(R.string.profile_help_hint));
        helpRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireContext(), HelpActivity.class));
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

    /** Warns specifically when logging out would also throw away a place in line. */
    private void confirmLogout() {
        List<Ticket> live = ActiveTicketStore.liveTickets();
        String message = live.isEmpty() ? getString(R.string.profile_logout_message)
                : live.size() == 1 ? getString(R.string.profile_logout_ticket_message,
                        live.get(0).getQueueName())
                : getString(R.string.profile_logout_tickets_message, live.size());

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
        ActiveTicketStore.clear();
        // Ends the token on the server too. Nothing waits for the answer: signing out on this
        // phone happens either way, even offline.
        ApiClient.api(requireContext()).logout("Bearer " + session.getToken())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable failure) {
                    }
                });
        session.logOut();
        // homeIntent clears the back stack, so Back from Login can't return to this account.
        startActivity(session.homeIntent(requireContext()));
    }
}

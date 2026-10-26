package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** The prototype's compact Profile groups, shared by both roles. */
public class ProfileFragment extends Fragment {
    private LinearLayout content;
    private boolean expanded;
    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        if (state != null) expanded = state.getBoolean("tools_expanded");
        return inflater.inflate(R.layout.fragment_profile, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) { content = view.findViewById(R.id.profile_content);
        view.findViewById(R.id.profile_back).setOnClickListener(v -> {
            boolean owner=new Session(requireContext()).getRole()==Session.Role.OWNER;
            com.google.android.material.bottomnavigation.BottomNavigationView nav=requireActivity().findViewById(owner?R.id.owner_home_nav:R.id.queuer_home_nav);
            nav.setSelectedItemId(owner?R.id.nav_queues:R.id.nav_browse);
        });
    }
    @Override public void onResume() { super.onResume(); render(); }
    @Override public void onHiddenChanged(boolean hidden) { super.onHiddenChanged(hidden); if (!hidden) render(); }
    @Override public void onSaveInstanceState(@NonNull Bundle out) { super.onSaveInstanceState(out); out.putBoolean("tools_expanded", expanded); }
    private void settings(String page) { if ("account".equals(page) || "privacy".equals(page) || "appointments".equals(page)) ProfileSettingsActivity.sheet(requireContext(), page); else startActivity(ProfileSettingsActivity.intent(requireContext(), page)); }
    private void render() {
        if (content == null) return;
        content.removeAllViews();
        Session session = new Session(requireContext());
        boolean verified = session.isLoggedIn() && FakeData.myVerification() == VerificationStatus.VERIFIED;
        View summary = getLayoutInflater().inflate(R.layout.view_profile_summary, content, false);
        String name = session.isLoggedIn() && !Validation.isBlank(session.getName()) ? session.getName() : getString(R.string.profile_guest);
        ((TextView) summary.findViewById(R.id.profile_name)).setText(name);
        ((TextView) summary.findViewById(R.id.profile_initials)).setText(Format.initials(name));
        ((TextView) summary.findViewById(R.id.profile_summary_status)).setText(getString(session.isLoggedIn() ? R.string.profile_account_summary : R.string.profile_guest_summary,
                getString(verified ? R.string.profile_verified_demo : R.string.profile_not_verified)));
        summary.findViewById(R.id.profile_details).setOnClickListener(v -> settings("account"));
        if (getResources().getConfiguration().fontScale >= 1.5f) {
            ((LinearLayout) summary).setOrientation(LinearLayout.VERTICAL);
            View nameBlock = (View) summary.findViewById(R.id.profile_name).getParent();
            nameBlock.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        }
        content.addView(summary);
        ProfileSettingsActivity.row(content, R.string.saved_title, getString(R.string.profile_saved_count, new SavedServices(requireContext()).ids().size()),
                () -> startActivity(new Intent(requireContext(), SavedServicesActivity.class))).setId(R.id.profile_saved_row);
        ProfileSettingsActivity.row(content, R.string.profile_past_visits, getString(R.string.profile_visits_count, FakeData.history().size()),
                () -> startActivity(new Intent(requireContext(), HistoryActivity.class)));
        ProfileSettingsActivity.row(content, R.string.p4_appointments, getString(R.string.profile_upcoming_hint), () -> settings("appointments")).setId(R.id.profile_appointments);
        ProfileSettingsActivity.gap(content);
        boolean reduced = ProfilePreferences.get(requireContext()).getBoolean("reduced", false);
        ProfileSettingsActivity.row(content, R.string.profile_preferences, getString(reduced ? R.string.profile_reduced : R.string.profile_system_motion), () -> settings("preferences"));
        ProfileSettingsActivity.row(content, R.string.profile_notifications, getString(ProfilePreferences.get(requireContext()).getBoolean("alerts", false) ? R.string.profile_alerts_on : R.string.profile_alerts_off), () -> settings("notifications"));
        ProfileSettingsActivity.row(content, R.string.profile_verification, getString(verified ? R.string.profile_verified_session : R.string.profile_verification_hint), () -> settings("verification"));
        ProfileSettingsActivity.row(content, R.string.profile_help_reports, getString(R.string.profile_reports_count, ProfilePreferences.reports(requireContext()).size()), () -> settings("help"));
        ProfileSettingsActivity.gap(content);
        ProfileSettingsActivity.disclosure(content, R.string.profile_tools, expanded, body -> {
            ProfileSettingsActivity.row(body, R.string.profile_privacy, null, () -> settings("privacy"));
            ProfileSettingsActivity.row(body, R.string.profile_organizer_today, null, () -> {
                if (!session.isLoggedIn()) { startActivity(new Intent(requireContext(), LoginActivity.class)); return; }
                session.setRole(Session.Role.OWNER); startActivity(session.homeIntent(requireContext()).putExtra("owner_tab", "today"));
            });
            ProfileSettingsActivity.row(body, R.string.profile_staff_roles, null, () -> settings("staff"));
            if (session.getRole() == Session.Role.OWNER) ProfileSettingsActivity.row(body, R.string.profile_customer_view, null, () -> {
                session.setRole(Session.Role.QUEUER); startActivity(session.homeIntent(requireContext()));
            });
        }, value -> expanded = value);
        ProfileSettingsActivity.button(content, session.isLoggedIn() ? R.string.profile_sign_out : R.string.profile_sign_in, 2, () -> {
            if (!session.isLoggedIn()) { startActivity(new Intent(requireContext(), LoginActivity.class)); return; }
            new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_logout_title)
                    .setMessage(ActiveTicketStore.liveTickets().isEmpty() ? R.string.profile_logout_message : R.string.profile_logout_tickets_warning)
                    .setNegativeButton(R.string.profile_cancel, null).setPositiveButton(R.string.profile_logout_action, (d, w) -> {
                        ActiveTicketStore.clear(); FakeData.clearHistory(); endServerSession(session); session.logOut(); startActivity(session.homeIntent(requireContext()));
                    }).show();
        });
    }

    /**
     * Ends the token on the server too. Nothing waits for the answer: signing out on this phone
     * happens either way, even offline.
     */
    private void endServerSession(Session session) {
        ApiClient.api(requireContext()).logout("Bearer " + session.getToken())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable failure) {
                    }
                });
    }
}

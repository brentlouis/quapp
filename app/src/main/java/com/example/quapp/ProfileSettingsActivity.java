package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Profile destinations reuse native rows, fields, dialogs and existing feature screens. */
public class ProfileSettingsActivity extends AppCompatActivity {
    private LinearLayout content;
    private String page;
    private String helpQuery = "";
    static Intent intent(Context context, String page) { return new Intent(context, ProfileSettingsActivity.class).putExtra("profile_page", page); }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_profile_settings);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.profile_settings_root));
        content = findViewById(R.id.profile_settings_content);
        page = getIntent().getStringExtra("profile_page");
        if (state != null) helpQuery = state.getString("help_query", "");
        findViewById(R.id.profile_settings_back).setOnClickListener(v -> finish());
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.profile_settings_nav);
        boolean owner = new Session(this).getRole() == Session.Role.OWNER;
        nav.inflateMenu(owner ? R.menu.menu_owner_home : R.menu.menu_queuer_home);
        nav.setSelectedItemId(R.id.nav_profile);
        nav.setOnItemSelectedListener(item -> {
            if (owner) startActivity(new Intent(this, OwnerHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("owner_tab", item.getItemId() == R.id.nav_profile ? "profile" : item.getItemId() == R.id.nav_today ? "today" : "queues"));
            else startActivity(QueuerHomeActivity.intent(this, item.getItemId() == R.id.nav_profile ? QueuerHomeActivity.TAB_PROFILE
                    : item.getItemId() == R.id.nav_tickets ? QueuerHomeActivity.TAB_TICKETS : QueuerHomeActivity.TAB_BROWSE));
            finish(); return true;
        });
        render();
    }
    @Override protected void onResume() { super.onResume(); if (content != null && ("help".equals(page) || "verification".equals(page))) render(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("help_query", helpQuery); super.onSaveInstanceState(state); }
    private void go(String next) { if ("privacy".equals(next)) sheet(this, next); else startActivity(intent(this, next)); }
    private void title(int id) { ((TextView) findViewById(R.id.profile_settings_title)).setText(id); }
    private void render() {
        content.removeAllViews();
        if ("preferences".equals(page)) {
            title(R.string.profile_preferences);
            toggle("reduced", R.string.profile_reduce_motion, R.string.profile_reduce_motion_hint, false);
            String order = ProfilePreferences.get(this).getString("order", "Recommended");
            if ("Shortest wait".equals(order)) order = "Shortest demo wait";
            choice(R.string.profile_service_order, order, new String[]{"Recommended", "Name", "Shortest demo wait"}, value -> { ProfilePreferences.get(this).edit().putString("order", value).apply(); render(); });
        } else if ("notifications".equals(page)) {
            title(R.string.profile_notifications);
            toggle("alerts", R.string.profile_demo_alerts, R.string.profile_alerts_hint, false);
            toggle("queueAlerts", R.string.profile_queue_calls, R.string.profile_queue_calls_hint, true);
            text(content, R.string.profile_no_real_alert, false);
            List<Queue> updates = new ArrayList<>();
            for (Queue queue : FakeData.queues()) if (!Validation.isBlank(FakeData.announcement(queue.getId()))) updates.add(queue);
            int unread = 0;
            for (Queue queue : updates) if (!isRead(queue)) unread++;
            disclosure(content, getString(R.string.profile_recent_updates, unread), true, body -> {
                if (updates.isEmpty()) text(body, R.string.profile_no_updates, false);
                for (Queue queue : updates) {
                    text(body, queue.getName(), true); text(body, FakeData.announcement(queue.getId()), false);
                    if (isRead(queue)) text(body, R.string.profile_read, false);
                    else row(body, R.string.profile_mark_read, null, () -> { ProfilePreferences.get(this).edit().putString("read:" + queue.getId(), FakeData.announcement(queue.getId())).apply(); render(); });
                }
            }, value -> { });
        } else if ("help".equals(page)) {
            title(R.string.profile_help_title); help();
        } else if ("verification".equals(page)) {
            title(R.string.profile_verification); text(content, R.string.profile_verification_heading, true);
            text(content, new Session(this).isLoggedIn() ? new Session(this).getVerification().name() : getString(R.string.profile_not_verified), false); text(content, R.string.profile_verification_body, false);
            row(content, R.string.profile_verification_action, null, () -> {
                Session session = new Session(this);
                if (!session.isLoggedIn()) startActivity(new Intent(this, LoginActivity.class));
                else startActivity(new Intent(this, GetVerifiedActivity.class));
            });
        } else if ("receipt".equals(page)) {
            title(R.string.profile_receipt);
            // The ticket comes from the server (one of the account's own, finished or not)
            String receiptId = getIntent().getStringExtra("receipt_ticket_id");
            if (receiptId == null) text(content, R.string.profile_receipt_unavailable, false);
            else Repository.run(this, ApiClient.api(this).ticket(receiptId), new Repository.Callback<Ticket>() {
                @Override public void onSuccess(Ticket selected) {
                    if (isFinishing() || isDestroyed()) return;
                    if (selected == null) { text(content, R.string.profile_receipt_unavailable, false); return; }
                    text(content, selected.getQueueName(), true);
                    text(content, getString(R.string.ticket_number_format, selected.getTicketNumber()), true);
                    text(content, selected.getStatus().name().replace('_', ' '), false);
                    text(content, selected.getVenue(), false);
                    if (selected.getFinishedAt() != null) text(content, Format.day(ProfileSettingsActivity.this, selected.getFinishedAt()), false);
                    text(content, R.string.profile_receipt_boundary, false);
                }
                @Override public void onError(ApiError error) {
                    if (!isFinishing() && !isDestroyed()) text(content, R.string.profile_receipt_unavailable, false);
                }
            });
            row(content, R.string.profile_past_visits, null, this::finish);
            row(content, R.string.profile_find_service, null, () -> startActivity(QueuerHomeActivity.intent(this, QueuerHomeActivity.TAB_BROWSE)));
        } else if ("staff".equals(page)) {
            title(R.string.profile_staff_roles); text(content, R.string.profile_staff_hint, false);
            Session session = new Session(this);
            if (session.isLoggedIn() && session.getRole() == Session.Role.OWNER) {
                for (Queue queue : FakeData.queues()) if (FakeData.ownsQueue(queue.getId())) row(content, queue.getName(), null, () -> startActivity(Phase4Activity.ownerIntent(this, Phase4Activity.STAFF, queue.getId())));
            } else row(content, R.string.profile_organizer_today, null, () -> {
                if (!session.isLoggedIn()) startActivity(new Intent(this, LoginActivity.class));
                else { session.setRole(Session.Role.OWNER); startActivity(session.homeIntent(this)); }
            });
        } else finish();
    }
    private boolean isRead(Queue queue) { return FakeData.announcement(queue.getId()).equals(ProfilePreferences.get(this).getString("read:" + queue.getId(), "")); }
    private void toggle(String key, int label, int hint, boolean fallback) {
        MaterialSwitch control = new MaterialSwitch(new androidx.appcompat.view.ContextThemeWrapper(this, R.style.ThemeOverlay_Quapp_ProfileSwitch));
        android.text.SpannableString copy = new android.text.SpannableString(getString(label) + "\n" + getString(hint));
        copy.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, getString(label).length(), 0);
        control.setText(copy); control.setTextAppearance(R.style.TextAppearance_Quapp_Body); control.setMinHeight(dp(this, 48));
        control.setChecked(ProfilePreferences.get(this).getBoolean(key, fallback));
        control.setOnCheckedChangeListener((v, checked) -> ProfilePreferences.get(this).edit().putBoolean(key, checked).apply());
        content.addView(control, new LinearLayout.LayoutParams(-1, -2)); gap(content);
    }
    private void choice(int label, String value, String[] values, Consumer<String> changed) {
        text(content, label, false);
        MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(value); button.setContentDescription(getString(label) + ": " + value); button.setMinHeight(dp(this, 48));
        button.setCornerRadius(dp(this, 12)); button.setStrokeWidth(dp(this, 1));
        button.setTextAppearance(R.style.TextAppearance_Quapp_Body);
        button.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
        button.setIconResource(R.drawable.ic_chevron_down); button.setIconGravity(MaterialButton.ICON_GRAVITY_END);
        button.setOnClickListener(v -> new MaterialAlertDialogBuilder(this).setTitle(label).setSingleChoiceItems(values, java.util.Arrays.asList(values).indexOf(value), (d, which) -> { d.dismiss(); changed.accept(values[which]); }).setNegativeButton(R.string.profile_cancel, null).show());
        content.addView(button, new LinearLayout.LayoutParams(-1, -2)); gap(content);
    }
    private void help() {
        TextInputEditText search = field(content, R.string.profile_find_help);
        search.setText(helpQuery);
        search.setHint(R.string.profile_help_placeholder); search.setSingleLine(true); search.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        LinearLayout topics = new LinearLayout(this); topics.setOrientation(LinearLayout.VERTICAL); content.addView(topics);
        Consumer<String> filter = query -> {
            topics.removeAllViews();
            int[] labels = {R.string.profile_faq_queue, R.string.profile_faq_offline, R.string.profile_faq_missed, R.string.profile_faq_privacy, R.string.profile_faq_alerts};
            int[] answers = {R.string.profile_faq_queue_body, R.string.profile_faq_offline_body, R.string.profile_faq_missed_body, R.string.profile_faq_privacy_body, R.string.profile_faq_alerts_body};
            for (int i = 0; i < labels.length; i++) {
                int answer = answers[i];
                if ((getString(labels[i]) + " " + getString(answer)).toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT))) disclosure(topics, labels[i], false, body -> text(body, answer, false), value -> { });
            }
            if (topics.getChildCount() == 0) { text(topics, R.string.profile_no_help, false); row(topics, R.string.profile_clear_search, null, () -> search.setText("")); }
        };
        search.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s, int start, int count, int after) { } public void onTextChanged(CharSequence s, int start, int before, int count) { helpQuery = s.toString(); filter.accept(helpQuery); } public void afterTextChanged(Editable e) { } });
        filter.accept(helpQuery);
        row(content, R.string.profile_reconnect, null, () -> { FakeData.setDebugMode(false); new MaterialAlertDialogBuilder(this).setMessage(R.string.profile_reconnected).setPositiveButton(android.R.string.ok, null).show(); });
        row(content, R.string.profile_ticket_recovery, null, () -> startActivity(QueuerHomeActivity.intent(this, QueuerHomeActivity.TAB_TICKETS)));
            button(content, R.string.profile_report_concern, 1, () -> {
            if (!new Session(this).isLoggedIn()) { startActivity(new Intent(this, LoginActivity.class)); return; }
            List<Queue> queues = FakeData.queues();
            if (queues.isEmpty()) { new MaterialAlertDialogBuilder(this).setMessage(R.string.profile_no_services).setPositiveButton(android.R.string.ok, null).show(); return; }
            String[] names = new String[queues.size()]; for (int i = 0; i < names.length; i++) names[i] = queues.get(i).getName();
            new MaterialAlertDialogBuilder(this).setTitle(R.string.profile_choose_service).setItems(names, (d, which) -> startActivity(ReportQueueActivity.intent(this, queues.get(which).getId()))).setNegativeButton(R.string.profile_cancel, null).show();
        });
        List<Report> reports = ProfilePreferences.reports(this);
        disclosure(content, getString(R.string.profile_my_reports, reports.size()), false, body -> {
            if (reports.isEmpty()) text(body, R.string.profile_no_reports, false);
            for (int i = reports.size() - 1; i >= 0; i--) { Report report = reports.get(i); Queue queue = FakeData.queueById(report.getQueueId()); text(body, queue == null ? getString(R.string.profile_unavailable_service) : queue.getName(), true); text(body, report.getReason().label, false); text(body, getString(R.string.profile_report_status, report.getCreatedAt().toString()), false); }
        }, value -> { });
        text(content, R.string.profile_help_boundary, false);
    }
    static void sheet(Context context, String page) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(context);
        LinearLayout body = new LinearLayout(context); body.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 24); body.setPadding(padding, padding, padding, padding); body.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.paper_raised)); scroll.addView(body); dialog.setContentView(scroll);
        LinearLayout header = new LinearLayout(context); header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView simulated = new TextView(context); simulated.setText(R.string.profile_simulated); simulated.setTextAppearance(R.style.TextAppearance_Quapp_BodyStrong);
        header.addView(simulated, new LinearLayout.LayoutParams(0, -2, 1));
        android.widget.ImageButton close = new android.widget.ImageButton(context); close.setImageResource(R.drawable.ic_circle_x);
        close.setContentDescription(context.getString(R.string.profile_close_dialog)); close.setBackgroundResource(android.R.color.transparent);
        close.setOnClickListener(v -> dialog.dismiss()); header.addView(close, new LinearLayout.LayoutParams(dp(context, 48), dp(context, 48))); body.addView(header);
        if ("account".equals(page)) {
            text(body, R.string.profile_account_details_heading, true); text(body, R.string.profile_account_details_hint, false);
            Session session = new Session(context);
            TextInputEditText name = field(body, R.string.register_name_label); name.setText(session.isLoggedIn() && !Validation.isBlank(session.getName()) ? session.getName() : context.getString(R.string.profile_guest)); name.setKeyListener(null); name.setFocusable(false);
            TextInputEditText email = field(body, R.string.profile_email); email.setText(R.string.profile_not_provided); email.setKeyListener(null); email.setFocusable(false);
            TextInputEditText phone = field(body, R.string.login_phone_label); phone.setText(session.isLoggedIn() ? session.getPhone() : context.getString(R.string.profile_not_provided)); phone.setKeyListener(null); phone.setFocusable(false);
            row(body, R.string.profile_verification_status, null, () -> { dialog.dismiss(); context.startActivity(intent(context, "verification")); });
        } else if ("privacy".equals(page)) {
            text(body, R.string.profile_privacy_heading, true); text(body, R.string.profile_privacy_body, false);
            MaterialButton clear = button(body, R.string.profile_clear_data_ellipsis, 0, () -> {
                dialog.dismiss();
                new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Quapp_Dialog_WalkIn).setTitle(R.string.profile_clear_data_title).setMessage(R.string.profile_clear_data_body)
                        .setNegativeButton(R.string.profile_cancel, null).setPositiveButton(R.string.profile_clear_data, (d, w) -> { ProfilePreferences.clear(context); if (context instanceof android.app.Activity) ((android.app.Activity) context).recreate(); }).show();
            });
            android.content.res.ColorStateList destructive = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(context, R.color.err));
            clear.setTextColor(destructive);
            clear.setStrokeColor(destructive);
        } else if ("appointments".equals(page)) {
            text(body, R.string.profile_upcoming_heading, true); text(body, R.string.profile_no_appointments, false);
            row(body, R.string.profile_explore_appointments, null, () -> { dialog.dismiss(); context.startActivity(Phase4Activity.intent(context, Phase4Activity.APPOINTMENTS)); });
        }
        button(body, R.string.profile_cancel, 0, dialog::dismiss);
        simulated.setBackgroundResource(R.drawable.bg_pill); simulated.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.signal)));
        simulated.setPadding(dp(context,12),dp(context,4),dp(context,12),dp(context,4));
        header.getChildAt(0).setLayoutParams(new LinearLayout.LayoutParams(-2,-2));
        View spacer=new View(context); header.addView(spacer,1,new LinearLayout.LayoutParams(0,1,1));
        QuappMotion.sheet(dialog);
        dialog.setOnShowListener(ignored -> {
            View frame=dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if(frame!=null){
                com.google.android.material.shape.MaterialShapeDrawable shape=new com.google.android.material.shape.MaterialShapeDrawable(com.google.android.material.shape.ShapeAppearanceModel.builder().setTopLeftCornerSize(dp(context,24)).setTopRightCornerSize(dp(context,24)).build());
                shape.setFillColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context,R.color.paper_raised))); frame.setBackground(shape); frame.setClipToOutline(true);
            }
            dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        });
        dialog.getBehavior().setMaxHeight((int)(context.getResources().getDisplayMetrics().heightPixels*.9f));
        dialog.show();
    }
    static MaterialButton button(LinearLayout parent, int label, int kind, Runnable action) {
        MaterialButton button=(MaterialButton)android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.view_phase4_button,parent,false);
        button.setText(label); button.setOnClickListener(v -> action.run());
        if(kind!=0){
            button.setStrokeWidth(0);
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(parent.getContext(),kind==1?R.color.primary:android.R.color.transparent)));
            button.setTextColor(androidx.core.content.ContextCompat.getColor(parent.getContext(),kind==1?R.color.on_primary:R.color.primary_text));
        }
        parent.addView(button); return button;
    }
    static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    static void gap(LinearLayout parent) { View gap = new View(parent.getContext()); parent.addView(gap, new LinearLayout.LayoutParams(1, dp(parent.getContext(), 8))); }
    static TextView text(LinearLayout parent, int text, boolean heading) { return text(parent, parent.getContext().getString(text), heading); }
    static TextView text(LinearLayout parent, CharSequence value, boolean heading) {
        TextView view = new TextView(parent.getContext()); view.setTextAppearance(heading ? R.style.TextAppearance_Quapp_HeadingSmall : R.style.TextAppearance_Quapp_Body);
        view.setText(value); ViewCompat.setAccessibilityHeading(view, heading); parent.addView(view, new LinearLayout.LayoutParams(-1, -2)); gap(parent); return view;
    }
    static View row(LinearLayout parent, int title, String subtitle, Runnable action) { return row(parent, parent.getContext().getString(title), subtitle, action); }
    static View row(LinearLayout parent, String title, String subtitle, Runnable action) {
        View row = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.view_list_row, parent, false);
        ListRow.bind(row, R.drawable.ic_info, title, subtitle);
        row.findViewById(R.id.row_icon).setVisibility(View.GONE); row.findViewById(R.id.row_chevron).setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams label = (LinearLayout.LayoutParams) ((View) row.findViewById(R.id.row_title).getParent()).getLayoutParams(); label.setMarginStart(0);
        ((TextView) row.findViewById(R.id.row_title)).setTextAppearance(R.style.TextAppearance_Quapp_ListPrimary);
        TextView supporting = row.findViewById(R.id.row_subtitle);
        supporting.setTextAppearance(R.style.TextAppearance_Quapp_Body);
        supporting.setTextColor(androidx.core.content.ContextCompat.getColor(parent.getContext(), R.color.ink_muted));
        androidx.core.widget.TextViewCompat.setLineHeight(supporting, parent.getResources().getDimensionPixelSize(R.dimen.type_20_sp));
        row.setPadding(0, dp(parent.getContext(), 6), 0, dp(parent.getContext(), 6));
        row.setMinimumHeight(dp(parent.getContext(), 48)); row.setFocusable(true); ViewCompat.setScreenReaderFocusable(row, true);
        row.setOnClickListener(v -> action.run()); parent.addView(row); return row;
    }
    static void disclosure(LinearLayout parent, int title, boolean expanded, Consumer<LinearLayout> fill, Consumer<Boolean> changed) { disclosure(parent, parent.getContext().getString(title), expanded, fill, changed); }
    static void disclosure(LinearLayout parent, String title, boolean expanded, Consumer<LinearLayout> fill, Consumer<Boolean> changed) {
        LinearLayout body = new LinearLayout(parent.getContext()); body.setOrientation(LinearLayout.VERTICAL);
        View toggle = row(parent, title, null, () -> { boolean show = body.getVisibility() != View.VISIBLE; body.setVisibility(show ? View.VISIBLE : View.GONE); View toggleView = (View) body.getTag(); toggleView.findViewById(R.id.row_chevron).setRotation(show ? 270 : 90); ViewCompat.setStateDescription(toggleView, parent.getContext().getString(show ? R.string.profile_expanded : R.string.profile_collapsed)); changed.accept(show); });
        toggle.findViewById(R.id.row_chevron).setRotation(expanded ? 270 : 90);
        body.setTag(toggle); ViewCompat.setStateDescription(toggle, parent.getContext().getString(expanded ? R.string.profile_expanded : R.string.profile_collapsed));
        parent.addView(body); fill.accept(body); body.setVisibility(expanded ? View.VISIBLE : View.GONE);
    }
    private static TextInputEditText field(LinearLayout parent, int label) {
        TextView caption = text(parent, label, false);
        TextInputLayout layout = new TextInputLayout(parent.getContext()); layout.setHintEnabled(false); layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        int radius=dp(parent.getContext(),12); layout.setBoxCornerRadii(radius,radius,radius,radius);
        TextInputEditText input = new TextInputEditText(layout.getContext()); input.setId(View.generateViewId()); caption.setLabelFor(input.getId()); input.setMinHeight(dp(parent.getContext(),48));
        input.setHintTextColor(androidx.core.content.ContextCompat.getColor(parent.getContext(), R.color.ink_muted));
        layout.addView(input, new LinearLayout.LayoutParams(-1, -2)); parent.addView(layout, new LinearLayout.LayoutParams(-1, -2)); gap(parent); return input;
    }
}

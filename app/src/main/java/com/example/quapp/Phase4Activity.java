package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/** Phase 4 frontend drafts. No reservation, group-ticket or membership mutation exists here. */
public class Phase4Activity extends AppCompatActivity {
    public static final String APPOINTMENTS = "appointments", FAMILY = "family", STAFF = "staff";
    private static final String FEATURE = "phase4_feature", QUEUE = "phase4_queue", OWNER = "phase4_owner";
    private String feature, queueId, date = "", time = "", holder = "", members = "";
    private int stage, role = -1;
    private boolean owner;
    private LinearLayout content;
    private final int[] roleLabels = {R.string.p4_operator, R.string.p4_manager, R.string.p4_viewer};
    private final int[] roleBodies = {R.string.p4_operator_body, R.string.p4_manager_body, R.string.p4_viewer_body};

    public static Intent intent(Context context, String feature) {
        return new Intent(context, Phase4Activity.class).putExtra(FEATURE, feature);
    }
    public static Intent ownerIntent(Context context, String feature, String queueId) {
        return intent(context, feature).putExtra(QUEUE, queueId).putExtra(OWNER, true);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        feature = getIntent().getStringExtra(FEATURE);
        queueId = getIntent().getStringExtra(QUEUE);
        owner = getIntent().getBooleanExtra(OWNER, false);
        if (!APPOINTMENTS.equals(feature) && !FAMILY.equals(feature) && !STAFF.equals(feature)) { finish(); return; }
        if (state != null) {
            queueId = state.getString(QUEUE, queueId); stage = state.getInt("stage");
            date = state.getString("date", ""); time = state.getString("time", "");
            holder = state.getString("holder", ""); members = state.getString("members", ""); role = state.getInt("role", -1);
        }
        setContentView(R.layout.activity_phase4);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.p4_root));
        content = findViewById(R.id.p4_content);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { back(); }
        });
        reconnectPickers();
    }
    @Override protected void onResume() {
        super.onResume();
        if (content == null) return;
        if ((owner || STAFF.equals(feature)) && (!owner || queueId == null || !FakeData.ownsQueue(queueId))) { finish(); return; }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString(QUEUE, queueId); out.putInt("stage", stage); out.putString("date", date); out.putString("time", time);
        out.putString("holder", holder); out.putString("members", members); out.putInt("role", role);
        super.onSaveInstanceState(out);
    }
    private void back() { if (stage == 0) finish(); else go(stage == 1 || stage == 3 ? 0 : stage == 4 ? 3 : 1); }
    private void go(int next) {
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(content.getWindowToken(), 0);
        stage = next; render(); ((NestedScrollView)findViewById(R.id.p4_scroll)).scrollTo(0, 0);
    }
    private void render() {
        content.removeAllViews();
        button(R.string.p4_back, this::back, true);
        text(getString(APPOINTMENTS.equals(feature) ? R.string.p4_appointments : FAMILY.equals(feature) ? R.string.p4_family : R.string.p4_staff), true);
        if (!FAMILY.equals(feature)) text(getString(R.string.p4_boundary), false);
        if (queueId != null) {
            Queue q = FakeData.queueById(queueId);
            if (q != null) text(getString(R.string.p4_queue, q.getName()), false);
        }
        if (stage == 0) {
            if (!FAMILY.equals(feature)) text(getString(R.string.p4_local), false);
            if (APPOINTMENTS.equals(feature)) {
                button(R.string.p4_services, owner ? () -> go(1) : this::chooseQueue, true);
                button(R.string.p4_upcoming, () -> go(3), true);
            } else if (FAMILY.equals(feature)) {
                text(getString(R.string.family_planning_only), false);
                text(getString(R.string.p4_family_body), false);
                button(R.string.p4_family_intro, () -> go(1), true);
                button(R.string.p4_family_status, () -> go(3), true);
            } else {
                text(getString(R.string.p4_staff_body), false);
                button(R.string.p4_members_list, () -> go(3), true);
                button(R.string.p4_invite_draft, () -> go(1), true);
            }
        } else if (stage == 3 || stage == 4) {
            status();
        } else if (APPOINTMENTS.equals(feature)) appointments();
        else if (FAMILY.equals(feature)) family();
        else staff();
    }
    private void appointments() {
        if (stage == 1) {
            if (!owner) button(R.string.p4_services, this::chooseQueue, true);
            text(getString(R.string.p4_availability), true);
            text(getString(R.string.p4_availability_body), false);
            button(R.string.p4_date, this::pickDate, true);
            button(R.string.p4_time, this::pickTime, true);
            preferences();
            button(R.string.p4_review, () -> {
                if (queueId == null || FakeData.queueById(queueId) == null) notice(R.string.p4_choose);
                else go(2);
            }, true);
        } else {
            text(getString(R.string.p4_draft_summary), true); preferences();
            text(getString(R.string.p4_availability_body), false);
            button(R.string.p4_reserve, null, false);
            button(R.string.p4_edit, () -> go(1), true);
            button(R.string.p4_upcoming, () -> go(3), true);
        }
    }
    private void preferences() {
        text(getString(R.string.p4_date_value, date.isEmpty() ? getString(R.string.p4_not_selected) : date), false);
        text(getString(R.string.p4_time_value, time.isEmpty() ? getString(R.string.p4_not_selected) : time), false);
    }
    private void family() {
        text(getString(R.string.p4_family_body), false);
        if (stage == 1) {
            TextInputLayout holderField = field(R.string.p4_holder, holder, value -> holder = value);
            TextInputLayout membersField = field(R.string.p4_members, members, value -> members = value);
            text(getString(R.string.p4_member_help), false);
            button(R.string.p4_review, () -> {
                holderField.setError(Validation.isBlank(holder) ? getString(R.string.p4_holder_required) : null);
                membersField.setError(Validation.isBlank(members) ? getString(R.string.p4_members_required) : null);
                if (Phase4Draft.canReviewGroup(holder, members)) go(2);
                else (Validation.isBlank(holder) ? holderField : membersField).getEditText().requestFocus();
            }, true);
        } else {
            text(getString(R.string.p4_family_review), true);
            text(getString(R.string.p4_group_summary, holder, members), false);
            button(R.string.p4_create_group, null, false);
            button(R.string.p4_edit, () -> go(1), true);
            button(R.string.p4_group_manage, () -> go(4), true);
        }
    }
    private void staff() {
        text(getString(R.string.p4_staff_body), false);
        text(getString(R.string.p4_roles_body), false);
        if (stage == 1) {
            button(R.string.p4_role, () -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.p4_role).setSingleChoiceItems(new String[]{getString(roleLabels[0]),getString(roleLabels[1]),getString(roleLabels[2])},role,
                    (dialog, which) -> { role = which; dialog.dismiss(); render(); }).show(), true);
            showRole();
            text(getString(R.string.p4_recipient), false);
            button(R.string.p4_review, () -> go(2), true);
        } else {
            text(getString(R.string.p4_draft_summary), true); showRole();
            text(getString(R.string.p4_recipient), false);
            button(R.string.p4_invite, null, false);
            button(R.string.p4_edit, () -> go(1), true);
            button(R.string.p4_invitation_status, () -> go(3), true);
        }
    }
    private void showRole() {
        text(getString(R.string.p4_role_value, role < 0 ? getString(R.string.p4_not_selected) : getString(roleLabels[role])), false);
        if (role >= 0) text(getString(roleBodies[role]), false);
    }
    private void status() {
        boolean appointment = APPOINTMENTS.equals(feature), family = FAMILY.equals(feature);
        text(getString(appointment ? R.string.p4_upcoming : family ? R.string.p4_family_status : R.string.p4_members_list), true);
        text(getString(appointment ? R.string.p4_no_appointments : family ? R.string.p4_no_group : R.string.p4_no_staff), false);
        if (stage == 3) button(appointment ? R.string.p4_details : family ? R.string.p4_group_manage : R.string.p4_invitation_status, () -> go(4), true);
        else if (appointment) {
            text(getString(R.string.p4_appointment_manage), false);
            button(R.string.p4_cancel_booking, null, false); button(R.string.p4_reschedule, null, false);
            button(R.string.p4_edit, () -> go(1), true);
        } else if (family) {
            text(getString(R.string.p4_group_manage_body), false);
            button(R.string.p4_edit, () -> go(1), true);
            button(R.string.p4_clear, () -> { holder = ""; members = ""; go(1); }, true);
            button(R.string.p4_leave_group, null, false);
        } else {
            text(getString(R.string.p4_staff_manage), false);
            button(R.string.p4_change_role, null, false); button(R.string.p4_revoke, null, false);
        }
        button(R.string.p4_retry, () -> notice(R.string.p4_retry_message), true);
    }
    private void chooseQueue() {
        List<Queue> queues = FakeData.queues();
        if (queues.isEmpty()) { notice(R.string.p4_no_services); return; }
        String[] names = new String[queues.size()];
        int selected = -1;
        for (int i = 0; i < queues.size(); i++) { names[i] = queues.get(i).getName(); if (queues.get(i).getId().equals(queueId)) selected = i; }
        new MaterialAlertDialogBuilder(this).setTitle(R.string.p4_services).setSingleChoiceItems(names, selected,
                (dialog, which) -> { queueId = queues.get(which).getId(); dialog.dismiss(); go(1); })
                .setNegativeButton(R.string.profile_cancel, null).show();
    }
    private void pickDate() {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.p4_date);
        if (!date.isEmpty()) builder.setSelection(java.time.LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
        MaterialDatePicker<Long> picker = builder.build();
        dateListener(picker); picker.show(getSupportFragmentManager(), "p4_date_picker");
    }
    private void pickTime() {
        MaterialTimePicker.Builder builder = new MaterialTimePicker.Builder().setTitleText(R.string.p4_time)
                .setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H);
        if (!time.isEmpty()) {
            java.time.LocalTime selected = java.time.LocalTime.parse(time);
            builder.setHour(selected.getHour()).setMinute(selected.getMinute());
        }
        MaterialTimePicker picker = builder.build();
        timeListener(picker); picker.show(getSupportFragmentManager(), "p4_time_picker");
    }
    @SuppressWarnings("unchecked") private void reconnectPickers() {
        androidx.fragment.app.Fragment datePicker = getSupportFragmentManager().findFragmentByTag("p4_date_picker");
        if (datePicker instanceof MaterialDatePicker) dateListener((MaterialDatePicker<Long>) datePicker);
        androidx.fragment.app.Fragment timePicker = getSupportFragmentManager().findFragmentByTag("p4_time_picker");
        if (timePicker instanceof MaterialTimePicker) timeListener((MaterialTimePicker) timePicker);
    }
    private void dateListener(MaterialDatePicker<Long> picker) {
        picker.addOnPositiveButtonClickListener(value -> { date = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate().toString(); render(); });
    }
    private void timeListener(MaterialTimePicker picker) {
        picker.addOnPositiveButtonClickListener(v -> { time = java.time.LocalTime.of(picker.getHour(), picker.getMinute()).toString(); render(); });
    }
    private interface Changed { void set(String value); }
    private TextInputLayout field(int hint, String value, Changed changed) {
        TextView label = text(getString(hint), false);
        TextInputLayout field = (TextInputLayout)getLayoutInflater().inflate(R.layout.view_phase4_field, content, false);
        field.getEditText().setId(View.generateViewId());
        label.setLabelFor(field.getEditText().getId());
        field.setHelperText(getString(R.string.p4_draft_limit));
        field.getEditText().setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(Phase4Draft.TEXT_LIMIT)});
        field.getEditText().setText(value);
        field.getEditText().addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { changed.set(s.toString()); field.setError(null); }
            public void afterTextChanged(Editable s) { }
        });
        content.addView(field);
        return field;
    }
    private void button(int label, Runnable action, boolean enabled) {
        boolean primary = label == R.string.p4_review || label == R.string.p4_invite_draft
                || label == R.string.p4_family_intro || label == R.string.p4_invite
                || label == R.string.p4_reserve || label == R.string.p4_create_group
                || (label == R.string.p4_services && stage == 0);
        MaterialButton button = (MaterialButton)getLayoutInflater().inflate(
                label == R.string.p4_back ? R.layout.view_phase4_back_button
                        : primary ? R.layout.view_phase4_primary_button : R.layout.view_phase4_button, content, false);
        button.setText(label); button.setEnabled(enabled);
        if (!enabled) button.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ink_muted));
        if (action != null) button.setOnClickListener(v -> action.run());
        content.addView(button);
    }
    private TextView text(String value, boolean heading) {
        TextView text = new TextView(this); text.setText(value);
        android.util.TypedValue appearance = new android.util.TypedValue();
        getTheme().resolveAttribute(heading ? com.google.android.material.R.attr.textAppearanceTitleLarge : com.google.android.material.R.attr.textAppearanceBodyLarge, appearance, true);
        text.setTextAppearance(appearance.resourceId);
        androidx.core.view.ViewCompat.setAccessibilityHeading(text, heading);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(heading ? R.dimen.space_lg : R.dimen.space_sm);
        content.addView(text, params);
        return text;
    }
    private void notice(int message) { QuappMotion.snackbar(content, getText(message), Snackbar.LENGTH_LONG).show(); }
}


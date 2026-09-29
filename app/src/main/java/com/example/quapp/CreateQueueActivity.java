package com.example.quapp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Create queue (canvas 14), or edit one when started with {@link #EXTRA_QUEUE_ID}. One screen
 * for both, because the form is identical; only the title, the button and the starting values
 * change.
 *
 * The pickers (municipality, category, dates, times) keep their choice in fields here, not in a
 * text box, so they're saved across rotation in onSaveInstanceState.
 */
public class CreateQueueActivity extends AppCompatActivity {

    /** Optional. Present = edit that queue; absent = create a new one. */
    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_EDIT_QUEUE_ID";

    private static final String STATE_MUNICIPALITY = "municipality";
    private static final String STATE_CATEGORY = "category";
    private static final String STATE_START = "start";
    private static final String STATE_END = "end";
    private static final String STATE_OPENS = "opens";
    private static final String STATE_CLOSES = "closes";

    private static final String TAG_START_PICKER = "start_picker";
    private static final String TAG_END_PICKER = "end_picker";
    private static final String TAG_OPENS_PICKER = "opens_picker";
    private static final String TAG_CLOSES_PICKER = "closes_picker";

    private Queue existingQueue;

    // Picker choices
    private String municipality;
    private Category category;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime opensAt;
    private LocalTime closesAt;

    private TextView municipalityField;
    private TextView categoryField;
    private TextView startField;
    private TextView endField;
    private TextView opensField;
    private TextView closesField;
    private View endColumn;
    private MaterialButtonToggleGroup lengthGroup;
    private MaterialButtonToggleGroup radiusGroup;

    private MaterialSwitch graceSwitch;
    private MaterialSwitch penaltySwitch;
    private MaterialSwitch proximitySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_queue);
        SystemBars.applyPaddingWithKeyboard(findViewById(R.id.create_root));

        String queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        if (queueId != null) {
            existingQueue = FakeData.queueById(queueId);
            if (existingQueue == null) {
                finish();
                return;
            }
        }

        cacheViews();
        setUpToggles();
        bindPostingAs();
        loadChoices(savedInstanceState);
        setUpPickers();
        setUpLength();
        reattachPickers();

        MaterialButton submitButton = findViewById(R.id.create_submit_button);
        if (existingQueue != null) {
            ((TextView) findViewById(R.id.create_title_text)).setText(R.string.create_edit_title);
            ((TextView) findViewById(R.id.create_subtitle_text)).setText(R.string.create_edit_subtitle);
            submitButton.setText(R.string.create_save_action);
        }

        findViewById(R.id.create_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitQueue();
            }
        });

        showChoices();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_MUNICIPALITY, municipality);
        outState.putString(STATE_CATEGORY, category == null ? null : category.name());
        // java.time values are saved as their ISO text ("2026-09-27", "08:00").
        outState.putString(STATE_START, startDate.toString());
        outState.putString(STATE_END, endDate.toString());
        outState.putString(STATE_OPENS, opensAt.toString());
        outState.putString(STATE_CLOSES, closesAt.toString());
    }

    private void cacheViews() {
        municipalityField = findViewById(R.id.create_municipality);
        categoryField = findViewById(R.id.create_category);
        startField = findViewById(R.id.create_start_date);
        endField = findViewById(R.id.create_end_date);
        opensField = findViewById(R.id.create_opens);
        closesField = findViewById(R.id.create_closes);
        endColumn = (View) endField.getParent();
        lengthGroup = findViewById(R.id.create_length_group);
        radiusGroup = findViewById(R.id.create_radius_group);
    }

    /** From the saved state after rotation, from the queue when editing, or the defaults. */
    private void loadChoices(@Nullable Bundle saved) {
        if (saved != null) {
            municipality = saved.getString(STATE_MUNICIPALITY);
            String savedCategory = saved.getString(STATE_CATEGORY);
            category = savedCategory == null ? null : Category.valueOf(savedCategory);
            startDate = LocalDate.parse(saved.getString(STATE_START));
            endDate = LocalDate.parse(saved.getString(STATE_END));
            opensAt = LocalTime.parse(saved.getString(STATE_OPENS));
            closesAt = LocalTime.parse(saved.getString(STATE_CLOSES));
            return;
        }
        if (existingQueue != null) {
            prefill(existingQueue);
            return;
        }
        startDate = Format.today();
        endDate = startDate;
        opensAt = LocalTime.of(8, 0);
        closesAt = LocalTime.of(17, 0);
        lengthGroup.check(R.id.create_one_day);
        radiusGroup.check(R.id.create_radius_1000);
    }

    private void prefill(Queue queue) {
        setText(R.id.create_name_input, queue.getName());
        setText(R.id.create_venue_input, queue.getVenue());
        setText(R.id.create_short_input, queue.getShortDescription());
        setText(R.id.create_details_input, queue.getDetails());
        setText(R.id.create_bring_input, queue.getBring());

        municipality = queue.getMunicipality();
        category = queue.getCategory();
        startDate = queue.getStartDate();
        endDate = queue.getEndDate();
        opensAt = queue.getOpensAt();
        closesAt = queue.getClosesAt();
        lengthGroup.check(startDate.equals(endDate) ? R.id.create_one_day : R.id.create_multiple_days);

        graceSwitch.setChecked(queue.isGracePeriodEnabled());
        penaltySwitch.setChecked(queue.isNoShowCooldownEnabled());
        proximitySwitch.setChecked(queue.isProximityCheckEnabled());
        radiusGroup.check(radiusButton(queue.isProximityCheckEnabled()
                ? queue.getJoinRadiusMeters() : 1000));
    }

    // ---- Verification toggles ------------------------------------------------

    private void setUpToggles() {
        graceSwitch = setUpToggle(R.id.create_toggle_grace,
                R.string.create_grace_label, R.string.create_grace_summary, true);
        penaltySwitch = setUpToggle(R.id.create_toggle_penalty,
                R.string.create_penalty_label, R.string.create_penalty_summary, true);
        proximitySwitch = setUpToggle(R.id.create_toggle_proximity,
                R.string.create_proximity_label, R.string.create_proximity_summary, false);

        // SMS confirmation isn't built (future work): shown, tagged Planned, and can't be turned on.
        View otp = findViewById(R.id.create_toggle_otp);
        MaterialSwitch otpSwitch = setUpToggle(R.id.create_toggle_otp,
                R.string.create_otp_label, R.string.create_otp_summary, false);
        otpSwitch.setEnabled(false);
        otp.setClickable(false);
        TextView tag = otp.findViewById(R.id.toggle_tag);
        tag.setText(R.string.create_planned);
        tag.setVisibility(View.VISIBLE);

        // The radius and pin only matter when the proximity check is on.
        final View proximityOptions = findViewById(R.id.create_proximity_options);
        proximitySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean on) {
                proximityOptions.setVisibility(on ? View.VISIBLE : View.GONE);
            }
        });
    }

    private MaterialSwitch setUpToggle(int includeId, int labelRes, int summaryRes,
                                       boolean checkedByDefault) {
        View row = findViewById(includeId);
        ((TextView) row.findViewById(R.id.toggle_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.toggle_summary)).setText(summaryRes);
        final MaterialSwitch toggle = row.findViewById(R.id.toggle_switch);
        toggle.setChecked(checkedByDefault);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle.toggle();
            }
        });
        return toggle;
    }

    /** "Posting as Brgy. Poblacion Council", with the badge if the account is verified. */
    private void bindPostingAs() {
        ((TextView) findViewById(R.id.create_posting_as)).setText(
                getString(R.string.create_posting_as, FakeData.MY_ORGANIZER_NAME));
        findViewById(R.id.create_posting_verified).setVisibility(
                FakeData.isMyOrganizerVerified() ? View.VISIBLE : View.GONE);
    }

    // ---- Pickers --------------------------------------------------------------

    private void setUpPickers() {
        municipalityField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickMunicipality();
            }
        });
        categoryField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickCategory();
            }
        });
        startField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker(TAG_START_PICKER, startDate);
            }
        });
        endField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker(TAG_END_PICKER, endDate);
            }
        });
        opensField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePicker(TAG_OPENS_PICKER, opensAt);
            }
        });
        closesField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePicker(TAG_CLOSES_PICKER, closesAt);
            }
        });
    }

    private void pickMunicipality() {
        final String[] towns = getResources().getStringArray(R.array.bohol_municipalities);
        int checked = -1;
        for (int i = 0; i < towns.length; i++) {
            if (towns[i].equals(municipality)) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.create_municipality_title)
                .setSingleChoiceItems(towns, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        municipality = towns[which];
                        showChoices();
                        dialog.dismiss();
                    }
                })
                .show();
    }

    /** The category sheet (canvas 17): tap one to choose it and close the sheet. */
    private void pickCategory() {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        Grain.attach(sheet);
        View content = getLayoutInflater().inflate(R.layout.sheet_category, null);
        RecyclerView list = content.findViewById(R.id.category_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new CategoryAdapter(category, new CategoryAdapter.OnCategoryPickedListener() {
            @Override
            public void onCategoryPicked(Category picked) {
                category = picked;
                showChoices();
                sheet.dismiss();
            }
        }));
        sheet.setContentView(content);
        sheet.show();
    }

    /**
     * Material's date picker works in UTC milliseconds at midnight, so a LocalDate goes in as
     * "that date at 00:00 UTC" and comes back out the same way. Past days can't be picked.
     */
    private void showDatePicker(String tag, LocalDate current) {
        long todayUtc = Format.today().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.from(todayUtc))
                        .build())
                .build();
        listenToDatePicker(picker, tag);
        picker.show(getSupportFragmentManager(), tag);
    }

    private void showTimePicker(String tag, LocalTime current) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(current.getHour())
                .setMinute(current.getMinute())
                .build();
        listenToTimePicker(picker, tag);
        picker.show(getSupportFragmentManager(), tag);
    }

    private void listenToDatePicker(final MaterialDatePicker<Long> picker, final String tag) {
        picker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
            @Override
            public void onPositiveButtonClick(Long selection) {
                LocalDate picked = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
                if (TAG_START_PICKER.equals(tag)) {
                    startDate = picked;
                    // One day, or a start that jumped past the end: the end follows.
                    if (isOneDay() || endDate.isBefore(startDate)) {
                        endDate = startDate;
                    }
                } else {
                    endDate = picked;
                }
                showChoices();
            }
        });
    }

    private void listenToTimePicker(final MaterialTimePicker picker, final String tag) {
        picker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalTime picked = LocalTime.of(picker.getHour(), picker.getMinute());
                if (TAG_OPENS_PICKER.equals(tag)) {
                    opensAt = picked;
                } else {
                    closesAt = picked;
                }
                showChoices();
            }
        });
    }

    /**
     * A picker that was open when the screen rotated comes back by itself (it's a
     * DialogFragment), but the listener set above belonged to the old Activity. Re-attach it.
     */
    @SuppressWarnings("unchecked")
    private void reattachPickers() {
        for (String tag : new String[]{TAG_START_PICKER, TAG_END_PICKER}) {
            Object picker = getSupportFragmentManager().findFragmentByTag(tag);
            if (picker instanceof MaterialDatePicker) {
                listenToDatePicker((MaterialDatePicker<Long>) picker, tag);
            }
        }
        for (String tag : new String[]{TAG_OPENS_PICKER, TAG_CLOSES_PICKER}) {
            Object picker = getSupportFragmentManager().findFragmentByTag(tag);
            if (picker instanceof MaterialTimePicker) {
                listenToTimePicker((MaterialTimePicker) picker, tag);
            }
        }
    }

    // ---- One day / multiple days ----------------------------------------------

    private void setUpLength() {
        lengthGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (!isChecked) {
                    return;
                }
                if (isOneDay()) {
                    endDate = startDate;
                }
                showChoices();
            }
        });
        MaterialButtonToggleGroup.OnButtonCheckedListener checkMark =
                new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                    @Override
                    public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId,
                                                boolean isChecked) {
                        // The chosen segment shows a check (DESIGN.md "Segmented button").
                        MaterialButton button = group.findViewById(checkedId);
                        button.setIconResource(isChecked ? R.drawable.ic_check : 0);
                    }
                };
        lengthGroup.addOnButtonCheckedListener(checkMark);
        radiusGroup.addOnButtonCheckedListener(checkMark);
        markChecked(lengthGroup);
        markChecked(radiusGroup);
    }

    private void markChecked(MaterialButtonToggleGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            MaterialButton button = (MaterialButton) group.getChildAt(i);
            button.setIconResource(button.isChecked() ? R.drawable.ic_check : 0);
        }
    }

    private boolean isOneDay() {
        return lengthGroup.getCheckedButtonId() != R.id.create_multiple_days;
    }

    /** Puts every choice on screen: the select fields, the end-date column and the summary. */
    private void showChoices() {
        municipalityField.setText(municipality);
        categoryField.setText(category == null ? null : getString(category.label));
        categoryField.setCompoundDrawablesRelativeWithIntrinsicBounds(
                category == null ? 0 : category.icon, 0, R.drawable.ic_chevron_down, 0);

        startField.setText(Format.day(this, startDate));
        endField.setText(Format.day(this, endDate));
        opensField.setText(Format.time(this, opensAt));
        closesField.setText(Format.time(this, closesAt));

        boolean oneDay = isOneDay();
        endColumn.setVisibility(oneDay ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.create_start_label)).setText(
                oneDay ? R.string.create_date_label : R.string.create_start_date_label);

        ((TextView) findViewById(R.id.create_pin_text)).setText(municipality == null
                ? getString(R.string.create_pin_none)
                : getString(R.string.create_pin_default, municipality));

        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        String range = oneDay || days < 1 ? Format.day(this, startDate) : rangeText();
        ((TextView) findViewById(R.id.create_schedule_summary)).setText(getResources().getQuantityString(
                R.plurals.create_schedule_summary, Math.max(days, 1), Math.max(days, 1), range,
                Format.time(this, opensAt), Format.time(this, closesAt)));
    }

    /** "Sep 27–28, 2026" */
    private String rangeText() {
        DateTimeFormatter monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault());
        String end = startDate.getMonth() == endDate.getMonth()
                ? String.valueOf(endDate.getDayOfMonth()) : endDate.format(monthDay);
        return startDate.format(monthDay) + "–" + end + ", " + endDate.getYear();
    }

    // ---- Saving ----------------------------------------------------------------

    private void submitQueue() {
        String name = text(R.id.create_name_input);
        String venue = text(R.id.create_venue_input);
        String shortDescription = text(R.id.create_short_input);
        String details = text(R.id.create_details_input);
        String bring = text(R.id.create_bring_input);

        // Checked in on-screen order, and all of them, so every problem shows at once.
        boolean valid = required(R.id.create_name_layout, name);
        valid &= required(R.id.create_venue_layout, venue);
        valid &= pickerChosen(municipalityField, municipality != null);
        valid &= pickerChosen(categoryField, category != null);
        findViewById(R.id.create_pick_error).setVisibility(
                municipality == null || category == null ? View.VISIBLE : View.GONE);
        valid &= required(R.id.create_short_layout, shortDescription);
        valid &= scheduleValid();

        if (!valid) {
            return;
        }

        // Unverified organizers run one queue at a time (canvas 58). One that opens later is fine.
        if (existingQueue == null && !startsLater() && !FakeData.isMyOrganizerVerified()) {
            Queue running = FakeData.myLiveQueue(null);
            if (running != null) {
                TrustSheets.showOneLiveQueue(this, running);
                return;
            }
        }

        // Editing keeps id, organizer, location and status; creating starts fresh.
        Queue.Builder builder = existingQueue != null
                ? existingQueue.toBuilder()
                : new Queue.Builder()
                        .setId(FakeData.newQueueId())
                        .setOrganizer(FakeData.MY_ORGANIZER_ID, FakeData.MY_ORGANIZER_NAME,
                                FakeData.isMyOrganizerVerified())
                        .setLocation(FakeData.defaultLatitude(), FakeData.defaultLongitude())
                        .setStatus(startsLater() ? Queue.Status.UPCOMING : Queue.Status.OPEN);

        Queue queue = builder
                .setName(name)
                .setVenue(venue)
                .setMunicipality(municipality)
                .setCategory(category)
                .setShortDescription(shortDescription)
                .setDetails(details.isEmpty() ? null : details)
                .setBring(bring.isEmpty() ? null : bring)
                .setSchedule(startDate, endDate, opensAt, closesAt)
                .setGracePeriodEnabled(graceSwitch.isChecked())
                .setNoShowCooldownEnabled(penaltySwitch.isChecked())
                .setProximity(proximitySwitch.isChecked(), checkedRadius())
                .build();

        FakeData.saveQueue(queue);
        finish();
    }

    /** A new queue whose first day or opening time is still ahead starts as Upcoming. */
    private boolean startsLater() {
        LocalDate today = Format.today();
        return startDate.isAfter(today)
                || (startDate.equals(today) && LocalTime.now(Format.MANILA).isBefore(opensAt));
    }

    private boolean scheduleValid() {
        TextView error = findViewById(R.id.create_schedule_error);
        int message = 0;
        if (endDate.isBefore(startDate)) {
            message = R.string.create_end_before_start_error;
        } else if (!closesAt.isAfter(opensAt)) {
            message = R.string.create_closes_before_opens_error;
        }
        error.setVisibility(message == 0 ? View.GONE : View.VISIBLE);
        if (message != 0) {
            error.setText(message);
        }
        return message == 0;
    }

    /** A select field shows the error outline until something is chosen. */
    private boolean pickerChosen(TextView field, boolean chosen) {
        field.setBackgroundResource(chosen ? R.drawable.bg_field : R.drawable.bg_field_error);
        return chosen;
    }

    private int checkedRadius() {
        int id = radiusGroup.getCheckedButtonId();
        if (id == R.id.create_radius_500) {
            return 500;
        } else if (id == R.id.create_radius_2000) {
            return 2000;
        } else if (id == R.id.create_radius_5000) {
            return 5000;
        }
        return 1000;
    }

    private static int radiusButton(int meters) {
        switch (meters) {
            case 500:
                return R.id.create_radius_500;
            case 2000:
                return R.id.create_radius_2000;
            case 5000:
                return R.id.create_radius_5000;
            default:
                return R.id.create_radius_1000;
        }
    }

    private boolean required(int layoutId, String value) {
        TextInputLayout layout = findViewById(layoutId);
        return Forms.check(layout, !Validation.isBlank(value),
                getString(R.string.create_required_error));
    }

    private String text(int inputId) {
        return Forms.text((TextInputEditText) findViewById(inputId));
    }

    private void setText(int inputId, @Nullable String value) {
        ((TextInputEditText) findViewById(inputId)).setText(value);
    }
}

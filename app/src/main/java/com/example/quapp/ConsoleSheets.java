package com.example.quapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The Live Console's three bottom sheets, all built on sheet_choices.xml:
 * Queue options (canvas 18), Extend closing time (19) and Remove from line (57).
 * They only collect the owner's choice; the console does the work and re-renders.
 */
final class ConsoleSheets {

    /** What the options sheet can ask the console to do. */
    interface OptionsListener {
        void onExtend();
        void onEdit();
        void onInsights();
        void onStatus(Queue.Status status);
        void onClose();
    }

    interface ExtendListener {
        void onExtended(LocalTime closesAt);
    }

    interface RemoveListener {
        void onRemove(Ticket ticket, Ticket.RemovalReason reason);
    }

    private static final int[] EXTEND_MINUTES = {30, 60, 120};

    private ConsoleSheets() {
        // Utility class.
    }

    // ---- Queue options (canvas 18) ------------------------------------------------

    static void showOptions(final Context context, Queue queue, final OptionsListener listener) {
        final Sheet sheet = new Sheet(context);
        sheet.title.setText(queue.getName());
        String closes = Format.time(context, queue.getClosesAt());
        boolean today = !queue.getEndDate().isBefore(Format.today());
        sheet.lead.setText(context.getString(R.string.options_lead_format,
                context.getString(statusLabel(queue.getStatus())),
                context.getString(today ? R.string.options_closes_today_format
                        : R.string.options_closes_format, closes)));

        Queue.Status status = queue.getStatus();
        boolean closed = status == Queue.Status.CLOSED;
        if (status == Queue.Status.OPEN || status == Queue.Status.PAUSED) {
            sheet.iconRow(R.drawable.ic_alarm_plus, context.getString(R.string.extend_title),
                    context.getString(R.string.options_extend_body_format, closes), false,
                    sheet.dismissThen(new Runnable() {
                        @Override
                        public void run() {
                            listener.onExtend();
                        }
                    }));
        }
        sheet.iconRow(R.drawable.ic_pencil, context.getString(R.string.console_menu_edit),
                context.getString(R.string.options_edit_body), false, sheet.dismissThen(new Runnable() {
                    @Override
                    public void run() {
                        listener.onEdit();
                    }
                }));
        sheet.iconRow(R.drawable.ic_chart, context.getString(R.string.console_menu_insights),
                context.getString(R.string.options_insights_body), false, sheet.dismissThen(new Runnable() {
                    @Override
                    public void run() {
                        listener.onInsights();
                    }
                }));
        if (status == Queue.Status.OPEN || status == Queue.Status.PAUSED) {
            final boolean paused = status == Queue.Status.PAUSED;
            sheet.iconRow(R.drawable.ic_circle_pause,
                    context.getString(paused ? R.string.console_menu_resume : R.string.console_menu_pause),
                    context.getString(paused ? R.string.options_resume_body : R.string.options_pause_body),
                    false, sheet.dismissThen(new Runnable() {
                        @Override
                        public void run() {
                            listener.onStatus(paused ? Queue.Status.OPEN : Queue.Status.PAUSED);
                        }
                    }));
        }
        // The one that can't be undone sits last, in red.
        if (closed) {
            sheet.iconRow(R.drawable.ic_circle_check, context.getString(R.string.console_menu_reopen),
                    context.getString(R.string.options_reopen_body), false, sheet.dismissThen(new Runnable() {
                        @Override
                        public void run() {
                            listener.onStatus(Queue.Status.OPEN);
                        }
                    }));
        } else {
            sheet.iconRow(R.drawable.ic_circle_x, context.getString(R.string.options_close_title),
                    context.getString(R.string.options_close_body), true, sheet.dismissThen(new Runnable() {
                        @Override
                        public void run() {
                            listener.onClose();
                        }
                    }));
        }
        sheet.show();
    }

    // ---- Extend closing time (canvas 19) ------------------------------------------

    static void showExtend(final FragmentActivity activity, final Queue queue, final int waiting,
                           final ExtendListener listener) {
        final Context context = activity;
        final Sheet sheet = new Sheet(context);
        sheet.title.setText(R.string.extend_title);
        sheet.lead.setText(context.getString(R.string.extend_lead_format, queue.getName()));
        sheet.numbers.setVisibility(View.VISIBLE);
        ((TextView) sheet.root.findViewById(R.id.choices_before_label)).setText(R.string.extend_now_closes);
        ((TextView) sheet.root.findViewById(R.id.choices_after_label)).setText(R.string.extend_will_close);
        ((TextView) sheet.root.findViewById(R.id.choices_before)).setText(
                Format.time(context, queue.getClosesAt()));

        final LocalTime[] chosen = new LocalTime[1];
        final List<View> rows = new ArrayList<>();
        final Runnable[] update = new Runnable[1];

        for (final int minutes : EXTEND_MINUTES) {
            final String label = context.getString(minutes == 30 ? R.string.extend_30
                    : minutes == 60 ? R.string.extend_60 : R.string.extend_120);
            rows.add(sheet.radioRow(label, null, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chosen[0] = later(queue.getClosesAt(), minutes);
                    sheet.check(rows, v);
                    update[0].run();
                }
            }));
        }
        // Custom opens the time picker; the row stays checked with the time it gave.
        rows.add(sheet.radioRow(context.getString(R.string.extend_custom),
                context.getString(R.string.extend_custom_body), new View.OnClickListener() {
                    @Override
                    public void onClick(final View row) {
                        LocalTime start = later(queue.getClosesAt(), 60);
                        final MaterialTimePicker picker = new MaterialTimePicker.Builder()
                                .setTimeFormat(android.text.format.DateFormat.is24HourFormat(context)
                                        ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                                .setHour(start.getHour())
                                .setMinute(start.getMinute())
                                .build();
                        picker.addOnPositiveButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                chosen[0] = LocalTime.of(picker.getHour(), picker.getMinute());
                                sheet.check(rows, row);
                                ((TextView) row.findViewById(R.id.option_body)).setText(
                                        Format.time(context, chosen[0]));
                                update[0].run();
                            }
                        });
                        picker.show(activity.getSupportFragmentManager(), "extend_time");
                    }
                }));

        update[0] = new Runnable() {
            @Override
            public void run() {
                boolean valid = chosen[0] != null && chosen[0].isAfter(queue.getClosesAt());
                String when = chosen[0] == null ? "" : Format.time(context, chosen[0]);
                ((TextView) sheet.root.findViewById(R.id.choices_after)).setText(when);
                if (!valid) {
                    sheet.note.setText(R.string.extend_too_late);
                    sheet.primary.setEnabled(false);
                    return;
                }
                String note = context.getString(R.string.extend_note_format, when);
                if (waiting > 0) {
                    note += " " + context.getResources().getQuantityString(
                            R.plurals.extend_note_waiting, waiting, waiting);
                }
                sheet.note.setText(note);
                sheet.primary.setEnabled(true);
                sheet.primary.setText(context.getString(R.string.extend_action_format, when));
            }
        };

        sheet.note.setVisibility(View.VISIBLE);
        sheet.primary.setVisibility(View.VISIBLE);
        sheet.primary.setOnClickListener(sheet.dismissThen(new Runnable() {
            @Override
            public void run() {
                listener.onExtended(chosen[0]);
            }
        }));

        // +1 hour to start with, as on the canvas.
        rows.get(1).performClick();
        sheet.show();
    }

    /** The closing time plus some minutes, kept within the day. */
    private static LocalTime later(LocalTime closes, int minutes) {
        int total = Math.min(closes.getHour() * 60 + closes.getMinute() + minutes, 23 * 60 + 59);
        return LocalTime.of(total / 60, total % 60);
    }

    // ---- Remove from line (canvas 57) --------------------------------------------

    static void showRemove(Context context, final Ticket ticket, final RemoveListener listener) {
        final Sheet sheet = new Sheet(context);
        sheet.title.setText(context.getString(R.string.remove_title_format,
                ticket.getTicketNumber(), ticket.getHolderName()));
        sheet.lead.setText(R.string.remove_lead);

        final Ticket.RemovalReason[] reasons = Ticket.RemovalReason.values();
        final Ticket.RemovalReason[] chosen = {reasons[0]};
        final List<View> rows = new ArrayList<>();
        for (final Ticket.RemovalReason reason : reasons) {
            rows.add(sheet.radioRow(context.getString(reasonTitle(reason)),
                    context.getString(reasonBody(reason)), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            chosen[0] = reason;
                            sheet.check(rows, v);
                        }
                    }));
        }
        sheet.check(rows, rows.get(0));

        sheet.danger.setVisibility(View.VISIBLE);
        sheet.danger.setText(R.string.remove_action);
        sheet.danger.setOnClickListener(sheet.dismissThen(new Runnable() {
            @Override
            public void run() {
                listener.onRemove(ticket, chosen[0]);
            }
        }));
        sheet.cancel.setVisibility(View.VISIBLE);
        sheet.show();
    }

    private static int reasonTitle(Ticket.RemovalReason reason) {
        switch (reason) {
            case DUPLICATE:
                return R.string.remove_duplicate;
            case ASKED_TO_LEAVE:
                return R.string.remove_asked;
            case PRANK:
            default:
                return R.string.remove_prank;
        }
    }

    private static int reasonBody(Ticket.RemovalReason reason) {
        switch (reason) {
            case DUPLICATE:
                return R.string.remove_duplicate_body;
            case ASKED_TO_LEAVE:
                return R.string.remove_asked_body;
            case PRANK:
            default:
                return R.string.remove_prank_body;
        }
    }

    static int statusLabel(Queue.Status status) {
        switch (status) {
            case UPCOMING:
                return R.string.detail_status_upcoming;
            case PAUSED:
                return R.string.detail_status_paused;
            case CLOSED:
                return R.string.detail_status_closed;
            case OPEN:
            default:
                return R.string.detail_status_open;
        }
    }

    // ---- The sheet itself --------------------------------------------------------

    /** One sheet_choices.xml in a BottomSheetDialog, with helpers to add rows. */
    private static final class Sheet {
        final Context context;
        final BottomSheetDialog dialog;
        final View root;
        final TextView title;
        final TextView lead;
        final View numbers;
        final LinearLayout rows;
        final TextView note;
        final MaterialButton primary;
        final MaterialButton danger;
        final MaterialButton cancel;

        Sheet(Context context) {
            this.context = context;
            dialog = new BottomSheetDialog(context);
            root = LayoutInflater.from(context).inflate(R.layout.sheet_choices, null);
            title = root.findViewById(R.id.choices_title);
            lead = root.findViewById(R.id.choices_lead);
            numbers = root.findViewById(R.id.choices_numbers);
            rows = root.findViewById(R.id.choices_rows);
            note = root.findViewById(R.id.choices_note);
            primary = root.findViewById(R.id.choices_primary);
            danger = root.findViewById(R.id.choices_danger);
            cancel = root.findViewById(R.id.choices_cancel);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.setContentView(root);
        }

        void show() {
            dialog.show();
        }

        /** A click listener that closes the sheet first, then acts. */
        View.OnClickListener dismissThen(final Runnable action) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    action.run();
                }
            };
        }

        /** A row with a tile icon (canvas 18). Red for the destructive one. */
        void iconRow(@DrawableRes int icon, String titleText, String body, boolean danger,
                     View.OnClickListener onClick) {
            View row = inflate(titleText, body);
            ImageView image = row.findViewById(R.id.option_icon);
            image.setImageResource(icon);
            if (danger) {
                int err = ContextCompat.getColor(context, R.color.err);
                image.setColorFilter(err);
                ((TextView) row.findViewById(R.id.option_title)).setTextColor(err);
            }
            row.setOnClickListener(onClick);
        }

        /** A row with a radio (canvases 19 and 57); the whole row is the touch target. */
        View radioRow(String titleText, String body, View.OnClickListener onClick) {
            View row = inflate(titleText, body);
            row.findViewById(R.id.option_tile).setVisibility(View.GONE);
            row.findViewById(R.id.option_radio).setVisibility(View.VISIBLE);
            row.setOnClickListener(onClick);
            return row;
        }

        /** Checks one radio row and unchecks the rest, like a RadioGroup. */
        void check(List<View> group, View selected) {
            for (View row : group) {
                boolean on = row == selected;
                ((MaterialRadioButton) row.findViewById(R.id.option_radio)).setChecked(on);
                row.setSelected(on);
            }
        }

        @NonNull
        private View inflate(String titleText, String body) {
            View row = LayoutInflater.from(context).inflate(R.layout.view_option_row, rows, false);
            ((TextView) row.findViewById(R.id.option_title)).setText(titleText);
            TextView bodyView = row.findViewById(R.id.option_body);
            bodyView.setText(body);
            bodyView.setVisibility(body == null ? View.GONE : View.VISIBLE);
            rows.addView(row);
            return row;
        }
    }
}

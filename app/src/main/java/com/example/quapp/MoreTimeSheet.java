package com.example.quapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * "I need more time" (canvas 28). The queuer picks minutes; the estimator's minutes per person
 * turn them into places (TicketRules.placesToMoveBack). The card previews the new position,
 * wait and "Be there by" for whatever is picked, before anything moves.
 *
 * Opened from the waiting ticket and from the Called screen ("Move me back"). Once per ticket.
 */
public final class MoreTimeSheet {

    /** Told when the ticket has moved, so the screen can show where it is now. */
    public interface OnMovedListener {
        void onMoved(Ticket moved, int places);
    }

    private static final int DEFAULT_MINUTES = 10;

    private final Context context;
    private final String ticketId;
    private final OnMovedListener listener;
    private BottomSheetDialog dialog;

    private int ahead;
    private int behind;
    private int waitNow;
    private double minutesPerPerson;
    private String queueId;

    private MoreTimeSheet(Context context, String ticketId, OnMovedListener listener) {
        // The sheet is always paper, even over the espresso Called screen.
        this.context = new ContextThemeWrapper(context, R.style.Theme_Quapp);
        this.ticketId = ticketId;
        this.listener = listener;
    }

    public static void show(Context context, String ticketId, OnMovedListener listener) {
        new MoreTimeSheet(context, ticketId, listener).show();
    }

    private void show() {
        Ticket ticket = ActiveTicketStore.ticket(ticketId);
        if (ticket == null || !ticket.isLive() || ticket.isMovedBack()) {
            return;
        }
        queueId = ticket.getQueueId();
        boolean called = ticket.getStatus() == Ticket.Status.CALLED;
        int inLine = FakeData.waitingTickets(queueId).size();
        // At the counter nobody is ahead, and the whole line is behind.
        ahead = called ? 0 : ticket.getPosition() - 1;
        behind = called ? inLine : inLine - ticket.getPosition();
        waitNow = called ? 0 : ticket.getEstimatedWaitMinutes();
        minutesPerPerson = FakeData.minutesPerPerson(queueId);

        View content = LayoutInflater.from(context).inflate(R.layout.sheet_more_time, null);
        ((TextView) content.findViewById(R.id.more_time_lead)).setText(context.getString(
                R.string.more_time_lead, ticket.getTicketNumber()));

        labelRow(content, R.id.more_time_row_ahead, R.string.more_time_ahead_label);
        labelRow(content, R.id.more_time_row_wait, R.string.more_time_wait_label);
        labelRow(content, R.id.more_time_row_arrive, R.string.more_time_arrive_label);

        final View view = content;
        MaterialButtonToggleGroup group = content.findViewById(R.id.more_time_group);
        group.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup g, int checkedId, boolean isChecked) {
                if (isChecked) {
                    preview(view, minutesFor(g, checkedId));
                }
            }
        });
        group.check(R.id.more_time_10);
        // check() doesn't fire the listener when the button was already checked; be sure.
        preview(content, DEFAULT_MINUTES);

        content.findViewById(R.id.more_time_keep).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog = new BottomSheetDialog(context);
        dialog.setContentView(content);
        dialog.show();
    }

    /** The minutes are the button's tag ("5" … "45"), so the layout is the one list of choices. */
    private static int minutesFor(MaterialButtonToggleGroup group, int buttonId) {
        return Integer.parseInt((String) group.findViewById(buttonId).getTag());
    }

    private void labelRow(View content, int rowId, int label) {
        ((TextView) content.findViewById(rowId).findViewById(R.id.preview_label)).setText(label);
    }

    private void valueRow(View content, int rowId, String value) {
        ((TextView) content.findViewById(rowId).findViewById(R.id.preview_value)).setText(value);
    }

    /** Fills the card, the explanation and the button for this many minutes. */
    private void preview(View content, int minutes) {
        final int places = TicketRules.placesToMoveBack(minutes, minutesPerPerson, behind);
        int newAhead = ahead + places;
        int waitAfter = FakeData.waitMinutes(queueId, newAhead);

        valueRow(content, R.id.more_time_row_ahead,
                context.getString(R.string.more_time_change_format, ahead, newAhead));
        valueRow(content, R.id.more_time_row_wait,
                context.getString(R.string.more_time_wait_change_format, waitNow, waitAfter));
        valueRow(content, R.id.more_time_row_arrive, context.getString(
                R.string.more_time_arrive_change_format, beThereBy(waitNow), beThereBy(waitAfter)));

        TextView basis = content.findViewById(R.id.more_time_basis);
        MaterialButton confirm = content.findViewById(R.id.more_time_confirm);
        if (behind == 0) {
            basis.setText(R.string.more_time_last);
            confirm.setText(R.string.more_time_nobody_behind);
            confirm.setEnabled(false);
            return;
        }

        // Locale.US keeps the decimal point a point, like the other numbers in the app.
        String perPerson = String.format(Locale.US, "%.1f", minutesPerPerson);
        String placesText = context.getResources().getQuantityString(
                R.plurals.more_time_places, places, places);
        String explanation = context.getString(R.string.more_time_basis_format,
                perPerson, minutes, placesText);
        if (places == behind) {
            explanation += " " + context.getString(R.string.more_time_end_of_line);
        }
        basis.setText(explanation + " " + context.getString(R.string.more_time_once));

        confirm.setEnabled(true);
        confirm.setText(context.getResources().getQuantityString(
                R.plurals.more_time_confirm, places, places));
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ticket moved = ActiveTicketStore.moveBack(ticketId, places);
                dialog.dismiss();
                if (moved != null) {
                    listener.onMoved(moved, places);
                }
            }
        });
    }

    /** "Be there by", by the same rule as the ticket screen. */
    @NonNull
    private String beThereBy(int waitMinutes) {
        int minutes = TicketRules.beThereInMinutes(waitMinutes);
        return Format.time(context, Instant.now().plus(Duration.ofMinutes(minutes)));
    }
}

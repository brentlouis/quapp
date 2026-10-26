package com.example.quapp;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Fills a queue card (item_queue_card) for Browse or for the owner's Queues tab. One card
 * layout and one binder, so both sides always look the same. The card is a catalogue card
 * (DESIGN.md "Queue card"): Where / What / When on ruled lines, then queuers see the wait and
 * owners see what they've done today.
 */
public final class QueueCards {

    /** A queue's last hour shows "Closes in 40 min" in amber (DECISIONS.md queue cards). */
    private static final long CLOSING_SOON_MINUTES = 60;

    private QueueCards() {
        // Utility class.
    }

    /**
     * @param stats the owner's numbers for this queue, or null on Browse
     */
    public static void bind(View card, Queue queue, @Nullable QueueStats stats) {
        Context context = card.getContext();
        boolean owner = stats != null;
        boolean faded = queue.getStatus() == Queue.Status.PAUSED
                || queue.getStatus() == Queue.Status.CLOSED;

        text(card, R.id.card_serial).setText(serial(context, queue));
        bindStamp(card, queue, owner);

        TextView name = text(card, R.id.card_name);
        name.setText(queue.getName());
        name.setTextColor(ContextCompat.getColor(context, faded ? R.color.ink_muted : R.color.ink));
        card.findViewById(R.id.card_verified).setVisibility(
                queue.isOrganizerVerified() ? View.VISIBLE : View.GONE);

        String where = queue.getVenue();
        if (queue.isProximityCheckEnabled()) {
            where = context.getString(R.string.card_where_within_format, where, context.getString(
                    R.string.card_within_format, radiusText(context, queue.getJoinRadiusMeters())));
        }
        row(card, R.id.card_row_where, R.string.card_label_where, where);
        String what = queue.getShortDescription();
        row(card, R.id.card_row_what, R.string.card_label_what, what);
        card.findViewById(R.id.card_row_what).setVisibility(
                what == null || what.isEmpty() ? View.GONE : View.VISIBLE);

        boolean closingSoon = isClosingSoon(queue);
        TextView when = row(card, R.id.card_row_when, R.string.card_label_when,
                closingSoon ? closesIn(context, queue) : when(context, queue));
        when.setTextAppearance(R.style.TextAppearance_Quapp_MonoSmall);
        when.setTextColor(ContextCompat.getColor(context, closingSoon ? R.color.warn : R.color.ink));

        if (owner) {
            bindOwnerLine(card, queue, stats);
        } else {
            bindQueuerLine(card, queue);
        }
    }

    /**
     * "Relief · No. 001": the category and a number, like a catalogue card's corner. It's
     * decoration, not an id: the demo's short ids ("q3") give their own number; the server's
     * random ids (uuids) give a steady one from 0 to 999, the same every time for that queue.
     */
    private static String serial(Context context, Queue queue) {
        String category = context.getString(queue.getCategory().label);
        String digits = queue.getId().replaceAll("\\D", "");
        int number = !digits.isEmpty() && digits.length() <= 3
                ? Integer.parseInt(digits) : Math.floorMod(queue.getId().hashCode(), 1000);
        return context.getString(R.string.card_serial_format, category, number);
    }

    /** Ruled metadata: stack at large text on phones instead of squeezing the value column. */
    private static TextView row(View card, int rowId, int label, CharSequence value) {
        LinearLayout row = card.findViewById(rowId);
        TextView labelText = row.findViewById(R.id.row_label);
        labelText.setText(label);
        TextView valueText = row.findViewById(R.id.row_value);
        android.content.res.Configuration configuration = card.getResources().getConfiguration();
        boolean stacked = configuration.fontScale >= 1.5f && configuration.screenWidthDp < 480;
        row.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        labelText.setMinWidth(stacked ? 0
                : card.getResources().getDimensionPixelSize(R.dimen.card_label_width));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) valueText.getLayoutParams();
        params.width = stacked ? LinearLayout.LayoutParams.MATCH_PARENT : 0;
        params.weight = stacked ? 0 : 1;
        params.setMarginStart(stacked ? 0
                : card.getResources().getDimensionPixelSize(R.dimen.space_md));
        params.topMargin = stacked
                ? card.getResources().getDimensionPixelSize(R.dimen.space_xs) : 0;
        valueText.setLayoutParams(params);
        valueText.setText(value);
        return valueText;
    }

    // ---- Stamp ----------------------------------------------------------------

    /** The status, rubber-stamped in its colour: OPEN, PAUSED (with the time, for owners). */
    private static void bindStamp(View card, Queue queue, boolean owner) {
        Context context = card.getContext();
        TextView stamp = text(card, R.id.card_status);
        int color;
        int label;
        switch (queue.getStatus()) {
            case UPCOMING:
                color = R.color.ink_muted;
                label = R.string.detail_status_upcoming;
                break;
            case PAUSED:
                color = R.color.warn;
                label = R.string.detail_status_paused;
                break;
            case CLOSED:
                color = R.color.ink_muted;
                label = R.string.detail_status_closed;
                break;
            case OPEN:
            default:
                color = R.color.ok;
                label = R.string.detail_status_open;
                break;
        }
        int c = ContextCompat.getColor(context, color);
        stamp.setTextColor(c);
        stamp.setBackground(TicketShapes.background(context,
                ShapeAppearanceModel.builder().setAllCornerSizes(new RelativeCornerSize(0.5f)).build(),
                ColorUtils.setAlphaComponent(c, 20)));
        // Owners see when a queue was paused, right in the stamp: "Paused 10:05".
        if (owner && queue.getStatus() == Queue.Status.PAUSED && queue.getPausedAt() != null) {
            stamp.setText(context.getString(R.string.card_paused_at_format,
                    Format.time(context, queue.getPausedAt())));
        } else {
            stamp.setText(label);
        }
    }

    /**
     * The schedule as the When line shows it: "Today · 8 AM – 4 PM", "Day 1 of 2 · 8 AM – 4 PM"
     * during a multi-day run, "Tomorrow · 1 – 5 PM", or the dates for later ones.
     */
    static String when(Context context, Queue queue) {
        LocalDate today = Format.today();
        String hours = context.getString(R.string.card_hours_format,
                Format.time(context, queue.getOpensAt()), Format.time(context, queue.getClosesAt()));
        LocalDate start = queue.getStartDate();
        LocalDate end = queue.getEndDate();
        boolean multiDay = !start.equals(end);
        if (multiDay && !today.isBefore(start) && !today.isAfter(end)) {
            long day = ChronoUnit.DAYS.between(start, today) + 1;
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            return context.getString(R.string.card_day_of_format, (int) day, (int) days, hours);
        }
        return Format.schedule(context, queue);
    }

    /** Open, on its last day, within the last hour before closing. */
    private static boolean isClosingSoon(Queue queue) {
        if (queue.getStatus() != Queue.Status.OPEN || !Format.today().equals(queue.getEndDate())) {
            return false;
        }
        LocalTime now = LocalTime.now(Format.MANILA);
        long minutes = Duration.between(now, queue.getClosesAt()).toMinutes();
        return minutes >= 0 && minutes <= CLOSING_SOON_MINUTES;
    }

    private static String closesIn(Context context, Queue queue) {
        LocalDateTime now = LocalDateTime.now(Format.MANILA);
        long minutes = Duration.between(now, queue.getEndDate().atTime(queue.getClosesAt())).toMinutes();
        return context.getString(R.string.card_closes_in_format, (int) Math.max(minutes, 1));
    }

    // ---- The last line ---------------------------------------------------------

    /** Open queues lead with "55 min"; upcoming and inactive queues keep one compact line. */
    private static void bindQueuerLine(View card, Queue queue) {
        Context context = card.getContext();
        View hero = card.findViewById(R.id.card_wait_hero);
        View compact = card.findViewById(R.id.card_row_wait);
        if (queue.getStatus() == Queue.Status.OPEN) {
            compact.setVisibility(View.GONE);
            hero.setVisibility(View.VISIBLE);
            text(card, R.id.card_wait_value).setText(context.getString(
                    R.string.card_wait_value_format, queue.getEstimatedWaitMinutes()));
            Integer serving = queue.getNowServing();
            text(card, R.id.card_wait_details).setText(context.getString(
                    R.string.card_wait_details_format, queue.getPeopleWaiting(),
                    serving == null ? context.getString(R.string.browse_eta_none)
                            : context.getString(R.string.ticket_number_format, serving)));
            return;
        }

        hero.setVisibility(View.GONE);
        compact.setVisibility(View.VISIBLE);
        CharSequence value;
        int label = R.string.card_label_wait;
        switch (queue.getStatus()) {
            case PAUSED:
                value = context.getString(R.string.card_paused_line_format,
                        pausedSince(context, queue), queue.getPeopleWaiting());
                break;
            case CLOSED:
                value = closedLine(context, queue, null);
                break;
            case UPCOMING:
                label = R.string.card_opens;
                value = context.getString(R.string.card_opens_line_format,
                        opensText(context, queue));
                break;
            default:
                value = "";
        }
        lastLine(card, label, value);
    }

    /** Today: "42 waiting · 18 served · 3 no-shows"; closed queues say when they closed. */
    private static void bindOwnerLine(View card, Queue queue, QueueStats stats) {
        Context context = card.getContext();
        card.findViewById(R.id.card_wait_hero).setVisibility(View.GONE);
        card.findViewById(R.id.card_row_wait).setVisibility(View.VISIBLE);
        if (queue.getStatus() == Queue.Status.CLOSED) {
            lastLine(card, R.string.card_label_today, closedLine(context, queue, stats));
            return;
        }
        int noShows = stats.getNoShowsToday();
        lastLine(card, R.string.card_label_today, context.getResources().getQuantityString(
                R.plurals.card_today_line, noShows, queue.getPeopleWaiting(),
                stats.getServedToday(), noShows));
    }

    /** The Wait / Today line: mono, like everything that counts. */
    private static void lastLine(View card, int label, CharSequence value) {
        TextView valueText = row(card, R.id.card_row_wait, label, value);
        valueText.setTextAppearance(R.style.TextAppearance_Quapp_MonoSmall);
        valueText.setTextColor(ContextCompat.getColor(card.getContext(), R.color.ink));
    }

    private static String pausedSince(Context context, Queue queue) {
        return queue.getPausedAt() == null ? "" : Format.time(context, queue.getPausedAt());
    }

    /** "Closed at 10 AM · 45 served", or just "Closed at 10 AM" when nothing more is known. */
    private static String closedLine(Context context, Queue queue, @Nullable QueueStats stats) {
        String at = queue.getClosedAt() == null
                ? Format.time(context, queue.getClosesAt()) : Format.time(context, queue.getClosedAt());
        return stats == null ? context.getString(R.string.card_closed_at_format, at)
                : context.getString(R.string.card_closed_served_format, at, stats.getServedToday());
    }

    /** "1 PM" when it opens today, else the day ("Tue"). */
    private static String opensText(Context context, Queue queue) {
        if (queue.getStartDate().equals(Format.today())) {
            return Format.time(context, queue.getOpensAt());
        }
        return Format.day(context, queue.getStartDate());
    }

    /** "55" with a smaller "min" after it (the Browse highlight uses it). */
    static CharSequence withUnit(String value, String unit) {
        SpannableStringBuilder text = new SpannableStringBuilder(value);
        int start = text.length();
        text.append(' ').append(unit);
        text.setSpan(new RelativeSizeSpan(0.55f), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    /** "500 m" or "2 km". */
    static String radiusText(Context context, int meters) {
        return Format.distance(context, meters);
    }

    private static TextView text(View card, int id) {
        return card.findViewById(id);
    }
}

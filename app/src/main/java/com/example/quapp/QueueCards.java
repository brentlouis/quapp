package com.example.quapp;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Fills a queue card (item_queue_card) for Browse or for the owner's Queues tab. One card
 * layout and one binder, so both sides always look the same; only the footer differs:
 * queuers see the wait, owners see what they've served (DESIGN.md "Queue card").
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

        // Faded cards: no fill, dashed edge, quieter title, footer on paper (DESIGN.md).
        card.setBackgroundResource(faded ? R.drawable.bg_card_faded : R.drawable.bg_card);
        // The background shape is the outline, so the strip's colour stops at the round corners.
        card.setClipToOutline(true);
        text(card, R.id.card_name).setTextColor(ContextCompat.getColor(context,
                faded ? R.color.ink_muted : R.color.ink));
        card.findViewById(R.id.card_footer).setBackgroundColor(faded ? 0
                : ContextCompat.getColor(context, R.color.paper_sunk));

        bindStrip(card, queue, owner);

        ((ImageView) card.findViewById(R.id.card_category_icon)).setImageResource(
                queue.getCategory().icon);
        text(card, R.id.card_name).setText(queue.getName());
        card.findViewById(R.id.card_verified).setVisibility(
                queue.isOrganizerVerified() ? View.VISIBLE : View.GONE);
        text(card, R.id.card_venue).setText(queue.getVenue());
        TextView radius = text(card, R.id.card_radius);
        radius.setVisibility(queue.isProximityCheckEnabled() ? View.VISIBLE : View.GONE);
        if (queue.isProximityCheckEnabled()) {
            radius.setText(context.getString(R.string.card_within_format,
                    radiusText(context, queue.getJoinRadiusMeters())));
        }
        text(card, R.id.card_description).setText(queue.getShortDescription());

        if (owner) {
            bindOwnerFooter(card, queue, stats);
        } else {
            bindQueuerFooter(card, queue);
        }
    }

    // ---- Strip ----------------------------------------------------------------

    private static void bindStrip(View card, Queue queue, boolean owner) {
        Context context = card.getContext();
        View strip = card.findViewById(R.id.card_strip);
        ImageView icon = card.findViewById(R.id.card_status_icon);
        TextView status = text(card, R.id.card_status);

        int ground;
        int color;
        int label;
        @DrawableRes int iconRes;
        switch (queue.getStatus()) {
            case UPCOMING:
                ground = R.color.paper_sunk;
                color = R.color.ink_muted;
                label = R.string.detail_status_upcoming;
                iconRes = R.drawable.ic_clock;
                break;
            case PAUSED:
                ground = R.color.warn_soft;
                color = R.color.warn;
                label = R.string.detail_status_paused;
                iconRes = R.drawable.bg_status_dot;
                break;
            case CLOSED:
                ground = 0;
                color = R.color.ink_muted;
                label = R.string.detail_status_closed;
                iconRes = 0;
                break;
            case OPEN:
            default:
                ground = R.color.ok_soft;
                color = R.color.ok;
                label = R.string.detail_status_open;
                iconRes = R.drawable.bg_status_dot;
                break;
        }
        strip.setBackgroundColor(ground == 0 ? 0 : ContextCompat.getColor(context, ground));
        int c = ContextCompat.getColor(context, color);
        status.setTextColor(c);
        // Owners see when a queue was paused, right in the label: "Paused 10:05".
        if (owner && queue.getStatus() == Queue.Status.PAUSED && queue.getPausedAt() != null) {
            status.setText(context.getString(R.string.card_paused_at_format,
                    Format.time(context, queue.getPausedAt())));
        } else {
            status.setText(label);
        }
        icon.setVisibility(iconRes == 0 ? View.GONE : View.VISIBLE);
        if (iconRes != 0) {
            icon.setImageResource(iconRes);
            icon.setColorFilter(c);
            int size = context.getResources().getDimensionPixelSize(
                    iconRes == R.drawable.bg_status_dot ? R.dimen.status_dot : R.dimen.chip_icon_size);
            icon.getLayoutParams().width = size;
            icon.getLayoutParams().height = size;
        }

        TextView when = text(card, R.id.card_when);
        boolean closingSoon = isClosingSoon(queue);
        when.setText(closingSoon ? closesIn(context, queue) : when(context, queue));
        when.setTextColor(ContextCompat.getColor(context, closingSoon ? R.color.warn : R.color.ink));
    }

    /**
     * The schedule as the strip shows it: "Today · 8 AM – 4 PM", "Day 1 of 2 · 8 AM – 4 PM"
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

    // ---- Footers --------------------------------------------------------------

    /** Est. wait · in line · now serving; or for Upcoming: opens · joined early. */
    private static void bindQueuerFooter(View card, Queue queue) {
        Context context = card.getContext();
        switch (queue.getStatus()) {
            case PAUSED:
                showLine(card, R.drawable.ic_circle_pause, context.getString(
                        R.string.card_paused_line_format, pausedSince(context, queue),
                        queue.getPeopleWaiting()));
                return;
            case CLOSED:
                showLine(card, R.drawable.ic_circle_x, closedLine(context, queue, null));
                return;
            case UPCOMING:
                showFooter(card);
                cell(card, R.id.card_cell_1, opensText(context, queue), R.string.card_opens, true);
                cell(card, R.id.card_cell_2, String.valueOf(queue.getPeopleWaiting()),
                        R.string.card_joined_early, false);
                hideCell(card, R.id.card_cell_3, R.id.card_cell_rule_3);
                return;
            case OPEN:
            default:
                showFooter(card);
                cell(card, R.id.card_cell_1, withUnit(String.valueOf(queue.getEstimatedWaitMinutes()),
                        context.getString(R.string.card_minutes_unit)), R.string.card_est_wait, true);
                cell(card, R.id.card_cell_2, String.valueOf(queue.getPeopleWaiting()),
                        R.string.card_in_line, false);
                Integer serving = queue.getNowServing();
                cell(card, R.id.card_cell_3, serving == null ? context.getString(R.string.browse_eta_none)
                        : context.getString(R.string.ticket_number_format, serving),
                        R.string.card_now_serving, false);
        }
    }

    /** Waiting · served · no-shows; closed queues get one line instead. */
    private static void bindOwnerFooter(View card, Queue queue, QueueStats stats) {
        Context context = card.getContext();
        if (queue.getStatus() == Queue.Status.CLOSED) {
            showLine(card, R.drawable.ic_circle_x, closedLine(context, queue, stats));
            return;
        }
        showFooter(card);
        cell(card, R.id.card_cell_1, String.valueOf(queue.getPeopleWaiting()),
                R.string.card_waiting, true);
        cell(card, R.id.card_cell_2, String.valueOf(stats.getServedToday()),
                R.string.card_served, false);
        int noShows = stats.getNoShowsToday();
        cell(card, R.id.card_cell_3, String.valueOf(noShows),
                noShows == 1 ? R.string.card_no_show : R.string.card_no_shows, false);
    }

    private static String pausedSince(Context context, Queue queue) {
        return queue.getPausedAt() == null ? "" : Format.time(context, queue.getPausedAt());
    }

    /** "Closed at 10 AM · 45 served", or just "Closed" when nothing more is known. */
    private static String closedLine(Context context, Queue queue, @Nullable QueueStats stats) {
        String at = queue.getClosedAt() == null
                ? Format.time(context, queue.getClosesAt()) : Format.time(context, queue.getClosedAt());
        return stats == null ? context.getString(R.string.card_closed_at_format, at)
                : context.getString(R.string.card_closed_served_format, at, stats.getServedToday());
    }

    /** "1 PM" when it opens today, else the day ("Tue"). */
    private static CharSequence opensText(Context context, Queue queue) {
        if (queue.getStartDate().equals(Format.today())) {
            String time = Format.time(context, queue.getOpensAt());
            // "1 PM": shrink the AM/PM like the "min" unit, so the number reads first.
            int space = time.lastIndexOf(' ');
            return space < 0 ? time : withUnit(time.substring(0, space), time.substring(space + 1));
        }
        return Format.day(context, queue.getStartDate());
    }

    /** "55" with a smaller "min" after it. */
    static CharSequence withUnit(String value, String unit) {
        SpannableStringBuilder text = new SpannableStringBuilder(value);
        int start = text.length();
        text.append(' ').append(unit);
        text.setSpan(new RelativeSizeSpan(0.55f), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    private static void cell(View card, int cellId, CharSequence value, int label, boolean hero) {
        View cell = card.findViewById(cellId);
        cell.setVisibility(View.VISIBLE);
        TextView valueText = cell.findViewById(R.id.cell_value);
        valueText.setText(value);
        valueText.setTextAppearance(hero ? R.style.TextAppearance_Quapp_StatHero
                : R.style.TextAppearance_Quapp_Stat);
        ((TextView) cell.findViewById(R.id.cell_label)).setText(label);
    }

    private static void hideCell(View card, int cellId, int ruleId) {
        card.findViewById(cellId).setVisibility(View.GONE);
        card.findViewById(ruleId).setVisibility(View.GONE);
    }

    private static void showFooter(View card) {
        card.findViewById(R.id.card_footer_rule).setVisibility(View.VISIBLE);
        card.findViewById(R.id.card_footer).setVisibility(View.VISIBLE);
        card.findViewById(R.id.card_cell_rule_3).setVisibility(View.VISIBLE);
        card.findViewById(R.id.card_line).setVisibility(View.GONE);
    }

    private static void showLine(View card, @DrawableRes int icon, String text) {
        card.findViewById(R.id.card_footer_rule).setVisibility(View.GONE);
        card.findViewById(R.id.card_footer).setVisibility(View.GONE);
        card.findViewById(R.id.card_line).setVisibility(View.VISIBLE);
        TextView line = text(card, R.id.card_line_text);
        line.setText(text);
        line.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
    }

    /** "500 m" or "2 km". */
    static String radiusText(Context context, int meters) {
        return Format.distance(context, meters);
    }

    private static TextView text(View card, int id) {
        return card.findViewById(id);
    }
}

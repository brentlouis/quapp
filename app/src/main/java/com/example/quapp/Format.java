package com.example.quapp;

import android.content.Context;
import android.text.format.DateFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Small text formatting shared across screens: names, phones, dates and schedules. */
public final class Format {

    /** Quapp runs in Bohol, so every time is shown in Philippine time (MODELS.md). */
    public static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    private Format() {
        // Utility class.
    }

    /** "Maria Santos" → "MS"; one word gives one letter. */
    public static String initials(String name) {
        String[] words = name.trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(words[0].charAt(0));
        if (words.length > 1) {
            result.append(words[words.length - 1].charAt(0));
        }
        return result.toString().toUpperCase(Locale.ROOT);
    }

    /** "09171234567" → "0917 123 4567", the way people read a PH mobile number aloud. */
    public static String spacedPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 4) + " " + phone.substring(4, 7) + " " + phone.substring(7);
    }

    public static LocalDate today() {
        return LocalDate.now(MANILA);
    }

    /**
     * "8 AM" or "8:30 AM", or "08:00" when the phone is set to 24-hour time.
     * Whole hours drop the ":00" so a schedule line stays short.
     */
    public static String time(Context context, LocalTime time) {
        String pattern;
        if (DateFormat.is24HourFormat(context)) {
            pattern = "HH:mm";
        } else {
            pattern = time.getMinute() == 0 ? "h a" : "h:mm a";
        }
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()));
    }

    /** A moment as a time of day in Manila, for "Closed at 10:00 AM". */
    public static String time(Context context, Instant instant) {
        return time(context, instant.atZone(MANILA).toLocalTime());
    }

    /** "Today", "Tomorrow" or "Sat, Sep 27". */
    public static String day(Context context, LocalDate date) {
        LocalDate today = today();
        if (date.equals(today)) {
            return context.getString(R.string.schedule_today);
        }
        if (date.equals(today.plusDays(1))) {
            return context.getString(R.string.schedule_tomorrow);
        }
        return date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()));
    }

    /** The date something happened, for History: "Sat, Aug 22". */
    public static String day(Context context, Instant instant) {
        return day(context, instant.atZone(MANILA).toLocalDate());
    }

    /**
     * A queue's schedule on one line:
     * "Today · 8 AM – 4 PM", or "Sep 27–28 · 8 AM – 4 PM" for a run of days.
     */
    public static String schedule(Context context, Queue queue) {
        String days;
        LocalDate start = queue.getStartDate();
        LocalDate end = queue.getEndDate();
        if (start.equals(end)) {
            days = day(context, start);
        } else if (start.getMonth() == end.getMonth()) {
            days = start.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
                    + "–" + end.getDayOfMonth();
        } else {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault());
            days = start.format(f) + " – " + end.format(f);
        }
        return context.getString(R.string.schedule_format, days,
                time(context, queue.getOpensAt()), time(context, queue.getClosesAt()));
    }
}

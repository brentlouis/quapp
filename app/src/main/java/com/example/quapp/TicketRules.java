package com.example.quapp;

import java.time.LocalDate;

/**
 * The two ticket rules from MODELS.md, kept free of Android so they can be unit tested:
 * when two queues' hours clash, and how many places "I need more time" moves you back.
 * The server enforces both; the app uses them to preview and to explain.
 */
public final class TicketRules {

    /** The minutes a queuer can ask for on the "I need more time" sheet. */
    public static final int[] MORE_TIME_CHOICES = {5, 10, 15, 20, 30, 45};

    private TicketRules() {
        // Utility class.
    }

    /**
     * True when you'd be in both lines at the same time: the day you'd queue is the same and the
     * hours overlap that day. Touching ends (one closes at 12:00, the other opens at 12:00) don't
     * count.
     *
     * A ticket is for one day even when a queue runs several, so each queue is compared on the
     * day you'd be in it ({@link #queueDay}), not on every day it runs.
     */
    public static boolean hoursOverlap(Queue a, Queue b, LocalDate today) {
        boolean sameDay = queueDay(a, today).equals(queueDay(b, today));
        boolean shareHours = a.getOpensAt().isBefore(b.getClosesAt())
                && b.getOpensAt().isBefore(a.getClosesAt());
        return sameDay && shareHours;
    }

    /** How much earlier than the estimated call "Be there by" asks you to arrive. */
    public static final int ARRIVAL_BUFFER_MINUTES = 10;

    /**
     * Minutes from now until "Be there by": the estimated call time minus a 10-minute buffer,
     * or minus half the wait when the wait is short, so a 10-minute wait doesn't say "now".
     */
    public static int beThereInMinutes(int waitMinutes) {
        int buffer = Math.min(ARRIVAL_BUFFER_MINUTES, waitMinutes / 2);
        return Math.max(0, waitMinutes - buffer);
    }

    /** The day a ticket for this queue is for: today once it has started, else its first day. */
    public static LocalDate queueDay(Queue queue, LocalDate today) {
        return queue.getStartDate().isAfter(today) ? queue.getStartDate() : today;
    }

    /**
     * How far back a ticket moves for the time asked: time ÷ the estimator's minutes per person,
     * rounded up (so you always get at least the time you asked for), and never past the end
     * of the line.
     *
     * @param minutesNeeded      what the queuer picked on the sheet
     * @param minutesPerPerson   the estimator's current minutes per person for this queue
     * @param peopleBehind       how many are behind the ticket right now
     */
    public static int placesToMoveBack(int minutesNeeded, double minutesPerPerson, int peopleBehind) {
        if (minutesNeeded <= 0 || peopleBehind <= 0) {
            return 0;
        }
        // A queue with no data yet uses the default (FakeData / MODELS.md: 5 min per person).
        double perPerson = minutesPerPerson > 0 ? minutesPerPerson : 5.0;
        int places = (int) Math.ceil(minutesNeeded / perPerson);
        return Math.min(Math.max(places, 1), peopleBehind);
    }
}

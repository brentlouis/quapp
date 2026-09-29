package com.example.quapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;

public class TicketRulesTest {

    private static final LocalDate MON = LocalDate.of(2026, 9, 28);

    private static Queue queue(LocalDate start, LocalDate end, int opens, int closes) {
        return new Queue.Builder()
                .setId("q")
                .setName("Queue")
                .setSchedule(start, end, LocalTime.of(opens, 0), LocalTime.of(closes, 0))
                .build();
    }

    // ---- hoursOverlap ----------------------------------------------------------

    @Test
    public void sameDaySameHoursOverlap() {
        assertTrue(TicketRules.hoursOverlap(queue(MON, MON, 8, 12), queue(MON, MON, 10, 14), MON));
    }

    @Test
    public void morningAndAfternoonDontOverlap() {
        assertFalse(TicketRules.hoursOverlap(queue(MON, MON, 8, 12), queue(MON, MON, 13, 17), MON));
    }

    @Test
    public void touchingEndsDontOverlap() {
        assertFalse(TicketRules.hoursOverlap(queue(MON, MON, 8, 12), queue(MON, MON, 12, 17), MON));
    }

    @Test
    public void differentDaysDontOverlap() {
        assertFalse(TicketRules.hoursOverlap(queue(MON, MON, 8, 17),
                queue(MON.plusDays(1), MON.plusDays(1), 8, 17), MON));
    }

    @Test
    public void aTicketForAMultiDayQueueIsForToday() {
        // In line today for a queue that also runs tomorrow: tomorrow's queue is still free.
        Queue twoDays = queue(MON, MON.plusDays(1), 8, 17);
        Queue tomorrow = queue(MON.plusDays(1), MON.plusDays(1), 13, 17);
        assertFalse(TicketRules.hoursOverlap(twoDays, tomorrow, MON));
        // Once it's tomorrow, both tickets would be for the same afternoon.
        assertTrue(TicketRules.hoursOverlap(twoDays, tomorrow, MON.plusDays(1)));
    }

    @Test
    public void upcomingQueueIsComparedOnItsFirstDay() {
        assertEquals(MON.plusDays(2), TicketRules.queueDay(queue(MON.plusDays(2), MON.plusDays(4), 9, 18), MON));
        assertEquals(MON, TicketRules.queueDay(queue(MON.minusDays(1), MON.plusDays(4), 9, 18), MON));
    }

    // ---- placesToMoveBack ------------------------------------------------------

    @Test
    public void roundsUpSoYouGetAtLeastTheTimeAsked() {
        // 10 min at 3 min per person: 3 places is only 9 min, so 4.
        assertEquals(4, TicketRules.placesToMoveBack(10, 3.0, 40));
    }

    @Test
    public void exactDivisionIsNotRoundedFurther() {
        assertEquals(5, TicketRules.placesToMoveBack(10, 2.0, 40));
    }

    @Test
    public void neverPastTheEndOfTheLine() {
        assertEquals(3, TicketRules.placesToMoveBack(45, 1.5, 3));
    }

    @Test
    public void lastInLineCantMove() {
        assertEquals(0, TicketRules.placesToMoveBack(10, 2.0, 0));
    }

    @Test
    public void slowQueueStillMovesOnePlace() {
        // 5 min at 12 min per person rounds up to 1.
        assertEquals(1, TicketRules.placesToMoveBack(5, 12.0, 10));
    }

    @Test
    public void noDataUsesTheFiveMinuteDefault() {
        assertEquals(2, TicketRules.placesToMoveBack(10, 0, 10));
    }

    // ---- beThereInMinutes ------------------------------------------------------

    @Test
    public void longWaitArrivesTenMinutesEarly() {
        assertEquals(45, TicketRules.beThereInMinutes(55));
    }

    @Test
    public void shortWaitArrivesHalfwayNotNow() {
        assertEquals(5, TicketRules.beThereInMinutes(10));
        assertEquals(0, TicketRules.beThereInMinutes(0));
    }
}

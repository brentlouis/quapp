package com.example.quapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public class QueueFilterTest {

    private static Queue queue(String id, String name, String venue,
                               String municipality, Category category) {
        return new Queue.Builder()
                .setId(id)
                .setName(name)
                .setVenue(venue)
                .setMunicipality(municipality)
                .setCategory(category)
                .setSchedule(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 28),
                        LocalTime.of(8, 0), LocalTime.of(17, 0))
                .build();
    }

    private final List<Queue> queues = Arrays.asList(
            queue("q1", "Barangay Relief Distribution", "Brgy. Poblacion Hall", "Tagbilaran City", Category.RELIEF),
            queue("q2", "Free Medical Mission", "Tagbilaran City Gym", "Tagbilaran City", Category.MEDICAL),
            queue("q6", "Anti-Rabies Vaccination Drive", "Brgy. Booy Health Center", "Dauis", Category.MEDICAL),
            queue("q7", "Tourist Assistance Desk", "Alona Beach Info Center", "Panglao", Category.OTHER));

    @Test
    public void noFiltersReturnsEverything() {
        assertEquals(4, QueueFilter.apply(queues, "", null, null).size());
        assertEquals(4, QueueFilter.apply(queues, null, null, null).size());
    }

    @Test
    public void searchIsCaseInsensitiveAndTrimmed() {
        List<Queue> result = QueueFilter.apply(queues, "  MEDICAL ", null, null);
        // Matches q2 by name and q6 by category.
        assertEquals(2, result.size());
    }

    @Test
    public void searchMatchesVenue() {
        List<Queue> result = QueueFilter.apply(queues, "alona", null, null);
        assertEquals(1, result.size());
        assertEquals("q7", result.get(0).getId());
    }

    @Test
    public void categoryAndMunicipalityCombine() {
        List<Queue> result = QueueFilter.apply(queues, "", Category.MEDICAL, "Dauis");
        assertEquals(1, result.size());
        assertEquals("q6", result.get(0).getId());
    }

    @Test
    public void allFiltersTogetherCanMatchNothing() {
        assertEquals(0, QueueFilter.apply(queues, "relief", Category.MEDICAL, null).size());
    }

    private static Queue live(String id, String town, Queue.Status status, int wait, boolean verified) {
        return new Queue.Builder()
                .setId(id)
                .setName(id)
                .setMunicipality(town)
                .setOrganizer("o", "Org", verified)
                .setStatus(status)
                .setEstimatedWaitMinutes(wait)
                .setSchedule(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 28),
                        LocalTime.of(8, 0), LocalTime.of(17, 0))
                .build();
    }

    private final List<Queue> live = Arrays.asList(
            live("paused", "Tagbilaran City", Queue.Status.PAUSED, 30, true),
            live("closed", "Tagbilaran City", Queue.Status.CLOSED, 0, true),
            live("upcoming", "Tagbilaran City", Queue.Status.UPCOMING, 0, true),
            live("slow", "Tagbilaran City", Queue.Status.OPEN, 55, true),
            live("fast", "Tagbilaran City", Queue.Status.OPEN, 11, true),
            live("unverified", "Dauis", Queue.Status.OPEN, 5, false),
            live("dauis", "Dauis", Queue.Status.OPEN, 20, true));

    @Test
    public void browseOrdersOpenUpcomingPausedAndHidesClosed() {
        List<Queue> result = QueueFilter.forBrowse(live);
        assertEquals(6, result.size());
        assertEquals("slow", result.get(0).getId());
        assertEquals("upcoming", result.get(4).getId());
        assertEquals("paused", result.get(5).getId());
    }

    @Test
    public void openNowSkipsUnverifiedAndSortsByWait() {
        List<Queue> result = QueueFilter.openNowAcrossBohol(live);
        assertEquals(3, result.size());
        assertEquals("fast", result.get(0).getId());
        assertEquals("dauis", result.get(1).getId());
    }

    @Test
    public void shortestWaitIsTheFastestOpenQueueInTown() {
        assertEquals("fast", QueueFilter.shortestWait(live, "Tagbilaran City").getId());
        assertEquals(null, QueueFilter.shortestWait(live, "Loon"));
    }

    @Test
    public void busiestOtherTownSkipsTheCurrentOne() {
        assertEquals("Dauis", QueueFilter.busiestOtherTown(live, "Tagbilaran City"));
        assertEquals("Tagbilaran City", QueueFilter.busiestOtherTown(live, "Loon"));
    }
}

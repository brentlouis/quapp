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
}

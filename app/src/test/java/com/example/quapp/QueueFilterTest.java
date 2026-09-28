package com.example.quapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class QueueFilterTest {

    private static Queue queue(String id, String name, String venue,
                               String municipality, String category) {
        return new Queue.Builder()
                .setId(id)
                .setName(name)
                .setVenue(venue)
                .setMunicipality(municipality)
                .setCategory(category)
                .setServiceHours("Daily")
                .build();
    }

    private final List<Queue> queues = Arrays.asList(
            queue("q1", "Barangay Relief Distribution", "Brgy. Poblacion Hall", "Tagbilaran City", "Relief"),
            queue("q2", "Free Medical Mission", "Tagbilaran City Gym", "Tagbilaran City", "Medical"),
            queue("q6", "Anti-Rabies Vaccination Drive", "Brgy. Booy Health Center", "Dauis", "Medical"),
            queue("q7", "Tourist Assistance Desk", "Alona Beach Info Center", "Panglao", "Community"));

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
        List<Queue> result = QueueFilter.apply(queues, "", "Medical", "Dauis");
        assertEquals(1, result.size());
        assertEquals("q6", result.get(0).getId());
    }

    @Test
    public void allFiltersTogetherCanMatchNothing() {
        assertEquals(0, QueueFilter.apply(queues, "relief", "Medical", null).size());
    }
}

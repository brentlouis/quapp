package com.example.quapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search and filter logic for Browse, kept out of the Activity so it can be unit tested. */
public final class QueueFilter {

    private QueueFilter() {
        // Utility class.
    }

    /**
     * @param query        free text matched against name, venue, municipality and category;
     *                     null or blank matches everything
     * @param category     exact category, or null for all
     * @param municipality exact municipality, or null for all
     */
    public static List<Queue> apply(List<Queue> queues, String query,
                                    String category, String municipality) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Queue> matches = new ArrayList<>();

        for (Queue queue : queues) {
            if (category != null && !category.equals(queue.getCategory())) {
                continue;
            }
            if (municipality != null && !municipality.equals(queue.getMunicipality())) {
                continue;
            }
            if (!needle.isEmpty() && !matchesText(queue, needle)) {
                continue;
            }
            matches.add(queue);
        }

        return matches;
    }

    private static boolean matchesText(Queue queue, String needle) {
        return contains(queue.getName(), needle)
                || contains(queue.getVenue(), needle)
                || contains(queue.getMunicipality(), needle)
                || contains(queue.getCategory(), needle);
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }
}

package com.example.quapp;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Search and filter logic for Browse, kept out of the Activity so it can be unit tested. */
public final class QueueFilter {

    private QueueFilter() {
        // Utility class.
    }

    /**
     * @param query        free text matched against name, venue, municipality, category and
     *                     the short description;
     *                     null or blank matches everything
     * @param category     exact category, or null for all
     * @param municipality exact municipality, or null for all
     */
    public static List<Queue> apply(List<Queue> queues, String query,
                                    Category category, String municipality) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Queue> matches = new ArrayList<>();

        for (Queue queue : queues) {
            if (category != null && category != queue.getCategory()) {
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

    /**
     * What Browse lists: Open first, then Upcoming, then Paused; Closed queues are hidden (they
     * stay in History). Within each status the order they came in is kept.
     */
    public static List<Queue> forBrowse(List<Queue> queues) {
        List<Queue> result = new ArrayList<>();
        for (Queue.Status status : new Queue.Status[]{
                Queue.Status.OPEN, Queue.Status.UPCOMING, Queue.Status.PAUSED}) {
            for (Queue queue : queues) {
                if (queue.getStatus() == status) {
                    result.add(queue);
                }
            }
        }
        return result;
    }

    /**
     * "Open now across Bohol" on a first visit: open queues by verified organizers only
     * (DECISIONS.md "Unverified organizers can post, with limits"), shortest wait first.
     */
    public static List<Queue> openNowAcrossBohol(List<Queue> queues) {
        List<Queue> result = new ArrayList<>();
        for (Queue queue : queues) {
            if (queue.getStatus() == Queue.Status.OPEN && queue.isOrganizerVerified()) {
                result.add(queue);
            }
        }
        Collections.sort(result, new Comparator<Queue>() {
            @Override
            public int compare(Queue a, Queue b) {
                return Integer.compare(a.getEstimatedWaitMinutes(), b.getEstimatedWaitMinutes());
            }
        });
        return result;
    }

    /** The open queue in this town with the shortest wait, or null if none is open. */
    @Nullable
    public static Queue shortestWait(List<Queue> queues, String municipality) {
        Queue best = null;
        for (Queue queue : queues) {
            if (queue.getStatus() == Queue.Status.OPEN
                    && queue.getMunicipality().equals(municipality)
                    && (best == null || queue.getEstimatedWaitMinutes() < best.getEstimatedWaitMinutes())) {
                best = queue;
            }
        }
        return best;
    }

    /**
     * For an empty town: the other town with the most open queues, to suggest instead.
     * Null when no other town has one.
     */
    @Nullable
    public static String busiestOtherTown(List<Queue> queues, String municipality) {
        Map<String, Integer> open = new HashMap<>();
        String best = null;
        for (Queue queue : queues) {
            String town = queue.getMunicipality();
            if (queue.getStatus() != Queue.Status.OPEN || town.equals(municipality)) {
                continue;
            }
            int count = open.containsKey(town) ? open.get(town) + 1 : 1;
            open.put(town, count);
            if (best == null || count > open.get(best)) {
                best = town;
            }
        }
        return best;
    }

    /** How many queues are open in a town. */
    public static int openCount(List<Queue> queues, String municipality) {
        int count = 0;
        for (Queue queue : queues) {
            if (queue.getStatus() == Queue.Status.OPEN && queue.getMunicipality().equals(municipality)) {
                count++;
            }
        }
        return count;
    }

    private static boolean matchesText(Queue queue, String needle) {
        return contains(queue.getName(), needle)
                || contains(queue.getVenue(), needle)
                || contains(queue.getMunicipality(), needle)
                || contains(queue.getCategory().name(), needle)
                || contains(queue.getShortDescription(), needle);
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }
}

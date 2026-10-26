package com.example.quapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The organizer's own queues with today's numbers for each, for Your queues and Today.
 * Keeps the last copy so a tab shows it straight away while it asks for the latest.
 *
 * The numbers are one request per queue (GET /queues/{id}/stats) after the list. An organizer
 * has a handful of queues, so that's a few small requests, not a problem worth a new endpoint.
 */
public final class MyQueues {

    public interface Loaded {
        void onLoaded(List<Queue> queues, Map<String, QueueStats> stats);

        void onFailed(ApiError error);
    }

    @Nullable
    private static List<Queue> lastQueues;
    private static final Map<String, QueueStats> lastStats = new HashMap<>();

    private MyQueues() {
    }

    /** The last list loaded, or null before the first answer. */
    @Nullable
    public static List<Queue> last() {
        return lastQueues;
    }

    public static Map<String, QueueStats> lastStats() {
        return lastStats;
    }

    /** The list, then each queue's numbers; `done` hears once everything is in. */
    public static void load(final Context context, final Loaded done) {
        ApiClient.api(context).myQueues().enqueue(new ApiCallback<List<Queue>>(context) {
            @Override
            protected void onSuccess(@Nullable List<Queue> queues) {
                final List<Queue> list = queues == null ? new ArrayList<Queue>() : queues;
                Queues.putAll(list);
                loadStats(context, list, done);
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                done.onFailed(error);
            }
        });
    }

    private static void loadStats(Context context, final List<Queue> queues, final Loaded done) {
        final Map<String, QueueStats> stats = new HashMap<>();
        if (queues.isEmpty()) {
            finish(queues, stats, done);
            return;
        }
        final int[] pending = {queues.size()};
        for (final Queue queue : queues) {
            ApiClient.api(context).stats(queue.getId()).enqueue(new ApiCallback<QueueStats>(context) {
                @Override
                protected void onSuccess(@Nullable QueueStats numbers) {
                    if (numbers != null) {
                        stats.put(queue.getId(), numbers);
                    }
                    oneDone();
                }

                @Override
                protected void onError(@NonNull ApiError error) {
                    // A card without numbers still shows the queue
                    oneDone();
                }

                private void oneDone() {
                    if (--pending[0] == 0) {
                        finish(queues, stats, done);
                    }
                }
            });
        }
    }

    private static void finish(List<Queue> queues, Map<String, QueueStats> stats, Loaded done) {
        lastQueues = queues;
        lastStats.clear();
        lastStats.putAll(stats);
        done.onLoaded(queues, stats);
    }

    /** Signing out forgets them. */
    public static void clear() {
        lastQueues = null;
        lastStats.clear();
    }
}

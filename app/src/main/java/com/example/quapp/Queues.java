package com.example.quapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The queues the app has seen from the server, by id, so any screen can show one straight away
 * (a ticket's venue hours, Queue detail opened from Browse) while it asks the server for the
 * latest. Filled by Browse, by Queue detail, and by the ticket store for the queues it holds
 * tickets in.
 *
 * Only the main thread touches it (Retrofit calls back there), so a plain HashMap is enough.
 */
public final class Queues {

    private static final Map<String, Queue> byId = new HashMap<>();

    private Queues() {
    }

    /** The last copy seen, or null if the app hasn't seen this queue yet. */
    @Nullable
    public static Queue get(String queueId) {
        return byId.get(queueId);
    }

    public static void put(Queue queue) {
        byId.put(queue.getId(), queue);
    }

    public static void putAll(List<Queue> queues) {
        for (Queue queue : queues) {
            put(queue);
        }
    }

    /** Asks the server for the latest copy, keeps it, then hands it to `done`. */
    public static void fetch(Context context, String queueId, final Loaded done) {
        ApiClient.api(context).queue(queueId).enqueue(new ApiCallback<Queue>(context) {
            @Override
            protected void onSuccess(@Nullable Queue queue) {
                put(queue);
                done.onLoaded(queue);
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                done.onFailed(error);
            }
        });
    }

    /** Signing out forgets what this account saw. */
    public static void clear() {
        byId.clear();
    }

    public interface Loaded {
        void onLoaded(Queue queue);

        void onFailed(ApiError error);
    }
}

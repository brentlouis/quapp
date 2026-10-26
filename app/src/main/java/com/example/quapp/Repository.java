package com.example.quapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import retrofit2.Call;

/**
 * The server, in the shape the screens were built around: ask for something, get
 * {@link Callback#onSuccess} or {@link Callback#onError} on the main thread. The screens' own
 * loading / empty / error states (LoadingStateHelper) stay as they were; only where the data
 * comes from changed (it was FakeData's async methods with the same shape).
 *
 * Signing out and suspension are still handled for every call by ApiCallback.
 */
public final class Repository {

    public interface Callback<T> {
        void onSuccess(T result);

        void onError(ApiError error);
    }

    private Repository() {
    }

    /** Every upcoming, open and paused queue (Browse, Saved, public profiles). Kept in Queues. */
    public static void queues(Context context, final Callback<List<Queue>> callback) {
        run(context, ApiClient.api(context).queues(), new Callback<List<Queue>>() {
            @Override
            public void onSuccess(List<Queue> queues) {
                Queues.putAll(queues);
                callback.onSuccess(queues);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    /** One queue, fresh from the server. Kept in Queues. */
    public static void queue(Context context, String queueId, final Callback<Queue> callback) {
        run(context, ApiClient.api(context).queue(queueId), new Callback<Queue>() {
            @Override
            public void onSuccess(Queue queue) {
                Queues.put(queue);
                callback.onSuccess(queue);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    /** The account's finished tickets, newest first (History, Profile's counts). */
    public static void history(Context context, Callback<List<Ticket>> callback) {
        run(context, ApiClient.api(context).myTickets(false), callback);
    }

    /** Today's numbers for one of the organizer's queues (Insights). */
    public static void stats(Context context, String queueId, Callback<QueueStats> callback) {
        run(context, ApiClient.api(context).stats(queueId), callback);
    }

    /** What a screen shows when a request failed: offline, or the server's own message. */
    public static String message(Context context, ApiError error) {
        return error.is(ApiError.OFFLINE) ? context.getString(R.string.api_offline) : error.message;
    }

    /**
     * Sends the request and hands the answer to the callback. A screen that has closed in the
     * meantime gets nothing (ApiCallback checks), so callbacks don't touch dead views.
     */
    static <T> void run(Context context, Call<T> call, final Callback<T> callback) {
        call.enqueue(new ApiCallback<T>(context) {
            @Override
            protected void onSuccess(@Nullable T body) {
                callback.onSuccess(body);
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                callback.onError(error);
            }
        });
    }
}

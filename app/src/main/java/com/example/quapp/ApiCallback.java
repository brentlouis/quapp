package com.example.quapp;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * What every screen does with a server answer, in one place. A screen only writes the parts
 * that are its own:
 *
 *     api.login(body).enqueue(new ApiCallback<AuthResponse>(this) {
 *         protected void onSuccess(AuthResponse auth) { … }
 *         protected void onError(ApiError error) { … }   // optional
 *     });
 *
 * Handled here for every screen:
 * - SUSPENDED: the account was suspended while signed in. Signed out, off to Account suspended.
 * - NOT_SIGNED_IN on a signed-in call: the session ended on the server (logged out elsewhere,
 *   or the admin suspended it). Signed out, back to Login.
 * - An answer arriving after the screen closed is dropped, so nothing touches a dead view.
 *
 * Retrofit calls these methods on the main thread, so they can update views directly.
 */
public abstract class ApiCallback<T> implements Callback<T> {

    private final Context context;

    protected ApiCallback(Context context) {
        this.context = context;
    }

    /** The request worked. `body` is null for answers with no content (204). */
    protected abstract void onSuccess(@Nullable T body);

    /**
     * The request didn't work, and it wasn't one of the cases handled for every screen.
     * By default a short message; screens override it to put the error where it belongs.
     */
    protected void onError(@NonNull ApiError error) {
        String message = error.is(ApiError.OFFLINE)
                ? context.getString(R.string.api_offline) : error.message;
        if (context instanceof Activity) {
            // A Snackbar at the bottom of the current screen (CLAUDE.md: Snackbar over Toast)
            Snackbar.make(((Activity) context).findViewById(android.R.id.content), message,
                    Snackbar.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public final void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        if (isGone()) {
            return;
        }
        if (response.isSuccessful()) {
            onSuccess(response.body());
            return;
        }
        String body = null;
        try {
            if (response.errorBody() != null) {
                body = response.errorBody().string();
            }
        } catch (IOException ignored) {
            // No body to read: ApiError.parse falls back to a generic error
        }
        ApiError error = ApiError.parse(response.code(), body);

        Session session = new Session(context);
        if (error.is("SUSPENDED") && session.isLoggedIn()) {
            session.logOut();
            context.startActivity(AccountSuspendedActivity.intent(context, session.getPhone(),
                    error.extraString("suspended_reason"), error.extraString("suspended_at")));
            return;
        }
        if (error.is("NOT_SIGNED_IN") && session.isLoggedIn()) {
            session.logOut();
            context.startActivity(session.homeIntent(context));
            return;
        }
        onError(error);
    }

    @Override
    public final void onFailure(@NonNull Call<T> call, @NonNull Throwable failure) {
        if (!isGone() && !call.isCanceled()) {
            onError(ApiError.offline());
        }
    }

    /** The screen that asked has closed since. */
    private boolean isGone() {
        return context instanceof Activity
                && (((Activity) context).isFinishing() || ((Activity) context).isDestroyed());
    }
}

package com.example.quapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Runs once when the app process starts, before any screen. Sets up the notification channel
 * and keeps the queuer's tickets up to date.
 *
 * While any Quapp screen is open, the tickets are synced with the server every 10 seconds
 * (ActiveTicketStore.sync); a ticket turning CALLED shows the "You're being called" notification
 * from there. With every screen closed, nothing polls (DECISIONS.md "'You've been called' reaches
 * the phone by polling", known limit).
 */
public class QuappApplication extends Application {

    /** How often the tickets are asked for while the app is open. */
    private static final long POLL_MS = 10_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    /** Screens currently on top (resumed); polling runs while this is above zero. */
    private int visible;

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            ActiveTicketStore.sync(QuappApplication.this);
            handler.postDelayed(this, POLL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        CalledNotifier.createChannel(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (visible++ == 0) {
                    // Back in the app: sync at once, then every 10 seconds
                    handler.post(poll);
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                if (--visible == 0) {
                    handler.removeCallbacks(poll);
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle state) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }
}

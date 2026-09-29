package com.example.quapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Whether the phone can reach the internet right now, and a callback when that changes.
 * Screens that show live numbers use it for the offline state (canvas 25).
 *
 * "Validated" means Android actually reached the internet through the network, so a Wi-Fi
 * with no internet behind it (or a captive portal) counts as offline.
 */
final class Connectivity {

    /** Told on the main thread whenever the phone goes on or offline. */
    interface Listener {
        void onConnectivityChanged(boolean online);
    }

    private final ConnectivityManager manager;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ConnectivityManager.NetworkCallback callback;

    Connectivity(Context context) {
        manager = ContextCompat.getSystemService(context, ConnectivityManager.class);
    }

    boolean isOnline() {
        if (manager == null) {
            return true;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /** Start listening (onResume). The network callbacks run on a background thread. */
    void start(final Listener listener) {
        stop();
        if (manager == null) {
            return;
        }
        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                              @NonNull NetworkCapabilities capabilities) {
                post(listener);
            }

            @Override
            public void onLost(@NonNull Network network) {
                post(listener);
            }
        };
        manager.registerDefaultNetworkCallback(callback);
    }

    /** Stop listening (onPause), so a closed screen isn't kept alive by the callback. */
    void stop() {
        if (manager != null && callback != null) {
            manager.unregisterNetworkCallback(callback);
        }
        callback = null;
        main.removeCallbacksAndMessages(null);
    }

    private void post(final Listener listener) {
        main.post(new Runnable() {
            @Override
            public void run() {
                listener.onConnectivityChanged(isOnline());
            }
        });
    }
}

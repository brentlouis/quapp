package com.example.quapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.util.Consumer;

/**
 * The proximity check (canvas 44, 45, 48): the phone's location is read once, when the queuer
 * taps Join, and compared with the queue's radius. Nothing is tracked afterwards.
 *
 * Uses the framework LocationManager rather than Google Play services, so it needs no extra
 * library. LocationManagerCompat gives the same "current location" call on every API level.
 */
final class ProximityCheck {

    /** Told once, on the main thread: the location, or null if none could be found. */
    interface Callback {
        void onLocated(@Nullable Location location);
    }

    /** Both, so the user can pick precise or approximate in the system dialog. */
    static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /** Indoors a first fix can take a while; after this we settle for the last known one. */
    private static final long TIMEOUT_MS = 15_000L;
    /** How old a last known location may be and still count as "here". */
    private static final long RECENT_MS = 2 * 60_000L;

    private ProximityCheck() {
        // Utility class.
    }

    /** Precise or approximate is enough: the smallest radius is 500 m. */
    static boolean hasPermission(Context context) {
        return granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
                || granted(context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /** The phone-wide Location switch in quick settings. */
    static boolean isLocationOn(Context context) {
        LocationManager manager = ContextCompat.getSystemService(context, LocationManager.class);
        return manager != null && LocationManagerCompat.isLocationEnabled(manager);
    }

    /** Metres from here to the queue's venue pin. */
    static float distanceMeters(Location here, Queue queue) {
        float[] result = new float[1];
        Location.distanceBetween(here.getLatitude(), here.getLongitude(),
                queue.getLatitude(), queue.getLongitude(), result);
        return result[0];
    }

    /**
     * Asks for one fresh fix. If none arrives within {@link #TIMEOUT_MS}, falls back to the last
     * location the phone knows, which may be null. Only call after {@link #hasPermission}.
     */
    @SuppressWarnings("MissingPermission") // checked by hasPermission() before every call
    static void locate(Context context, final Callback callback) {
        final LocationManager manager = ContextCompat.getSystemService(context, LocationManager.class);
        final String provider = provider(manager);
        if (manager == null || provider == null) {
            callback.onLocated(null);
            return;
        }

        // A fix from the last couple of minutes is still where the phone is: no need to wait.
        Location recent = manager.getLastKnownLocation(provider);
        if (recent != null && ageMs(recent) < RECENT_MS) {
            callback.onLocated(recent);
            return;
        }

        final Handler main = new Handler(Looper.getMainLooper());
        final CancellationSignal cancel = new CancellationSignal();
        // One-element array: the anonymous classes below can't reassign a local boolean.
        final boolean[] done = {false};

        final Runnable timeout = new Runnable() {
            @Override
            public void run() {
                if (done[0]) {
                    return;
                }
                done[0] = true;
                cancel.cancel();
                callback.onLocated(manager.getLastKnownLocation(provider));
            }
        };
        main.postDelayed(timeout, TIMEOUT_MS);

        LocationManagerCompat.getCurrentLocation(manager, provider, cancel,
                ContextCompat.getMainExecutor(context), new Consumer<Location>() {
                    @Override
                    public void accept(Location location) {
                        if (done[0]) {
                            return;
                        }
                        done[0] = true;
                        main.removeCallbacks(timeout);
                        callback.onLocated(location != null ? location
                                : manager.getLastKnownLocation(provider));
                    }
                });
    }

    /**
     * The fused provider (Android 12+) mixes GPS, Wi-Fi and cell; before that, the network
     * provider answers faster indoors than GPS. Null if every provider is off.
     */
    @Nullable
    private static String provider(@Nullable LocationManager manager) {
        if (manager == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && manager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
            return LocationManager.FUSED_PROVIDER;
        }
        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        return null;
    }

    /** Measured on the clock that doesn't jump when the phone's time is changed. */
    private static long ageMs(Location location) {
        return (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1_000_000L;
    }

    private static boolean granted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}

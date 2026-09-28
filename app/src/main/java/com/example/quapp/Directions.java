package com.example.quapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

/**
 * Opens a queue's venue in whatever maps app is installed: a geo: link with the venue's
 * coordinates and its name as the pin label. No Maps SDK or location permission needed
 * (DECISIONS.md "Arrival info").
 */
public final class Directions {

    private Directions() {
        // Utility class.
    }

    public static void open(Activity activity, @Nullable Queue queue) {
        if (queue == null) {
            return;
        }
        Uri uri = Uri.parse(String.format(Locale.US, "geo:0,0?q=%f,%f(%s)",
                queue.getLatitude(), queue.getLongitude(), Uri.encode(queue.getVenue())));
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(activity.findViewById(android.R.id.content),
                    R.string.ticket_no_maps_app, Snackbar.LENGTH_SHORT).show();
        }
    }
}

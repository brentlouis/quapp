package com.example.quapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.google.android.material.snackbar.Snackbar;

/**
 * Emails Quapp support. Used by Forgot password, Help and support, and Account suspended:
 * with no SMS gateway and no in-app chat, email is the one way to reach a person.
 */
final class Support {

    private Support() {
        // Utility class.
    }

    /**
     * ACTION_SENDTO with a mailto: address only matches email apps, not every app that can
     * share text. The subject and body are filled in so the user only types their details.
     */
    static void email(Activity activity, String subject, String body) {
        String address = activity.getString(R.string.forgot_support_email);
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No email app installed: show the address so they can write from anywhere.
            Snackbar.make(activity.findViewById(android.R.id.content),
                    activity.getString(R.string.forgot_no_email_app, address),
                    Snackbar.LENGTH_LONG).show();
        }
    }
}

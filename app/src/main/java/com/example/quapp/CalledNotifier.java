package com.example.quapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * "You're being called" (canvas 21): a heads-up notification with a live countdown and an
 * "I'm here" button, so a queuer with the phone in a pocket can confirm without opening the app.
 * It's what makes the 3-minute grace period fair (DECISIONS.md "Notifications aren't optional").
 *
 * Today FakeData tells QuappApplication when a ticket is called; with the backend, a push
 * message from the server does, and this class stays the same.
 */
final class CalledNotifier {

    static final String CHANNEL_ID = "called";
    static final String EXTRA_TICKET_ID = "com.example.quapp.EXTRA_NOTIFIED_TICKET_ID";

    private CalledNotifier() {
        // Utility class.
    }

    /**
     * High importance so it pops up over whatever is on screen and makes a sound. Creating a
     * channel that already exists does nothing, so this is safe to call on every app start.
     */
    static void createChannel(Context context) {
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.notification_channel_called))
                .setDescription(context.getString(R.string.notification_channel_called_description))
                .build();
        NotificationManagerCompat.from(context).createNotificationChannel(channel);
    }

    /** Android 13+ asks the user first; older versions allow notifications by default. */
    static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    @SuppressWarnings("MissingPermission") // canPost() checks it
    static void show(Context context, Ticket ticket) {
        if (!canPost(context)) {
            return;
        }
        long remaining = ActiveTicketStore.graceRemainingMs(ticket);
        String number = context.getString(R.string.ticket_number_format, ticket.getTicketNumber());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_quapp)
                .setColor(ContextCompat.getColor(context, R.color.spotlight))
                .setContentTitle(context.getString(R.string.notification_called_title, number))
                .setContentText(context.getString(R.string.notification_called_text,
                        ticket.getQueueName()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openCalled(context, ticket.getId()))
                .setAutoCancel(true)
                .addAction(R.drawable.ic_check, context.getString(R.string.ticket_here_action),
                        here(context, ticket.getId()))
                .addAction(0, context.getString(R.string.notification_called_open),
                        openCalled(context, ticket.getId()));

        // The countdown ticks in the notification itself, and it goes when the window closes.
        if (ticket.getCalledAt() != null) {
            builder.setWhen(System.currentTimeMillis() + remaining)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setTimeoutAfter(remaining);
        }

        NotificationManagerCompat.from(context).notify(id(ticket.getId()), builder.build());
    }

    static void cancel(Context context, String ticketId) {
        NotificationManagerCompat.from(context).cancel(id(ticketId));
    }

    /** One notification per ticket, so being called by two queues shows two. */
    private static int id(String ticketId) {
        return ticketId.hashCode();
    }

    /** "I'm here" confirms in the background (HereReceiver); the app doesn't open. */
    private static PendingIntent here(Context context, String ticketId) {
        Intent intent = new Intent(context, HereReceiver.class);
        intent.putExtra(EXTRA_TICKET_ID, ticketId);
        return PendingIntent.getBroadcast(context, id(ticketId), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Opens the Called screen with My tickets underneath it, so Back from there lands in the
     * app rather than closing it.
     */
    private static PendingIntent openCalled(Context context, String ticketId) {
        Intent home = QueuerHomeActivity.intent(context, QueuerHomeActivity.TAB_TICKETS);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Intent[] stack = {home, CalledActivity.intent(context, ticketId)};
        return PendingIntent.getActivities(context, id(ticketId), stack,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

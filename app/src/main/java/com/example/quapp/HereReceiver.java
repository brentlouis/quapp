package com.example.quapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * "I'm here" pressed on the called notification (canvas 21). Confirms the ticket without
 * opening the app, the same as the button on the Called screen, then clears the notification.
 * Registered in the manifest and not exported, so only Quapp's own notification can send it.
 */
public class HereReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String ticketId = intent.getStringExtra(CalledNotifier.EXTRA_TICKET_ID);
        if (ticketId == null) {
            return;
        }
        final Ticket ticket = ActiveTicketStore.ticket(ticketId);
        CalledNotifier.cancel(context, ticketId);
        if (ticket == null || ticket.getStatus() != Ticket.Status.CALLED) {
            // Too late: the window closed or the counter moved on. The ticket screen says how.
            Toast.makeText(context, R.string.notification_here_too_late, Toast.LENGTH_LONG).show();
            return;
        }
        // goAsync keeps the app alive past onReceive until the server has answered
        final PendingResult pending = goAsync();
        ActiveTicketStore.here(context, ticketId, new ActiveTicketStore.Done<Ticket>() {
            @Override
            public void onDone(@Nullable Ticket confirmed) {
                Toast.makeText(context, context.getString(R.string.notification_here_done,
                        ticket.getTicketNumber()), Toast.LENGTH_LONG).show();
                pending.finish();
            }

            @Override
            public void onFailed(@NonNull ApiError error) {
                Toast.makeText(context, error.is(ApiError.OFFLINE)
                        ? context.getString(R.string.api_offline) : error.message,
                        Toast.LENGTH_LONG).show();
                pending.finish();
            }
        });
    }
}

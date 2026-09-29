package com.example.quapp;

import android.app.Application;

/**
 * Runs once when the app process starts, before any screen. Sets up the notification channel
 * and listens for the counter calling one of the queuer's tickets.
 *
 * The listener stands in for a push message from the server: today FakeData calls it when the
 * console calls someone, later a Firebase message will (DECISIONS.md "Notifications").
 */
public class QuappApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CalledNotifier.createChannel(this);

        FakeData.setCounterListener(new FakeData.CounterListener() {
            @Override
            public void onCalled(Ticket ticket) {
                // Only the queuer's own tickets; the console calls strangers all day.
                if (ActiveTicketStore.holds(ticket.getId())) {
                    CalledNotifier.show(QuappApplication.this, ticket);
                }
            }

            @Override
            public void onLeftCounter(String ticketId) {
                CalledNotifier.cancel(QuappApplication.this, ticketId);
            }
        });
    }
}

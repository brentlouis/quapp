package com.example.quapp;

public final class ActiveTicketStore {

    private static Ticket activeTicket;

    private ActiveTicketStore() {
        // Utility class.
    }

    public static void setTicket(Ticket ticket) {
        activeTicket = ticket;
    }

    public static Ticket getTicket() {
        return activeTicket;
    }

    public static void clearTicket() {
        activeTicket = null;
    }
}
package com.example.quapp;

public class Ticket {

    public enum Status {
        WAITING,
        CALLED,
        SERVED,
        NO_SHOW
    }

    private final String id;
    private final String queueId;
    private final String queueName;
    private final String venue;
    private final String holderName;
    private final String holderPhone;
    private final int ticketNumber;
    private final int position;
    private final int estimatedWaitMinutes;
    private final Status status;

    public Ticket(String id,
                  String queueId,
                  String queueName,
                  String venue,
                  String holderName,
                  String holderPhone,
                  int ticketNumber,
                  int position,
                  int estimatedWaitMinutes,
                  Status status) {
        this.id = id;
        this.queueId = queueId;
        this.queueName = queueName;
        this.venue = venue;
        this.holderName = holderName;
        this.holderPhone = holderPhone;
        this.ticketNumber = ticketNumber;
        this.position = position;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getQueueId() {
        return queueId;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getVenue() {
        return venue;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getHolderPhone() {
        return holderPhone;
    }

    public int getTicketNumber() {
        return ticketNumber;
    }

    public int getPosition() {
        return position;
    }

    public int getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public Status getStatus() {
        return status;
    }
}
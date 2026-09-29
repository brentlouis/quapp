package com.example.quapp;

import androidx.annotation.Nullable;

import java.time.Instant;

/**
 * One person's place in one queue. Field for field the Ticket in MODELS.md.
 *
 * {@code ticketNumber} and {@code position} are different on purpose: the number is fixed
 * (you're #47 for good), the position counts down as the line moves (1 = next).
 */
public class Ticket {

    public enum Status {
        WAITING,
        CALLED,
        SERVED,
        NO_SHOW,
        /** The owner closed the queue. Never counts as a no-show. */
        QUEUE_CLOSED,
        /** The owner took the ticket out of the line, with a RemovalReason. */
        REMOVED
    }

    public enum RemovalReason {
        /** Counts as a no-show and goes to the admin. */
        PRANK,
        DUPLICATE,
        ASKED_TO_LEAVE
    }

    private final String id;
    private final String queueId;
    private final String queueName;
    private final String venue;
    private final String holderName;
    @Nullable
    private final String holderPhone;
    private final boolean walkIn;
    private final int ticketNumber;
    private final int position;
    private final int estimatedWaitMinutes;
    private final Status status;
    private final Instant joinedAt;
    @Nullable
    private final Instant calledAt;
    /** When the holder tapped "I'm here"; the ticket stays CALLED until the counter serves it. */
    @Nullable
    private final Instant hereAt;
    @Nullable
    private final Instant finishedAt;
    private final boolean movedBack;
    @Nullable
    private final Instant movedBackAt;
    @Nullable
    private final RemovalReason removalReason;

    private Ticket(Builder b) {
        id = b.id;
        queueId = b.queueId;
        queueName = b.queueName;
        venue = b.venue;
        holderName = b.holderName;
        holderPhone = b.holderPhone;
        walkIn = b.walkIn;
        ticketNumber = b.ticketNumber;
        position = b.position;
        estimatedWaitMinutes = b.estimatedWaitMinutes;
        status = b.status;
        joinedAt = b.joinedAt;
        calledAt = b.calledAt;
        hereAt = b.hereAt;
        finishedAt = b.finishedAt;
        movedBack = b.movedBack;
        movedBackAt = b.movedBackAt;
        removalReason = b.removalReason;
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.id = id;
        b.queueId = queueId;
        b.queueName = queueName;
        b.venue = venue;
        b.holderName = holderName;
        b.holderPhone = holderPhone;
        b.walkIn = walkIn;
        b.ticketNumber = ticketNumber;
        b.position = position;
        b.estimatedWaitMinutes = estimatedWaitMinutes;
        b.status = status;
        b.joinedAt = joinedAt;
        b.calledAt = calledAt;
        b.hereAt = hereAt;
        b.finishedAt = finishedAt;
        b.movedBack = movedBack;
        b.movedBackAt = movedBackAt;
        b.removalReason = removalReason;
        return b;
    }

    public String getId() { return id; }
    public String getQueueId() { return queueId; }
    public String getQueueName() { return queueName; }
    public String getVenue() { return venue; }
    public String getHolderName() { return holderName; }
    @Nullable public String getHolderPhone() { return holderPhone; }
    public boolean isWalkIn() { return walkIn; }
    public int getTicketNumber() { return ticketNumber; }
    public int getPosition() { return position; }
    public int getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public Status getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    @Nullable public Instant getCalledAt() { return calledAt; }
    @Nullable public Instant getHereAt() { return hereAt; }
    @Nullable public Instant getFinishedAt() { return finishedAt; }
    public boolean isMovedBack() { return movedBack; }
    @Nullable public Instant getMovedBackAt() { return movedBackAt; }
    @Nullable public RemovalReason getRemovalReason() { return removalReason; }

    /** Still holding a place in line (waiting or being called). */
    public boolean isLive() {
        return status == Status.WAITING || status == Status.CALLED;
    }

    /**
     * A copy with a new status. Being called stamps calledAt; any ending stamps finishedAt,
     * so History can show the date.
     */
    public Ticket withStatus(Status newStatus) {
        Builder b = toBuilder().setStatus(newStatus);
        if (newStatus == Status.CALLED) {
            b.setCalledAt(Instant.now());
        } else if (newStatus != Status.WAITING) {
            b.setFinishedAt(Instant.now());
        }
        return b.build();
    }

    public static class Builder {

        private String id;
        private String queueId;
        private String queueName = "";
        private String venue = "";
        private String holderName = "";
        private String holderPhone;
        private boolean walkIn;
        private int ticketNumber;
        private int position;
        private int estimatedWaitMinutes;
        private Status status = Status.WAITING;
        private Instant joinedAt = Instant.now();
        private Instant calledAt;
        private Instant hereAt;
        private Instant finishedAt;
        private boolean movedBack;
        private Instant movedBackAt;
        private RemovalReason removalReason;

        public Builder setId(String id) { this.id = id; return this; }

        public Builder setQueue(String queueId, String queueName, String venue) {
            this.queueId = queueId;
            this.queueName = queueName;
            this.venue = venue;
            return this;
        }

        public Builder setHolder(String name, @Nullable String phone) {
            this.holderName = name;
            this.holderPhone = phone;
            return this;
        }

        public Builder setWalkIn(boolean walkIn) { this.walkIn = walkIn; return this; }
        public Builder setTicketNumber(int number) { this.ticketNumber = number; return this; }
        public Builder setPosition(int position) { this.position = position; return this; }
        public Builder setEstimatedWaitMinutes(int minutes) { this.estimatedWaitMinutes = minutes; return this; }
        public Builder setStatus(Status status) { this.status = status; return this; }
        public Builder setJoinedAt(Instant at) { this.joinedAt = at; return this; }
        public Builder setCalledAt(@Nullable Instant at) { this.calledAt = at; return this; }
        public Builder setFinishedAt(@Nullable Instant at) { this.finishedAt = at; return this; }

        public Builder setMovedBack(@Nullable Instant at) {
            this.movedBack = at != null;
            this.movedBackAt = at;
            return this;
        }

        public Builder setRemovalReason(@Nullable RemovalReason reason) {
            this.removalReason = reason;
            return this;
        }

        public Ticket build() {
            if (id == null || queueId == null) {
                throw new IllegalStateException("A ticket needs an id and a queue.");
            }
            return new Ticket(this);
        }
    }
}

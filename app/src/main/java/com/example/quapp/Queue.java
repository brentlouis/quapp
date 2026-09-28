package com.example.quapp;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A public queue: what it is, when it runs, which checks it uses, and its live numbers.
 * Field for field the Queue in MODELS.md; change the contract there first.
 */
public class Queue {

    public enum Status {
        /** Scheduled, not open yet. People can join early and hold a number. */
        UPCOMING,
        OPEN,
        /** No new joins; people already in line keep their spot. */
        PAUSED,
        CLOSED
    }

    // Who runs it
    private final String id;
    private final String organizerId;
    private final String organizerName;
    private final boolean organizerVerified;

    // What it is
    private final String name;
    private final Category category;
    private final String shortDescription;
    @Nullable
    private final String details;
    @Nullable
    private final String bring;

    // Where
    private final String venue;
    private final String municipality;
    private final double latitude;
    private final double longitude;

    // When (same hours every day from startDate to endDate)
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalTime opensAt;
    private final LocalTime closesAt;
    private final Status status;
    @Nullable
    private final Instant pausedAt;
    @Nullable
    private final Instant closedAt;

    // Checks
    private final boolean gracePeriodEnabled;
    private final boolean noShowCooldownEnabled;
    private final boolean proximityCheckEnabled;
    private final int joinRadiusMeters;

    // Live numbers (the server computes these)
    private final int peopleWaiting;
    @Nullable
    private final Integer nowServing;
    private final int estimatedWaitMinutes;

    private Queue(Builder b) {
        id = b.id;
        organizerId = b.organizerId;
        organizerName = b.organizerName;
        organizerVerified = b.organizerVerified;
        name = b.name;
        category = b.category;
        shortDescription = b.shortDescription;
        details = b.details;
        bring = b.bring;
        venue = b.venue;
        municipality = b.municipality;
        latitude = b.latitude;
        longitude = b.longitude;
        startDate = b.startDate;
        endDate = b.endDate;
        opensAt = b.opensAt;
        closesAt = b.closesAt;
        status = b.status;
        pausedAt = b.pausedAt;
        closedAt = b.closedAt;
        gracePeriodEnabled = b.gracePeriodEnabled;
        noShowCooldownEnabled = b.noShowCooldownEnabled;
        proximityCheckEnabled = b.proximityCheckEnabled;
        joinRadiusMeters = b.joinRadiusMeters;
        peopleWaiting = b.peopleWaiting;
        nowServing = b.nowServing;
        estimatedWaitMinutes = b.estimatedWaitMinutes;
    }

    /** A builder pre-filled with this queue's values, for making a changed copy. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.id = id;
        b.organizerId = organizerId;
        b.organizerName = organizerName;
        b.organizerVerified = organizerVerified;
        b.name = name;
        b.category = category;
        b.shortDescription = shortDescription;
        b.details = details;
        b.bring = bring;
        b.venue = venue;
        b.municipality = municipality;
        b.latitude = latitude;
        b.longitude = longitude;
        b.startDate = startDate;
        b.endDate = endDate;
        b.opensAt = opensAt;
        b.closesAt = closesAt;
        b.status = status;
        b.pausedAt = pausedAt;
        b.closedAt = closedAt;
        b.gracePeriodEnabled = gracePeriodEnabled;
        b.noShowCooldownEnabled = noShowCooldownEnabled;
        b.proximityCheckEnabled = proximityCheckEnabled;
        b.joinRadiusMeters = joinRadiusMeters;
        b.peopleWaiting = peopleWaiting;
        b.nowServing = nowServing;
        b.estimatedWaitMinutes = estimatedWaitMinutes;
        return b;
    }

    public String getId() { return id; }
    public String getOrganizerId() { return organizerId; }
    public String getOrganizerName() { return organizerName; }
    public boolean isOrganizerVerified() { return organizerVerified; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public String getShortDescription() { return shortDescription; }
    @Nullable public String getDetails() { return details; }
    @Nullable public String getBring() { return bring; }
    public String getVenue() { return venue; }
    public String getMunicipality() { return municipality; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalTime getOpensAt() { return opensAt; }
    public LocalTime getClosesAt() { return closesAt; }
    public Status getStatus() { return status; }
    @Nullable public Instant getPausedAt() { return pausedAt; }
    @Nullable public Instant getClosedAt() { return closedAt; }
    public boolean isGracePeriodEnabled() { return gracePeriodEnabled; }
    public boolean isNoShowCooldownEnabled() { return noShowCooldownEnabled; }
    public boolean isProximityCheckEnabled() { return proximityCheckEnabled; }
    public int getJoinRadiusMeters() { return joinRadiusMeters; }
    public int getPeopleWaiting() { return peopleWaiting; }
    @Nullable public Integer getNowServing() { return nowServing; }
    public int getEstimatedWaitMinutes() { return estimatedWaitMinutes; }

    /** Open and Upcoming queues take joins (Upcoming lets people hold a number early). */
    public boolean acceptsJoins() {
        return status == Status.OPEN || status == Status.UPCOMING;
    }

    /** Named setters instead of a 27-argument constructor, so two booleans can't be swapped silently. */
    public static class Builder {

        private String id;
        private String organizerId = "";
        private String organizerName = "";
        private boolean organizerVerified;
        private String name;
        private Category category = Category.OTHER;
        private String shortDescription = "";
        private String details;
        private String bring;
        private String venue = "";
        private String municipality = "";
        private double latitude;
        private double longitude;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalTime opensAt = LocalTime.of(8, 0);
        private LocalTime closesAt = LocalTime.of(17, 0);
        private Status status = Status.OPEN;
        private Instant pausedAt;
        private Instant closedAt;
        private boolean gracePeriodEnabled;
        private boolean noShowCooldownEnabled;
        private boolean proximityCheckEnabled;
        private int joinRadiusMeters;
        private int peopleWaiting;
        private Integer nowServing;
        private int estimatedWaitMinutes;

        public Builder setId(String id) { this.id = id; return this; }

        public Builder setOrganizer(String organizerId, String organizerName, boolean verified) {
            this.organizerId = organizerId;
            this.organizerName = organizerName;
            this.organizerVerified = verified;
            return this;
        }

        public Builder setName(String name) { this.name = name; return this; }
        public Builder setCategory(Category category) { this.category = category; return this; }
        public Builder setShortDescription(String text) { this.shortDescription = text; return this; }
        public Builder setDetails(@Nullable String details) { this.details = details; return this; }
        public Builder setBring(@Nullable String bring) { this.bring = bring; return this; }
        public Builder setVenue(String venue) { this.venue = venue; return this; }
        public Builder setMunicipality(String municipality) { this.municipality = municipality; return this; }

        public Builder setLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        /** One day: pass the same date twice. */
        public Builder setSchedule(LocalDate startDate, LocalDate endDate,
                                   LocalTime opensAt, LocalTime closesAt) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.opensAt = opensAt;
            this.closesAt = closesAt;
            return this;
        }

        public Builder setClosesAt(LocalTime closesAt) { this.closesAt = closesAt; return this; }
        public Builder setStatus(Status status) { this.status = status; return this; }
        public Builder setPausedAt(@Nullable Instant pausedAt) { this.pausedAt = pausedAt; return this; }
        public Builder setClosedAt(@Nullable Instant closedAt) { this.closedAt = closedAt; return this; }
        public Builder setGracePeriodEnabled(boolean on) { this.gracePeriodEnabled = on; return this; }
        public Builder setNoShowCooldownEnabled(boolean on) { this.noShowCooldownEnabled = on; return this; }

        /** Proximity on with a radius, or off (radius 0). */
        public Builder setProximity(boolean on, int radiusMeters) {
            this.proximityCheckEnabled = on;
            this.joinRadiusMeters = on ? radiusMeters : 0;
            return this;
        }

        public Builder setPeopleWaiting(int count) { this.peopleWaiting = count; return this; }
        public Builder setNowServing(@Nullable Integer number) { this.nowServing = number; return this; }
        public Builder setEstimatedWaitMinutes(int minutes) { this.estimatedWaitMinutes = minutes; return this; }

        public Queue build() {
            if (id == null || name == null || startDate == null || endDate == null) {
                throw new IllegalStateException("A queue needs an id, a name and a schedule.");
            }
            return new Queue(this);
        }
    }
}

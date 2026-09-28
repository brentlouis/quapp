package com.example.quapp;

public class Queue {

    public enum Status {
        OPEN,
        PAUSED,
        CLOSED
    }

    // Identity and location
    private final String id;
    private final String name;
    private final String venue;
    private final String municipality;
    private final double latitude;
    private final double longitude;

    // Descriptive
    private final String category;
    private final String description;
    private final String serviceHours;

    // Live state
    private final int peopleWaiting;
    private final int estimatedWaitMinutes;
    private final Status status;

    // Verification config
    private final boolean smsOtpEnabled;
    private final boolean gracePeriodEnabled;
    private final boolean noShowPenaltyEnabled;
    private final boolean proximityCheckEnabled;

    private Queue(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.venue = builder.venue;
        this.municipality = builder.municipality;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.category = builder.category;
        this.description = builder.description;
        this.serviceHours = builder.serviceHours;
        this.peopleWaiting = builder.peopleWaiting;
        this.estimatedWaitMinutes = builder.estimatedWaitMinutes;
        this.status = builder.status;
        this.smsOtpEnabled = builder.smsOtpEnabled;
        this.gracePeriodEnabled = builder.gracePeriodEnabled;
        this.noShowPenaltyEnabled = builder.noShowPenaltyEnabled;
        this.proximityCheckEnabled = builder.proximityCheckEnabled;
    }

    /** A builder pre-filled with this queue's values, for making a changed copy. */
    public Builder toBuilder() {
        return new Builder()
                .setId(id)
                .setName(name)
                .setVenue(venue)
                .setMunicipality(municipality)
                .setLocation(latitude, longitude)
                .setCategory(category)
                .setDescription(description)
                .setServiceHours(serviceHours)
                .setPeopleWaiting(peopleWaiting)
                .setEstimatedWaitMinutes(estimatedWaitMinutes)
                .setStatus(status)
                .setSmsOtpEnabled(smsOtpEnabled)
                .setGracePeriodEnabled(gracePeriodEnabled)
                .setNoShowPenaltyEnabled(noShowPenaltyEnabled)
                .setProximityCheckEnabled(proximityCheckEnabled);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVenue() {
        return venue;
    }

    public String getMunicipality() {
        return municipality;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getServiceHours() {
        return serviceHours;
    }

    public int getPeopleWaiting() {
        return peopleWaiting;
    }

    public int getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public Status getStatus() {
        return status;
    }

    /** Only an OPEN queue accepts new joins. Paused and closed queues both refuse them. */
    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public boolean isSmsOtpEnabled() {
        return smsOtpEnabled;
    }

    public boolean isGracePeriodEnabled() {
        return gracePeriodEnabled;
    }

    public boolean isNoShowPenaltyEnabled() {
        return noShowPenaltyEnabled;
    }

    public boolean isProximityCheckEnabled() {
        return proximityCheckEnabled;
    }

    public boolean hasAnyVerification() {
        return smsOtpEnabled
                || gracePeriodEnabled
                || noShowPenaltyEnabled
                || proximityCheckEnabled;
    }

    /**
     * Named setters replace the old 16-argument constructor, so two booleans
     * can no longer be swapped silently.
     */
    public static class Builder {

        private String id;
        private String name;
        private String venue;
        private String municipality;
        private double latitude;
        private double longitude;
        private String category;
        private String description = "";
        private String serviceHours;
        private int peopleWaiting;
        private int estimatedWaitMinutes;
        private Status status = Status.OPEN;
        private boolean smsOtpEnabled;
        private boolean gracePeriodEnabled;
        private boolean noShowPenaltyEnabled;
        private boolean proximityCheckEnabled;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setVenue(String venue) {
            this.venue = venue;
            return this;
        }

        public Builder setMunicipality(String municipality) {
            this.municipality = municipality;
            return this;
        }

        public Builder setLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setServiceHours(String serviceHours) {
            this.serviceHours = serviceHours;
            return this;
        }

        public Builder setPeopleWaiting(int peopleWaiting) {
            this.peopleWaiting = peopleWaiting;
            return this;
        }

        public Builder setEstimatedWaitMinutes(int estimatedWaitMinutes) {
            this.estimatedWaitMinutes = estimatedWaitMinutes;
            return this;
        }

        public Builder setStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder setSmsOtpEnabled(boolean enabled) {
            this.smsOtpEnabled = enabled;
            return this;
        }

        public Builder setGracePeriodEnabled(boolean enabled) {
            this.gracePeriodEnabled = enabled;
            return this;
        }

        public Builder setNoShowPenaltyEnabled(boolean enabled) {
            this.noShowPenaltyEnabled = enabled;
            return this;
        }

        public Builder setProximityCheckEnabled(boolean enabled) {
            this.proximityCheckEnabled = enabled;
            return this;
        }

        public Queue build() {
            if (id == null || name == null) {
                throw new IllegalStateException("A queue needs at least an id and a name.");
            }
            return new Queue(this);
        }
    }
}

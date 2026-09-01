package com.example.quapp;

public class Queue {

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
    private final boolean open;

    // Verification config
    private final boolean smsOtpEnabled;
    private final boolean gracePeriodEnabled;
    private final boolean noShowPenaltyEnabled;
    private final boolean proximityCheckEnabled;

    public Queue(String id,
                 String name,
                 String venue,
                 String municipality,
                 double latitude,
                 double longitude,
                 String category,
                 String description,
                 String serviceHours,
                 int peopleWaiting,
                 int estimatedWaitMinutes,
                 boolean open,
                 boolean smsOtpEnabled,
                 boolean gracePeriodEnabled,
                 boolean noShowPenaltyEnabled,
                 boolean proximityCheckEnabled) {
        this.id = id;
        this.name = name;
        this.venue = venue;
        this.municipality = municipality;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.description = description;
        this.serviceHours = serviceHours;
        this.peopleWaiting = peopleWaiting;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.open = open;
        this.smsOtpEnabled = smsOtpEnabled;
        this.gracePeriodEnabled = gracePeriodEnabled;
        this.noShowPenaltyEnabled = noShowPenaltyEnabled;
        this.proximityCheckEnabled = proximityCheckEnabled;
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

    public boolean isOpen() {
        return open;
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
}
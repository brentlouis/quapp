package com.example.quapp;

import androidx.annotation.Nullable;

import java.time.Instant;

/**
 * An account (MODELS.md). One type for everyone; which side of the app you use is a local
 * choice (Session). Password, install id and phone_verified stay on the server.
 */
public class User {

    public enum Status {
        ACTIVE,
        /** Blocked at login (canvas 55). Their live queues were closed, tickets released. */
        SUSPENDED
    }

    private final String id;
    private final String name;
    private final String phone;
    private final Status status;
    @Nullable
    private final String suspendedReason;
    @Nullable
    private final Instant suspendedAt;
    private final VerificationStatus verificationStatus;
    @Nullable
    private final String organizationName;

    public User(String id, String name, String phone, Status status,
                @Nullable String suspendedReason, @Nullable Instant suspendedAt,
                VerificationStatus verificationStatus, @Nullable String organizationName) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.status = status;
        this.suspendedReason = suspendedReason;
        this.suspendedAt = suspendedAt;
        this.verificationStatus = verificationStatus;
        this.organizationName = organizationName;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public Status getStatus() { return status; }
    @Nullable public String getSuspendedReason() { return suspendedReason; }
    @Nullable public Instant getSuspendedAt() { return suspendedAt; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    @Nullable public String getOrganizationName() { return organizationName; }
}

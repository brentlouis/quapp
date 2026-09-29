package com.example.quapp;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.time.Instant;

/**
 * An organizer asking for the badge (canvas 50). Field for field the VerificationRequest in
 * MODELS.md. On the server the answers are deleted after the decision; only the status and the
 * admin's note stay.
 */
public class VerificationRequest {

    /** What kind of organization it is; the admin uses it to know which office to call. */
    public enum OrganizationType {
        LGU_OFFICE(R.string.org_type_lgu),
        BARANGAY(R.string.org_type_barangay),
        HEALTH(R.string.org_type_health),
        SCHOOL(R.string.org_type_school),
        COMMUNITY_GROUP(R.string.org_type_community),
        OTHER(R.string.org_type_other);

        @StringRes
        public final int label;

        OrganizationType(@StringRes int label) {
            this.label = label;
        }
    }

    private final String id;
    private final String userId;
    private final String organizationName;
    private final OrganizationType organizationType;
    private final String position;
    private final String officePhone;
    private final VerificationStatus status;
    @Nullable
    private final String adminNote;
    private final Instant createdAt;
    @Nullable
    private final Instant decidedAt;

    public VerificationRequest(String id, String userId, String organizationName,
                               OrganizationType organizationType, String position,
                               String officePhone, VerificationStatus status,
                               @Nullable String adminNote, Instant createdAt,
                               @Nullable Instant decidedAt) {
        this.id = id;
        this.userId = userId;
        this.organizationName = organizationName;
        this.organizationType = organizationType;
        this.position = position;
        this.officePhone = officePhone;
        this.status = status;
        this.adminNote = adminNote;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getOrganizationName() { return organizationName; }
    public OrganizationType getOrganizationType() { return organizationType; }
    public String getPosition() { return position; }
    public String getOfficePhone() { return officePhone; }
    public VerificationStatus getStatus() { return status; }
    @Nullable public String getAdminNote() { return adminNote; }
    public Instant getCreatedAt() { return createdAt; }
    @Nullable public Instant getDecidedAt() { return decidedAt; }

    /** The same request with the admin's decision on it. */
    public VerificationRequest decided(VerificationStatus decision) {
        return new VerificationRequest(id, userId, organizationName, organizationType, position,
                officePhone, decision, adminNote, createdAt, Instant.now());
    }
}

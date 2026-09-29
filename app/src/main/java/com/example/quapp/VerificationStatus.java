package com.example.quapp;

/**
 * Where an organizer's badge stands (MODELS.md). Quapp checks by calling the office's public
 * number, by hand (DECISIONS.md "Organizer verification: optional, checked by phone").
 */
public enum VerificationStatus {
    /** Never asked. Can post, with the unverified limits. */
    NONE,
    /** Asked; the admin hasn't called yet. Same limits as NONE. */
    PENDING,
    VERIFIED,
    /** The call didn't confirm it. Can ask again. */
    REJECTED,
    /** Was verified, then the admin took the badge away. Queues keep running. */
    REVOKED
}

package com.example.quapp;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.time.Instant;

/**
 * Someone reporting a queue (canvas 54). Field for field the Report in MODELS.md. Who reported
 * is kept on the server only, to stop one person flooding reports; the organizer never sees it.
 */
public class Report {

    /** The four reasons on the report screen, in the order they're shown. */
    public enum Reason {
        FAKE(R.string.report_reason_fake, R.string.report_reason_fake_body),
        ASKED_FOR_MONEY(R.string.report_reason_money, R.string.report_reason_money_body),
        WRONG_PLACE_OR_TIME(R.string.report_reason_place, R.string.report_reason_place_body),
        OTHER(R.string.report_reason_other, R.string.report_reason_other_body);

        @StringRes
        public final int label;
        @StringRes
        public final int body;

        Reason(@StringRes int label, @StringRes int body) {
            this.label = label;
            this.body = body;
        }
    }

    private final String id;
    private final String queueId;
    private final Reason reason;
    @Nullable
    private final String details;
    private final Instant createdAt;

    public Report(String id, String queueId, Reason reason, @Nullable String details,
                  Instant createdAt) {
        this.id = id;
        this.queueId = queueId;
        this.reason = reason;
        this.details = details;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getQueueId() { return queueId; }
    public Reason getReason() { return reason; }
    @Nullable public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}

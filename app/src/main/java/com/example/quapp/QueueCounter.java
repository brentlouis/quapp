package com.example.quapp;

/** An owner-managed counter inside one queue; its process-local serving state lives in FakeData. */
public final class QueueCounter {
    private final String id;
    private final String queueId;
    private final String name;
    QueueCounter(String id, String queueId, String name) { this.id = id; this.queueId = queueId; this.name = name; }
    public String getId() { return id; }
    public String getQueueId() { return queueId; }
    public String getName() { return name; }
}

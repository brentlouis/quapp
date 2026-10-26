package com.example.quapp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Immutable, unsaved form snapshots; no FakeData operations or process-death persistence. */
final class PreviewSnapshots {
    private static final Map<String, Queue> snapshots = new HashMap<>();
    private PreviewSnapshots() { }
    static String put(Queue snapshot) { String id = UUID.randomUUID().toString(); snapshots.put(id, snapshot); return id; }
    static Queue get(String id) { return snapshots.get(id); }
    static void remove(String id) { snapshots.remove(id); }
}

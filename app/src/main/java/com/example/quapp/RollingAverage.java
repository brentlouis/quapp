package com.example.quapp;

import java.util.ArrayDeque;

/**
 * Average of the last {@code windowSize} samples. Older samples fall out as new
 * ones arrive, so the average follows how fast the queue is moving right now
 * rather than how fast it moved this morning.
 */
public final class RollingAverage {

    private final int windowSize;
    private final ArrayDeque<Long> samples = new ArrayDeque<>();
    private long sum;

    public RollingAverage(int windowSize) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be at least 1");
        }
        this.windowSize = windowSize;
    }

    public void add(long sample) {
        samples.addLast(sample);
        sum += sample;

        if (samples.size() > windowSize) {
            sum -= samples.removeFirst();
        }
    }

    public int size() {
        return samples.size();
    }

    public boolean isEmpty() {
        return samples.isEmpty();
    }

    /** Returns 0 when there are no samples yet. */
    public double average() {
        if (samples.isEmpty()) {
            return 0;
        }
        return (double) sum / samples.size();
    }
}

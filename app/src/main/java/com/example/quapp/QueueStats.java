package com.example.quapp;

/** Read-only snapshot of one queue's numbers, for the Dashboard and Insights screens. */
public class QueueStats {

    private final int servedToday;
    private final int noShowsToday;
    private final int waitingNow;
    private final double averageServiceMinutes;
    private final int serviceSampleCount;
    private final int projectedWaitMinutes;

    public QueueStats(int servedToday,
                      int noShowsToday,
                      int waitingNow,
                      double averageServiceMinutes,
                      int serviceSampleCount,
                      int projectedWaitMinutes) {
        this.servedToday = servedToday;
        this.noShowsToday = noShowsToday;
        this.waitingNow = waitingNow;
        this.averageServiceMinutes = averageServiceMinutes;
        this.serviceSampleCount = serviceSampleCount;
        this.projectedWaitMinutes = projectedWaitMinutes;
    }

    public int getServedToday() {
        return servedToday;
    }

    public int getNoShowsToday() {
        return noShowsToday;
    }

    public int getWaitingNow() {
        return waitingNow;
    }

    public double getAverageServiceMinutes() {
        return averageServiceMinutes;
    }

    public int getServiceSampleCount() {
        return serviceSampleCount;
    }

    public int getProjectedWaitMinutes() {
        return projectedWaitMinutes;
    }

    /** Share of finished tickets that were no-shows, 0–100. 0 when nobody has finished yet. */
    public int getNoShowRatePercent() {
        int finished = servedToday + noShowsToday;
        if (finished == 0) {
            return 0;
        }
        return Math.round(noShowsToday * 100f / finished);
    }
}

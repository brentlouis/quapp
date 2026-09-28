package com.example.quapp;

/** Read-only snapshot of one queue's numbers, for the owner's Today tab and Insights. */
public class QueueStats {

    /** Where today's minutes-per-person came from (MODELS.md "The estimator"). */
    public enum EstimateSource {
        /** The last 5 service times, while the queue has little history today. */
        ROLLING_AVERAGE,
        /** The server's learning model. */
        MODEL
    }

    private final int servedToday;
    private final int noShowsToday;
    private final int waitingNow;
    private final double averageServiceMinutes;
    private final int serviceSampleCount;
    private final int projectedWaitMinutes;
    private final EstimateSource estimateSource;
    private final int modelSamples;

    public QueueStats(int servedToday,
                      int noShowsToday,
                      int waitingNow,
                      double averageServiceMinutes,
                      int serviceSampleCount,
                      int projectedWaitMinutes,
                      EstimateSource estimateSource,
                      int modelSamples) {
        this.servedToday = servedToday;
        this.noShowsToday = noShowsToday;
        this.waitingNow = waitingNow;
        this.averageServiceMinutes = averageServiceMinutes;
        this.serviceSampleCount = serviceSampleCount;
        this.projectedWaitMinutes = projectedWaitMinutes;
        this.estimateSource = estimateSource;
        this.modelSamples = modelSamples;
    }

    public EstimateSource getEstimateSource() {
        return estimateSource;
    }

    /** How many served people the model has learned from in total. */
    public int getModelSamples() {
        return modelSamples;
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

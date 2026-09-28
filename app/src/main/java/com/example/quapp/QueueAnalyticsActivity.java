package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * The owner's numbers for one queue: today's counts and the rolling-average
 * wait forecast that also drives the ETA queuers see on Browse.
 */
public class QueueAnalyticsActivity extends AppCompatActivity {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_ANALYTICS_QUEUE_ID";

    private String queueId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_analytics);
        SystemBars.applyPadding(findViewById(R.id.analytics_root));

        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);

        findViewById(R.id.analytics_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Queue queue = FakeData.queueById(queueId);
        if (queue == null) {
            finish();
            return;
        }

        QueueStats stats = FakeData.stats(queueId);

        TextView queueName = findViewById(R.id.analytics_queue_name);
        queueName.setText(queue.getName());

        bindStat(R.id.analytics_served, R.string.analytics_served_label,
                String.valueOf(stats.getServedToday()));
        bindStat(R.id.analytics_no_shows, R.string.analytics_no_shows_label,
                String.valueOf(stats.getNoShowsToday()));
        bindStat(R.id.analytics_no_show_rate, R.string.analytics_no_show_rate_label,
                getString(R.string.analytics_percent_format, stats.getNoShowRatePercent()));
        bindStat(R.id.analytics_waiting, R.string.analytics_waiting_label,
                String.valueOf(stats.getWaitingNow()));

        bindForecast(stats);
    }

    private void bindStat(int includeId, int labelRes, String value) {
        View cell = findViewById(includeId);
        TextView label = cell.findViewById(R.id.stat_label);
        TextView valueText = cell.findViewById(R.id.stat_value);
        label.setText(labelRes);
        valueText.setText(value);
    }

    private void bindForecast(QueueStats stats) {
        TextView averageValue = findViewById(R.id.analytics_average_value);
        TextView averageBasis = findViewById(R.id.analytics_average_basis);
        TextView projectedValue = findViewById(R.id.analytics_projected_value);

        // Locale.US keeps the decimal point a point, matching how the countdown is formatted.
        averageValue.setText(String.format(Locale.US,
                getString(R.string.analytics_average_format), stats.getAverageServiceMinutes()));

        int samples = stats.getServiceSampleCount();
        if (samples == 0) {
            averageBasis.setText(R.string.analytics_average_none);
        } else {
            averageBasis.setText(getResources().getQuantityString(
                    R.plurals.analytics_average_basis, samples, samples));
        }

        projectedValue.setText(getString(R.string.analytics_projected_format,
                stats.getProjectedWaitMinutes()));
    }
}

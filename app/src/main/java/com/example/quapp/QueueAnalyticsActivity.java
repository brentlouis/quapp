package com.example.quapp;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * The owner's numbers for one queue: the rolling-average wait forecast (the same number that
 * drives the ETA queuers see on Browse) and today's counts.
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

        Queue queue = Queues.get(queueId);  // opened from the console or Today, which loaded it
        if (queue == null) {
            finish();
            return;
        }
        TextView queueName = findViewById(R.id.analytics_queue_name);
        queueName.setText(getString(R.string.analytics_subtitle_format, queue.getName()));

        // The last numbers Today or Your queues loaded, if any, then the latest
        QueueStats last = MyQueues.lastStats().get(queueId);
        if (last != null) {
            bind(last);
        }
        ApiClient.api(this).stats(queueId).enqueue(new ApiCallback<QueueStats>(this) {
            @Override
            protected void onSuccess(@Nullable QueueStats stats) {
                if (stats != null) {
                    bind(stats);
                }
            }
        });
    }

    private void bind(QueueStats stats) {
        bindForecast(stats);
        bindToday(stats);
    }

    private void bindForecast(QueueStats stats) {
        TextView projectedValue = findViewById(R.id.analytics_projected_value);
        projectedValue.setText(getString(R.string.analytics_projected_format,
                stats.getProjectedWaitMinutes()));

        // Where the estimate came from: the ML model, or the rolling average while this queue
        // has too little history today (DECISIONS.md "Wait-time estimation learns online").
        ((TextView) findViewById(R.id.analytics_source)).setText(
                stats.getEstimateSource() == QueueStats.EstimateSource.MODEL
                        ? getString(R.string.analytics_source_model, stats.getModelSamples())
                        : getString(R.string.analytics_source_rolling));

        TextView basis = findViewById(R.id.analytics_average_basis);
        int samples = stats.getServiceSampleCount();
        if (samples == 0) {
            basis.setText(R.string.analytics_average_none);
            return;
        }

        // "1.3 min per person" in ink and bold, then how it was worked out, then who's waiting.
        // Locale.US keeps the decimal point a point, matching how the countdown is formatted.
        String average = String.format(Locale.US,
                getString(R.string.analytics_average_format), stats.getAverageServiceMinutes());
        SpannableStringBuilder text = new SpannableStringBuilder(average);
        text.setSpan(new StyleSpan(Typeface.BOLD), 0, average.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.ink)),
                0, average.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.append(' ');
        text.append(getResources().getQuantityString(
                R.plurals.analytics_average_basis, samples, samples));
        text.append(getString(R.string.analytics_waiting_suffix, stats.getWaitingNow()));
        basis.setText(text);
    }

    private void bindToday(QueueStats stats) {
        LinearLayout slip = findViewById(R.id.analytics_slip);
        ReceiptSlip.clear(slip);
        ReceiptSlip.addRow(slip, getString(R.string.analytics_served_label),
                String.valueOf(stats.getServedToday()));
        ReceiptSlip.addRow(slip, getString(R.string.analytics_waiting_label),
                String.valueOf(stats.getWaitingNow()));
        ReceiptSlip.addRow(slip, getString(R.string.analytics_no_shows_label),
                String.valueOf(stats.getNoShowsToday()));
        ReceiptSlip.addRow(slip, getString(R.string.analytics_no_show_rate_label),
                getString(R.string.analytics_percent_format, stats.getNoShowRatePercent()));
    }
}

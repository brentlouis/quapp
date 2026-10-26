package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/** Owner-only integration boundary; no recipient data or transfer operation is fabricated. */
public class OrganizerHandoverActivity extends AppCompatActivity {
    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_HANDOVER_QUEUE_ID";
    private String queueId;

    public static Intent intent(Context context, String queueId) {
        return new Intent(context, OrganizerHandoverActivity.class).putExtra(EXTRA_QUEUE_ID, queueId);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_organizer_handover);
        SystemBars.applyPadding(findViewById(R.id.handover_root));
        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        findViewById(R.id.handover_back).setOnClickListener(v -> finish());
        findViewById(R.id.handover_retry).setOnClickListener(v -> showUnavailable(true));
    }

    @Override protected void onResume() {
        super.onResume();
        showUnavailable(false);
    }

    private void showUnavailable(boolean retry) {
        Queue queue = FakeData.queueById(queueId);
        if (queue == null || !FakeData.ownsQueue(queueId)) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.handover_queue)).setText(Validation.isBlank(queue.getName())
                ? getString(R.string.handover_queue_unnamed) : queue.getName());
        // Retry rechecks local eligibility; the real recipient service is not integrated yet.
        ((TextView) findViewById(R.id.handover_message)).setText(retry
                ? R.string.handover_still_unavailable : R.string.handover_unavailable);
    }
}

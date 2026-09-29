package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.List;

/**
 * Counter display (canvas 32): a phone or tablet at the counter, landscape, showing who's being
 * served, the next three numbers and a QR to join. It checks the line every few seconds, so it
 * follows the console without anyone touching it. Back closes it.
 */
public class CounterDisplayActivity extends AppCompatActivity {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_COUNTER_QUEUE_ID";

    private static final long REFRESH_MS = 5_000L;
    private static final int UP_NEXT = 3;

    private String queueId;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            load();
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    public static Intent intent(Context context, String queueId) {
        Intent intent = new Intent(context, CounterDisplayActivity.class);
        intent.putExtra(EXTRA_QUEUE_ID, queueId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter_display);
        SystemBars.applyPadding(findViewById(R.id.counter_root));

        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        Queue queue = Queues.get(queueId);  // opened from Share, which had it
        if (queue == null) {
            finish();
            return;
        }

        // A public screen: no status or navigation bar. A swipe from the edge brings them back.
        WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());
        bars.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        bars.hide(WindowInsetsCompat.Type.systemBars());

        int size = getResources().getDimensionPixelSize(R.dimen.counter_qr_size);
        ((ImageView) findViewById(R.id.counter_qr)).setImageBitmap(QueueLink.qr(QueueLink.url(queue),
                size, ContextCompat.getColor(this, R.color.ink),
                ContextCompat.getColor(this, R.color.paper_raised)));
        ((TextView) findViewById(R.id.counter_scan)).setText(
                getString(R.string.counter_scan, queue.getName()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresh);
    }

    /** Asks the server for the line; the owner's phone shows it on the counter screen. */
    private void load() {
        ApiClient.api(this).line(queueId).enqueue(new ApiCallback<Line>(this) {
            @Override
            protected void onSuccess(@Nullable Line line) {
                if (line != null) {
                    render(line);
                }
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                // A public screen: keep the last numbers rather than show an error to the room
            }
        });
    }

    private void render(Line line) {
        Queue queue = Queues.get(queueId);
        // A slot that ran out of time isn't anyone's turn any more.
        boolean timedOut = line.isNowServingTimedOut();
        Ticket serving = line.getNowServing();
        TextView number = findViewById(R.id.counter_number);
        TextView instruction = findViewById(R.id.counter_instruction);
        if (serving == null || timedOut) {
            number.setText(R.string.counter_none);
            instruction.setText(R.string.counter_waiting);
        } else {
            number.setText(getString(R.string.ticket_number_format, serving.getTicketNumber()));
            instruction.setText(R.string.counter_come_up);
        }

        List<Ticket> waiting = line.getWaiting();
        StringBuilder next = new StringBuilder();
        for (int i = 0; i < Math.min(UP_NEXT, waiting.size()); i++) {
            if (i > 0) {
                next.append("   ");
            }
            next.append(getString(R.string.ticket_number_format, waiting.get(i).getTicketNumber()));
        }
        TextView nextView = findViewById(R.id.counter_next);
        nextView.setText(next.length() == 0 ? getString(R.string.counter_none) : next);

        ((TextView) findViewById(R.id.counter_queue)).setText(getResources().getQuantityString(
                R.plurals.counter_queue_waiting, waiting.size(), queue.getName(), waiting.size()));
    }
}

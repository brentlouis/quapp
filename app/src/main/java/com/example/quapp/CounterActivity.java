package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/** One counter, with explicit finish before the next ticket can be reserved. */
public class CounterActivity extends AppCompatActivity {
    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_COUNTER_OWNER_QUEUE_ID";
    public static final String EXTRA_COUNTER_ID = "com.example.quapp.EXTRA_MANAGED_COUNTER_ID";
    private String queueId, counterId;
    private QueueCounter counter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() { public void run() { render(); if (!isFinishing()) handler.postDelayed(this, 3000); } };
    public static Intent intent(Context context, String queueId, String counterId) { return new Intent(context, CounterActivity.class).putExtra(EXTRA_QUEUE_ID, queueId).putExtra(EXTRA_COUNTER_ID, counterId); }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_counter); SystemBars.applyPadding(findViewById(R.id.managed_counter_root));
        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID); counterId = getIntent().getStringExtra(EXTRA_COUNTER_ID);
        if (!resolve()) { finish(); return; }
        findViewById(R.id.managed_counter_back).setOnClickListener(v -> finish());
        findViewById(R.id.managed_counter_rename).setOnClickListener(v -> CounterListActivity.editName(this, queueId, counter, this::render));
        findViewById(R.id.managed_counter_call).setOnClickListener(v -> act(0));
        findViewById(R.id.managed_counter_done).setOnClickListener(v -> act(1));
        findViewById(R.id.managed_counter_no_show).setOnClickListener(v -> act(2));
        findViewById(R.id.managed_counter_display).setOnClickListener(v -> startActivity(CounterDisplayActivity.intent(this, queueId, counterId)));
    }
    private boolean resolve() {
        if (!FakeData.ownsQueue(queueId)) return false;
        for (QueueCounter candidate : FakeData.counters(queueId)) if (candidate.getId().equals(counterId)) { counter = candidate; return true; }
        return false;
    }
    @Override protected void onResume() { super.onResume(); if (counter != null) handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    private void act(int action) {
        try { if (action == 0) FakeData.callNextAtCounter(queueId, counterId); else FakeData.finishCounter(queueId, counterId, action == 2); }
        catch (SecurityException | IllegalArgumentException | IllegalStateException error) { QuappMotion.snackbar(findViewById(R.id.managed_counter_root), R.string.counters_unavailable, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).setTextMaxLines(Integer.MAX_VALUE).show(); }
        render();
    }
    private void render() {
        if (!resolve()) { finish(); return; }
        FakeData.checkGraceExpired(queueId);
        Ticket ticket = FakeData.counterServing(queueId, counterId); Queue queue = FakeData.queueById(queueId);
        ((TextView)findViewById(R.id.managed_counter_name)).setText(CounterListActivity.name(this, counter));
        ((TextView)findViewById(R.id.managed_counter_ticket)).setText(ticket == null ? getString(R.string.counter_none) : getString(R.string.ticket_number_format, ticket.getTicketNumber()));
        ((TextView)findViewById(R.id.managed_counter_holder)).setText(ticket == null ? "" : ticket.getHolderName());
        ((TextView)findViewById(R.id.managed_counter_state)).setText(getString(R.string.counters_waiting_format, FakeData.waitingTickets(queueId).size()));
        boolean running = queue.getStatus() == Queue.Status.OPEN || queue.getStatus() == Queue.Status.PAUSED;
        findViewById(R.id.managed_counter_call).setEnabled(running && ticket == null && !FakeData.waitingTickets(queueId).isEmpty());
        findViewById(R.id.managed_counter_done).setEnabled(ticket != null);
        findViewById(R.id.managed_counter_no_show).setEnabled(ticket != null);
    }
}

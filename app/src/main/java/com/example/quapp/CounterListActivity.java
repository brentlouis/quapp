package com.example.quapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;

/** Owner-only management of counters sharing this queue's waiting line. */
public class CounterListActivity extends AppCompatActivity {
    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_MANAGED_COUNTER_QUEUE_ID";
    private String queueId;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CounterAdapter adapter;
    private final Runnable refresh = new Runnable() { public void run() { render(); if (!isFinishing()) handler.postDelayed(this, 3000); } };
    public static Intent intent(Context context, String queueId) { return new Intent(context, CounterListActivity.class).putExtra(EXTRA_QUEUE_ID, queueId); }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_counter_list); SystemBars.applyPadding(findViewById(R.id.counters_root));
        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        if (!FakeData.ownsQueue(queueId)) { finish(); return; }
        RecyclerView list = findViewById(R.id.counters_list); list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CounterAdapter(); list.setAdapter(adapter);
        findViewById(R.id.counters_back).setOnClickListener(v -> finish());
        findViewById(R.id.counters_add).setOnClickListener(v -> editName(this, queueId, null, this::render));
    }
    @Override protected void onResume() { super.onResume(); if (adapter != null) handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    private void render() {
        if (!FakeData.ownsQueue(queueId)) { finish(); return; }
        FakeData.checkGraceExpired(queueId);
        adapter.items = FakeData.counters(queueId); adapter.notifyDataSetChanged();
    }
    static String name(Context context, QueueCounter counter) { return counter.getName().isEmpty() ? context.getString(R.string.counters_default_name) : counter.getName(); }
    static void editName(Activity activity, String queueId, QueueCounter counter, Runnable changed) {
        View body = activity.getLayoutInflater().inflate(R.layout.dialog_counter_name, null);
        TextInputLayout layout = body.findViewById(R.id.counter_name_layout);
        TextInputEditText input = body.findViewById(R.id.counter_name_input);
        if (counter != null) input.setText(name(activity, counter));
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Quapp_Dialog_WalkIn)
                .setTitle(counter == null ? R.string.counters_add : R.string.counters_rename).setView(body)
                .setNegativeButton(R.string.console_cancel, null).setPositiveButton(R.string.counters_save, null).show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = Forms.text(input);
            if (!Forms.check(layout, !Validation.isBlank(value), activity.getString(R.string.counters_name_required))) return;
            try {
                if (counter == null) FakeData.createCounter(queueId, value); else FakeData.renameCounter(queueId, counter.getId(), value);
                dialog.dismiss(); changed.run();
            } catch (SecurityException | IllegalArgumentException error) {
                QuappMotion.snackbar(activity.findViewById(android.R.id.content), R.string.counters_unavailable, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).setTextMaxLines(Integer.MAX_VALUE).show();
            }
        });
    }
    private final class CounterAdapter extends RecyclerView.Adapter<CounterHolder> {
        List<QueueCounter> items = new ArrayList<>();
        @Override public CounterHolder onCreateViewHolder(ViewGroup parent, int type) { return new CounterHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_managed_counter, parent, false)); }
        @Override public void onBindViewHolder(CounterHolder holder, int position) {
            QueueCounter counter = items.get(position); holder.title.setText(name(CounterListActivity.this, counter));
            Ticket ticket = FakeData.counterServing(queueId, counter.getId());
            holder.state.setText(ticket == null ? getString(R.string.counters_available) : getString(R.string.counters_serving_format, ticket.getTicketNumber()));
            holder.itemView.setOnClickListener(v -> startActivity(CounterActivity.intent(CounterListActivity.this, queueId, counter.getId())));
        }
        @Override public int getItemCount() { return items.size(); }
    }
    private static final class CounterHolder extends RecyclerView.ViewHolder {
        final TextView title, state;
        CounterHolder(View view) { super(view); title = view.findViewById(R.id.managed_counter_row_name); state = view.findViewById(R.id.managed_counter_row_state); }
    }
}

package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SavedServicesActivity extends AppCompatActivity {
    private OpenNowAdapter adapter;
    private LoadingStateHelper state;
    private SavedServices saved;

    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_saved_services);
        SystemBars.applyPadding(findViewById(R.id.saved_root));
        saved = new SavedServices(this);
        RecyclerView list = findViewById(R.id.saved_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OpenNowAdapter(queue -> startActivity(new Intent(this, QueueDetailActivity.class)
                .putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, queue.getId())), this::load);
        list.setAdapter(adapter);
        state = new LoadingStateHelper(list, findViewById(R.id.saved_empty),
                findViewById(R.id.saved_loading), findViewById(R.id.saved_error));
        state.setOnRetryListener(this::load);
        findViewById(R.id.saved_back).setOnClickListener(v -> finish());
        findViewById(R.id.saved_browse).setOnClickListener(v -> {
            startActivity(QueuerHomeActivity.intent(this, QueuerHomeActivity.TAB_BROWSE)); finish();
        });
        findViewById(R.id.saved_clear).setOnClickListener(v -> new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Quapp_Dialog_WalkIn)
                .setTitle(R.string.saved_clear_title).setMessage(R.string.saved_clear_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.saved_clear_confirm, (dialog, which) -> { saved.clear(); load(); }).show());
    }

    @Override protected void onResume() { super.onResume(); load(); }

    private void load() {
        boolean any = !saved.ids().isEmpty();
        ((android.widget.TextView) findViewById(R.id.saved_empty_title)).setText(
                any ? R.string.saved_unavailable_title : R.string.saved_empty_title);
        findViewById(R.id.saved_clear).setVisibility(any ? View.VISIBLE : View.GONE);
        if (!any) { state.showEmpty(); return; }
        state.showLoading(getString(R.string.loading_queues));
        FakeData.getQueuesAsync(new FakeData.Callback<List<Queue>>() {
            @Override public void onSuccess(List<Queue> queues) {
                if (isFinishing() || isDestroyed()) return;
                Set<String> ids = saved.ids();
                List<Queue> result = new ArrayList<>();
                for (Queue queue : queues) if (ids.contains(queue.getId())) result.add(queue);
                adapter.submitQueues(result);
                if (result.isEmpty()) state.showEmpty(); else state.showContent();
            }
            @Override public void onError(Throwable error) {
                if (!isFinishing() && !isDestroyed()) state.showError(getString(R.string.error_default_message));
            }
        });
    }
}

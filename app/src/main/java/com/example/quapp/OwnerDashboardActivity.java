package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

public class OwnerDashboardActivity extends AppCompatActivity
        implements OwnedQueueAdapter.OnOwnedQueueClickListener {

    private OwnedQueueAdapter queueAdapter;
    private RecyclerView queueList;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);
        SystemBars.applyPadding(findViewById(R.id.dashboard_root));

        queueList = findViewById(R.id.dashboard_list);
        emptyState = findViewById(R.id.dashboard_empty);

        queueList.setLayoutManager(new LinearLayoutManager(this));
        queueList.setHasFixedSize(true);

        queueAdapter = new OwnedQueueAdapter(this);
        queueList.setAdapter(queueAdapter);

        ExtendedFloatingActionButton createFab = findViewById(R.id.dashboard_create_fab);
        createFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(OwnerDashboardActivity.this, CreateQueueActivity.class));
            }
        });

        MaterialButton emptyButton = findViewById(R.id.dashboard_empty_button);
        emptyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(OwnerDashboardActivity.this, CreateQueueActivity.class));
            }
        });

        loadQueues();
    }

    private void loadQueues() {
        List<Queue> ownedQueues = FakeData.ownedQueues();

        if (ownedQueues.isEmpty()) {
            queueList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            queueList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            queueAdapter.submitQueues(ownedQueues);
        }
    }

    @Override
    public void onOwnedQueueClick(Queue queue) {
        Intent intent = new Intent(this, LiveConsoleActivity.class);
        intent.putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID, queue.getId());
        startActivity(intent);
    }
}
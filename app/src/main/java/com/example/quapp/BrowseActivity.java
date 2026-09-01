package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class BrowseActivity extends AppCompatActivity
        implements QueueAdapter.OnQueueClickListener {

    private QueueAdapter queueAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);
        SystemBars.applyPadding(findViewById(R.id.browse_root));

        RecyclerView queueList = findViewById(R.id.browse_list);
        queueList.setLayoutManager(new LinearLayoutManager(this));
        queueList.setHasFixedSize(true);

        queueAdapter = new QueueAdapter(this);
        queueList.setAdapter(queueAdapter);

        queueAdapter.submitQueues(FakeData.queues());
    }

    @Override
    public void onQueueClick(Queue queue) {
        Intent intent = new Intent(this, QueueDetailActivity.class);
        intent.putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, queue.getId());
        startActivity(intent);
    }
}

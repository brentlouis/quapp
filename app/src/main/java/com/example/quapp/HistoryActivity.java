package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        SystemBars.applyPadding(findViewById(R.id.history_root));

        findViewById(R.id.history_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.history_empty_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Browse is already further down the stack. CLEAR_TOP returns to that
                // instance (closing Profile and History) instead of stacking a new one.
                Intent intent = new Intent(HistoryActivity.this, BrowseActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        RecyclerView list = findViewById(R.id.history_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new MaterialDividerItemDecoration(
                this, MaterialDividerItemDecoration.VERTICAL));

        HistoryAdapter adapter = new HistoryAdapter();
        list.setAdapter(adapter);

        List<Ticket> tickets = FakeData.history();
        adapter.submitTickets(tickets);

        boolean empty = tickets.isEmpty();
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.history_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}

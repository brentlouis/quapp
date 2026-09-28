package com.example.quapp;

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
                // The queuer home is already further down the stack. intent() uses CLEAR_TOP to
                // return to that instance (closing History) and switch it to the Browse tab.
                startActivity(QueuerHomeActivity.intent(HistoryActivity.this,
                        QueuerHomeActivity.TAB_BROWSE));
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

package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
        HistoryAdapter adapter = new HistoryAdapter();
        list.setAdapter(adapter);

        List<Ticket> tickets = FakeData.history();
        adapter.submitTickets(tickets);

        boolean empty = tickets.isEmpty();
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.history_slip).setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.history_section).setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.history_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        bindTotals(tickets);
    }

    /** Queues joined, served, no-show on the receipt slip. */
    private void bindTotals(List<Ticket> tickets) {
        int served = 0;
        int noShows = 0;
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == Ticket.Status.SERVED) {
                served++;
            } else if (ticket.getStatus() == Ticket.Status.NO_SHOW) {
                noShows++;
            }
        }
        LinearLayout slip = findViewById(R.id.history_slip);
        ReceiptSlip.clear(slip);
        ReceiptSlip.addRow(slip, getString(R.string.history_joined_label), String.valueOf(tickets.size()));
        ReceiptSlip.addRow(slip, getString(R.string.history_served_label), String.valueOf(served));
        ReceiptSlip.addRow(slip, getString(R.string.history_no_show_label), String.valueOf(noShows));
    }
}

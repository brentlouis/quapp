package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/** Queue history (canvas 11): the account's finished tickets, from the server. */
public class HistoryActivity extends AppCompatActivity {

    private HistoryAdapter adapter;

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

        RecyclerView list = findViewById(R.id.history_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        list.setAdapter(adapter);

        load();
    }

    private void load() {
        showOnly(R.id.history_loading);
        ApiClient.api(this).myTickets(false).enqueue(new ApiCallback<List<Ticket>>(this) {
            @Override
            protected void onSuccess(@Nullable List<Ticket> tickets) {
                if (tickets == null || tickets.isEmpty()) {
                    showEmpty();
                    return;
                }
                adapter.submitTickets(tickets);
                bindTotals(tickets);
                showOnly(R.id.history_list, R.id.history_slip, R.id.history_section);
            }

            @Override
            protected void onError(@NonNull ApiError error) {
                showError(error);
            }
        });
    }

    /** Nothing finished yet: point to Browse. */
    private void showEmpty() {
        ((TextView) findViewById(R.id.history_empty_title)).setText(R.string.history_empty_title);
        ((TextView) findViewById(R.id.history_empty_body)).setText(R.string.history_empty_body);
        MaterialButton button = findViewById(R.id.history_empty_button);
        button.setText(R.string.history_empty_action);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // The queuer home is already further down the stack. intent() uses CLEAR_TOP to
                // return to that instance (closing History) and switch it to the Browse tab.
                startActivity(QueuerHomeActivity.intent(HistoryActivity.this,
                        QueuerHomeActivity.TAB_BROWSE));
            }
        });
        showOnly(R.id.history_empty);
    }

    /** The same block as empty, saying what went wrong, with Try again. */
    private void showError(ApiError error) {
        ((TextView) findViewById(R.id.history_empty_title)).setText(R.string.history_error_title);
        ((TextView) findViewById(R.id.history_empty_body)).setText(error.is(ApiError.OFFLINE)
                ? getString(R.string.api_offline) : error.message);
        MaterialButton button = findViewById(R.id.history_empty_button);
        button.setText(R.string.history_error_retry);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                load();
            }
        });
        showOnly(R.id.history_empty);
    }

    /** Shows these views and hides the rest of the states. */
    private void showOnly(int... shown) {
        int[] all = {R.id.history_loading, R.id.history_empty, R.id.history_list,
                R.id.history_slip, R.id.history_section};
        for (int id : all) {
            boolean visible = false;
            for (int s : shown) {
                visible |= s == id;
            }
            findViewById(id).setVisibility(visible ? View.VISIBLE : View.GONE);
        }
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

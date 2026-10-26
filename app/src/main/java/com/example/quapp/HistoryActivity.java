package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView list;
    private HistoryAdapter adapter;
    private View emptyView;
    private View slipView;
    private View sectionView;
    private LoadingStateHelper loadingStateHelper;
    private List<Ticket> cachedTickets;

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
                startActivity(QueuerHomeActivity.intent(HistoryActivity.this,
                        QueuerHomeActivity.TAB_BROWSE));
            }
        });

        list = findViewById(R.id.history_list);
        emptyView = findViewById(R.id.history_empty);
        slipView = findViewById(R.id.history_slip);
        sectionView = findViewById(R.id.history_section);

        list.setLayoutManager(new LinearLayoutManager(this));
        ((android.widget.TextView) findViewById(R.id.history_title)).setText(R.string.profile_past_visits);
        adapter = new HistoryAdapter(ticket -> startActivity(ProfileSettingsActivity.intent(this, "receipt").putExtra("receipt_ticket_id", ticket.getId())));
        list.setAdapter(adapter);

        loadingStateHelper = new LoadingStateHelper(
                null,
                emptyView,
                findViewById(R.id.history_loading),
                findViewById(R.id.history_error)
        );
        loadingStateHelper.setOnRetryListener(new LoadingStateHelper.OnRetryListener() {
            @Override
            public void onRetry() {
                loadHistory();
            }
        });

        loadHistory();
    }

    private void loadHistory() {
        if (cachedTickets == null) {
            loadingStateHelper.showLoading(getString(R.string.loading_tickets));
            list.setVisibility(View.GONE);
            slipView.setVisibility(View.GONE);
            sectionView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }

        FakeData.getHistoryAsync(new FakeData.Callback<List<Ticket>>() {
            @Override
            public void onSuccess(List<Ticket> tickets) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                cachedTickets = tickets;
                loadingStateHelper.showContent();
                boolean empty = tickets.isEmpty();
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
                slipView.setVisibility(empty ? View.GONE : View.VISIBLE);
                sectionView.setVisibility(empty ? View.GONE : View.VISIBLE);
                emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                adapter.submitTickets(tickets);
                bindTotals(tickets);
            }

            @Override
            public void onError(Throwable error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (cachedTickets == null) {
                    loadingStateHelper.showError(getString(R.string.error_default_message));
                    list.setVisibility(View.GONE);
                    slipView.setVisibility(View.GONE);
                    sectionView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.GONE);
                } else {
                    QuappMotion.snackbar(
                            findViewById(R.id.history_root), R.string.error_default_message,
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
            }
        });
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

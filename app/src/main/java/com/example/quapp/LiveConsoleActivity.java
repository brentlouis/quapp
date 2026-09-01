package com.example.quapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class LiveConsoleActivity extends AppCompatActivity
        implements WaitingTicketAdapter.OnTicketActionListener {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_CONSOLE_QUEUE_ID";

    private Queue queue;
    private List<Ticket> waitingTickets;
    private Ticket nowServing;

    private WaitingTicketAdapter ticketAdapter;
    private RecyclerView ticketList;
    private View emptyState;
    private TextView servingValue;
    private TextView waitingHeader;
    private MaterialButton callNextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_console);
        SystemBars.applyPadding(findViewById(R.id.console_root));

        String queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        queue = FakeData.queueById(queueId);

        if (queue == null) {
            finish();
            return;
        }

        cacheViews();

        TextView queueName = findViewById(R.id.console_queue_name);
        queueName.setText(queue.getName());

        ticketList.setLayoutManager(new LinearLayoutManager(this));
        ticketAdapter = new WaitingTicketAdapter(this);
        ticketList.setAdapter(ticketAdapter);

        waitingTickets = FakeData.ticketsForQueue(queue);

        findViewById(R.id.console_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        callNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callNext();
            }
        });

        render();
    }

    private void cacheViews() {
        ticketList = findViewById(R.id.console_list);
        emptyState = findViewById(R.id.console_empty);
        servingValue = findViewById(R.id.console_serving_value);
        waitingHeader = findViewById(R.id.console_waiting_header);
        callNextButton = findViewById(R.id.console_call_next);
    }

    private void callNext() {
        if (waitingTickets.isEmpty()) {
            return;
        }

        nowServing = waitingTickets.remove(0);
        render();
    }

    @Override
    public void onServeTicket(Ticket ticket) {
        waitingTickets.remove(ticket);
        render();
    }

    @Override
    public void onNoShowTicket(Ticket ticket) {
        waitingTickets.remove(ticket);
        render();
    }

    private void render() {
        if (nowServing == null) {
            servingValue.setText(R.string.console_none_serving);
        } else {
            servingValue.setText(getString(R.string.console_called_format,
                    nowServing.getTicketNumber(), nowServing.getHolderName()));
        }

        waitingHeader.setText(getString(R.string.console_waiting_count_format,
                waitingTickets.size()));

        callNextButton.setEnabled(!waitingTickets.isEmpty());

        if (waitingTickets.isEmpty()) {
            ticketList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            ticketList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        ticketAdapter.submitTickets(waitingTickets);
    }
}
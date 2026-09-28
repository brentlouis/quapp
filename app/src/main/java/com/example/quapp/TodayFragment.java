package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Today tab: totals across every queue the owner runs, then each queue's numbers.
 * The totals used to sit on the dashboard; they moved here so the Queues tab can lead with
 * the live queue (DECISIONS.md "Bottom navigation, landing states and first visit").
 */
public class TodayFragment extends Fragment implements TodayQueueAdapter.OnTodayQueueClickListener {

    private TodayQueueAdapter adapter;
    private RecyclerView list;
    private TextView servedText;
    private TextView waitingText;
    private TextView noShowRateText;
    private View[] populatedViews;
    private View emptyState;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        servedText = view.findViewById(R.id.today_served);
        waitingText = view.findViewById(R.id.today_waiting);
        noShowRateText = view.findViewById(R.id.today_no_show_rate);
        emptyState = view.findViewById(R.id.today_empty);

        list = view.findViewById(R.id.today_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(
                requireContext(), MaterialDividerItemDecoration.VERTICAL);
        divider.setLastItemDecorated(false);
        list.addItemDecoration(divider);

        adapter = new TodayQueueAdapter(this);
        list.setAdapter(adapter);

        // Everything that hides together when there are no queues.
        populatedViews = new View[]{
                view.findViewById(R.id.today_totals_top),
                view.findViewById(R.id.today_totals),
                view.findViewById(R.id.today_totals_bottom),
                view.findViewById(R.id.today_section),
                list
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refresh();
        }
    }

    private void refresh() {
        List<Queue> queues = FakeData.ownedQueues();

        boolean empty = queues.isEmpty();
        for (View v : populatedViews) {
            v.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }

        int served = 0;
        int noShows = 0;
        int waiting = 0;
        Map<String, QueueStats> statsById = new HashMap<>();
        for (Queue queue : queues) {
            QueueStats stats = FakeData.stats(queue.getId());
            statsById.put(queue.getId(), stats);
            served += stats.getServedToday();
            noShows += stats.getNoShowsToday();
            waiting += stats.getWaitingNow();
        }

        // Same rule as QueueStats.getNoShowRatePercent, over all queues together.
        int finished = served + noShows;
        int noShowRate = finished == 0 ? 0 : Math.round(noShows * 100f / finished);

        servedText.setText(String.valueOf(served));
        waitingText.setText(String.valueOf(waiting));
        noShowRateText.setText(getString(R.string.today_percent_format, noShowRate));

        adapter.submitQueues(queues, statsById);
    }

    @Override
    public void onTodayQueueClick(Queue queue) {
        Intent intent = new Intent(requireContext(), QueueAnalyticsActivity.class);
        intent.putExtra(QueueAnalyticsActivity.EXTRA_QUEUE_ID, queue.getId());
        startActivity(intent);
    }
}

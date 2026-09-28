package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Queues tab. Same behaviour as the old OwnerDashboardActivity; only the lifecycle changed,
 * the same way Browse did. The Civic Paper redesign (live-queue card, filters) comes later.
 */
public class OwnerQueuesFragment extends Fragment
        implements OwnedQueueAdapter.OnOwnedQueueClickListener {

    private OwnedQueueAdapter queueAdapter;
    private RecyclerView queueList;
    private View emptyState;
    private ExtendedFloatingActionButton createFab;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_owner_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Profile is a tab now, so the header button goes.
        view.findViewById(R.id.dashboard_profile).setVisibility(View.GONE);

        queueList = view.findViewById(R.id.dashboard_list);
        emptyState = view.findViewById(R.id.dashboard_empty);

        queueList.setLayoutManager(new LinearLayoutManager(requireContext()));
        queueList.setHasFixedSize(true);

        queueAdapter = new OwnedQueueAdapter(this);
        queueList.setAdapter(queueAdapter);

        View.OnClickListener openCreate = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireContext(), CreateQueueActivity.class));
            }
        };
        createFab = view.findViewById(R.id.dashboard_create_fab);
        createFab.setOnClickListener(openCreate);
        view.findViewById(R.id.dashboard_empty_button).setOnClickListener(openCreate);
    }

    /** Coming back from another Activity (Create Queue, Live Console). */
    @Override
    public void onResume() {
        super.onResume();
        loadQueues();
    }

    /** Coming back from another tab. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadQueues();
        }
    }

    private void loadQueues() {
        List<Queue> ownedQueues = FakeData.ownedQueues();

        // The empty state has its own create button; showing the FAB too would double it.
        if (ownedQueues.isEmpty()) {
            queueList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            createFab.hide();
        } else {
            queueList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            createFab.show();
            Map<String, QueueStats> stats = new HashMap<>();
            for (Queue queue : ownedQueues) {
                stats.put(queue.getId(), FakeData.stats(queue.getId()));
            }
            queueAdapter.submitQueues(ownedQueues, stats);
        }
    }

    @Override
    public void onOwnedQueueClick(Queue queue) {
        Intent intent = new Intent(requireContext(), LiveConsoleActivity.class);
        intent.putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID, queue.getId());
        startActivity(intent);
    }
}

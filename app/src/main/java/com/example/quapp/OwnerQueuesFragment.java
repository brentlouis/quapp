package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The owner's Queues tab (canvas 13, 35): the live queue on top as the spotlight, a status
 * filter with counts, then a card per queue with today's numbers. With no queues yet, a
 * three-step guide to running the first one.
 */
public class OwnerQueuesFragment extends Fragment implements QueueAdapter.OnQueueClickListener {

    private static final String STATE_STATUS = "status";

    private QueueAdapter adapter;
    private RecyclerView list;
    private View live;
    private View filters;
    private View firstVisit;
    private TextView filterEmpty;
    private ChipGroup statusGroup;
    private ExtendedFloatingActionButton createFab;
    private final Map<Queue.Status, Chip> statusChips = new HashMap<>();
    private Chip allChip;

    // null means every status
    private Queue.Status selectedStatus;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_queues, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getString(STATE_STATUS) != null) {
            selectedStatus = Queue.Status.valueOf(savedInstanceState.getString(STATE_STATUS));
        }

        list = view.findViewById(R.id.owner_list);
        live = view.findViewById(R.id.owner_live);
        filters = view.findViewById(R.id.owner_filters);
        firstVisit = view.findViewById(R.id.owner_first_visit);
        filterEmpty = view.findViewById(R.id.owner_filter_empty);
        statusGroup = view.findViewById(R.id.owner_status_group);
        createFab = view.findViewById(R.id.owner_create_fab);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new QueueAdapter(this);
        list.setAdapter(adapter);

        live.setBackground(TicketShapes.spotlightBackground(requireContext()));

        View.OnClickListener openCreate = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireContext(), CreateQueueActivity.class));
            }
        };
        createFab.setOnClickListener(openCreate);
        view.findViewById(R.id.owner_first_create).setOnClickListener(openCreate);

        setUpStatusChips();
        setUpSteps(view);
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_STATUS, selectedStatus == null ? null : selectedStatus.name());
    }

    // ---- Setup ----------------------------------------------------------------

    /** All, Open, Upcoming, Paused, Closed. The counts are filled in on every refresh. */
    private void setUpStatusChips() {
        allChip = addStatusChip(null);
        Chip toCheck = allChip;
        for (Queue.Status status : new Queue.Status[]{Queue.Status.OPEN, Queue.Status.UPCOMING,
                Queue.Status.PAUSED, Queue.Status.CLOSED}) {
            Chip chip = addStatusChip(status);
            statusChips.put(status, chip);
            if (status == selectedStatus) {
                toCheck = chip;
            }
        }
        toCheck.setChecked(true);
        statusGroup.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                if (checkedIds.isEmpty()) {
                    return;
                }
                selectedStatus = (Queue.Status) group.findViewById(checkedIds.get(0)).getTag();
                refresh();
            }
        });
    }

    /** A status chip; the selected one becomes a punched ticket. */
    private Chip addStatusChip(@Nullable Queue.Status status) {
        final Chip chip = (Chip) LayoutInflater.from(requireContext())
                .inflate(R.layout.view_filter_chip, statusGroup, false);
        chip.setId(View.generateViewId());
        chip.setTag(status);
        final ShapeAppearanceModel pill = chip.getShapeAppearanceModel();
        final ShapeAppearanceModel punched =
                TicketShapes.selection(requireContext(), R.dimen.radius_selection);
        chip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                chip.setShapeAppearanceModel(isChecked ? punched : pill);
            }
        });
        statusGroup.addView(chip);
        return chip;
    }

    /** The three steps on the first-visit card. */
    private void setUpSteps(View view) {
        LinearLayout steps = view.findViewById(R.id.owner_first_steps);
        int[][] content = {
                {R.string.step_1, R.string.owner_step_create, R.string.owner_step_create_body},
                {R.string.step_2, R.string.owner_step_share, R.string.owner_step_share_body},
                {R.string.step_3, R.string.owner_step_call, R.string.owner_step_call_body}};
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int[] step : content) {
            View row = inflater.inflate(R.layout.view_step, steps, false);
            ((TextView) row.findViewById(R.id.step_number)).setText(step[0]);
            ((TextView) row.findViewById(R.id.step_title)).setText(step[1]);
            ((TextView) row.findViewById(R.id.step_body)).setText(step[2]);
            steps.addView(row);
        }
    }

    // ---- Rendering ----------------------------------------------------------

    private void refresh() {
        View view = getView();
        if (view == null) {
            return;
        }
        List<Queue> owned = FakeData.ownedQueues();
        boolean first = owned.isEmpty();
        bindGreeting(view, first);

        firstVisit.setVisibility(first ? View.VISIBLE : View.GONE);
        filters.setVisibility(first ? View.GONE : View.VISIBLE);
        list.setVisibility(first ? View.GONE : View.VISIBLE);
        // The first-visit card has its own Create button; one filled button per screen.
        if (first) {
            createFab.hide();
            live.setVisibility(View.GONE);
            filterEmpty.setVisibility(View.GONE);
            return;
        }
        createFab.show();

        bindLive(owned);
        bindCounts(owned);

        List<Queue> shown = new ArrayList<>();
        Map<String, QueueStats> stats = new HashMap<>();
        for (Queue queue : owned) {
            if (selectedStatus == null || queue.getStatus() == selectedStatus) {
                shown.add(queue);
                stats.put(queue.getId(), FakeData.stats(queue.getId()));
            }
        }
        adapter.submitQueues(shown, stats);
        boolean none = shown.isEmpty() && selectedStatus != null;
        filterEmpty.setVisibility(none ? View.VISIBLE : View.GONE);
        if (none) {
            filterEmpty.setText(getString(R.string.owner_filter_empty_format,
                    getString(statusLabel(selectedStatus)).toLowerCase(Locale.getDefault())));
        }
    }

    private void bindGreeting(View view, boolean first) {
        int hour = LocalTime.now(Format.MANILA).getHour();
        int greeting = first ? R.string.browse_welcome
                : hour < 12 ? R.string.browse_good_morning
                : hour < 18 ? R.string.browse_good_afternoon
                : R.string.browse_good_evening;
        String name = new Session(requireContext()).getName();
        String firstName = name == null ? null : name.trim().split("\\s+")[0];
        ((TextView) view.findViewById(R.id.owner_greeting)).setText(firstName == null
                ? getString(greeting)
                : getString(R.string.browse_greeting_name_format, getString(greeting), firstName));
    }

    /** The first open queue is "live now": the number being served, and a way into the console. */
    private void bindLive(List<Queue> owned) {
        Queue current = null;
        for (Queue queue : owned) {
            if (queue.getStatus() == Queue.Status.OPEN) {
                current = queue;
                break;
            }
        }
        live.setVisibility(current == null ? View.GONE : View.VISIBLE);
        if (current == null) {
            return;
        }
        final Queue queue = current;
        Ticket serving = FakeData.nowServing(queue.getId());
        ((TextView) live.findViewById(R.id.owner_live_number)).setText(serving == null
                ? getString(R.string.browse_eta_none)
                : getString(R.string.ticket_number_format, serving.getTicketNumber()));
        ((TextView) live.findViewById(R.id.owner_live_name)).setText(queue.getName());
        ((TextView) live.findViewById(R.id.owner_live_detail)).setText(serving == null
                ? getString(R.string.owner_live_waiting_format, queue.getPeopleWaiting())
                : getString(R.string.owner_live_serving_format, queue.getPeopleWaiting(),
                        serving.getHolderName()));

        View.OnClickListener openConsole = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onQueueClick(queue);
            }
        };
        live.setOnClickListener(openConsole);
        live.findViewById(R.id.owner_live_console).setOnClickListener(openConsole);
    }

    /** "All 4", "Open 1", … */
    private void bindCounts(List<Queue> owned) {
        allChip.setText(getString(R.string.owner_chip_count_format,
                getString(R.string.browse_filter_all), owned.size()));
        for (Map.Entry<Queue.Status, Chip> entry : statusChips.entrySet()) {
            int count = 0;
            for (Queue queue : owned) {
                if (queue.getStatus() == entry.getKey()) {
                    count++;
                }
            }
            entry.getValue().setText(getString(R.string.owner_chip_count_format,
                    getString(statusLabel(entry.getKey())), count));
        }
    }

    @StringRes
    private static int statusLabel(Queue.Status status) {
        switch (status) {
            case UPCOMING:
                return R.string.detail_status_upcoming;
            case PAUSED:
                return R.string.detail_status_paused;
            case CLOSED:
                return R.string.detail_status_closed;
            case OPEN:
            default:
                return R.string.detail_status_open;
        }
    }

    @Override
    public void onQueueClick(Queue queue) {
        Intent intent = new Intent(requireContext(), LiveConsoleActivity.class);
        intent.putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID, queue.getId());
        startActivity(intent);
    }
}

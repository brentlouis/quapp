package com.example.quapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnedQueueAdapter
        extends RecyclerView.Adapter<OwnedQueueAdapter.OwnedQueueViewHolder> {

    public interface OnOwnedQueueClickListener {
        void onOwnedQueueClick(Queue queue);
    }

    private final List<Queue> queues = new ArrayList<>();
    private final Map<String, QueueStats> statsById = new HashMap<>();
    private final OnOwnedQueueClickListener clickListener;

    public OwnedQueueAdapter(OnOwnedQueueClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /** Stats are keyed by queue id; the adapter only displays them, never fetches them. */
    public void submitQueues(List<Queue> newQueues, Map<String, QueueStats> newStats) {
        queues.clear();
        queues.addAll(newQueues);
        statsById.clear();
        statsById.putAll(newStats);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OwnedQueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_owned_queue, parent, false);
        return new OwnedQueueViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull OwnedQueueViewHolder holder, int position) {
        Queue queue = queues.get(position);
        holder.bind(queue, statsById.get(queue.getId()), clickListener);
    }

    @Override
    public int getItemCount() {
        return queues.size();
    }

    static class OwnedQueueViewHolder extends RecyclerView.ViewHolder {

        private final View statusDot;
        private final TextView nameText;
        private final TextView venueText;
        private final TextView waitingText;
        private final TextView servedText;

        OwnedQueueViewHolder(@NonNull View itemView) {
            super(itemView);
            statusDot = itemView.findViewById(R.id.owned_status_dot);
            nameText = itemView.findViewById(R.id.owned_name);
            venueText = itemView.findViewById(R.id.owned_venue);
            waitingText = itemView.findViewById(R.id.owned_waiting);
            servedText = itemView.findViewById(R.id.owned_served);
        }

        void bind(final Queue queue, QueueStats stats,
                  final OnOwnedQueueClickListener clickListener) {
            nameText.setText(queue.getName());
            waitingText.setText(String.valueOf(queue.getPeopleWaiting()));
            servedText.setText(String.valueOf(stats == null ? 0 : stats.getServedToday()));

            int dotColor;
            switch (queue.getStatus()) {
                case PAUSED:
                    dotColor = R.color.status_called;
                    venueText.setText(itemView.getContext().getString(R.string.browse_venue_format,
                            queue.getVenue(),
                            itemView.getContext().getString(R.string.dashboard_status_paused)));
                    break;
                case CLOSED:
                    dotColor = R.color.status_expired;
                    venueText.setText(itemView.getContext().getString(R.string.browse_venue_format,
                            queue.getVenue(),
                            itemView.getContext().getString(R.string.dashboard_status_closed)));
                    break;
                case OPEN:
                default:
                    dotColor = R.color.status_served;
                    venueText.setText(queue.getVenue());
                    break;
            }

            // mutate() gives this row its own copy; without it, rows can share one tinted drawable.
            statusDot.getBackground().mutate().setTint(
                    ContextCompat.getColor(itemView.getContext(), dotColor));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onOwnedQueueClick(queue);
                }
            });
        }
    }
}
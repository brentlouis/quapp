package com.example.quapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class OwnedQueueAdapter
        extends RecyclerView.Adapter<OwnedQueueAdapter.OwnedQueueViewHolder> {

    public interface OnOwnedQueueClickListener {
        void onOwnedQueueClick(Queue queue);
    }

    private final List<Queue> queues = new ArrayList<>();
    private final OnOwnedQueueClickListener clickListener;

    public OwnedQueueAdapter(OnOwnedQueueClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitQueues(List<Queue> newQueues) {
        queues.clear();
        queues.addAll(newQueues);
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
        holder.bind(queues.get(position), clickListener);
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

        void bind(final Queue queue, final OnOwnedQueueClickListener clickListener) {
            nameText.setText(queue.getName());
            venueText.setText(queue.getVenue());
            waitingText.setText(String.valueOf(queue.getPeopleWaiting()));

            // Placeholder until served counts come from the backend.
            servedText.setText(String.valueOf(queue.isOpen() ? 18 : 0));

            int dotColor = queue.isOpen() ? R.color.status_served : R.color.status_expired;
            statusDot.getBackground().setTint(
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
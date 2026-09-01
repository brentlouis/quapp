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

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    public interface OnQueueClickListener {
        void onQueueClick(Queue queue);
    }

    private final List<Queue> queues = new ArrayList<>();
    private final OnQueueClickListener clickListener;

    public QueueAdapter(OnQueueClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitQueues(List<Queue> newQueues) {
        queues.clear();
        queues.addAll(newQueues);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue, parent, false);
        return new QueueViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        holder.bind(queues.get(position), clickListener);
    }

    @Override
    public int getItemCount() {
        return queues.size();
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameText;
        private final TextView statusText;
        private final TextView venueText;
        private final TextView waitingText;
        private final TextView etaText;
        private final TextView categoryText;

        QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.queue_name);
            statusText = itemView.findViewById(R.id.queue_status);
            venueText = itemView.findViewById(R.id.queue_venue);
            waitingText = itemView.findViewById(R.id.queue_waiting);
            etaText = itemView.findViewById(R.id.queue_eta);
            categoryText = itemView.findViewById(R.id.queue_category);
        }

        void bind(final Queue queue, final OnQueueClickListener clickListener) {
            nameText.setText(queue.getName());
            venueText.setText(itemView.getContext().getString(
                    R.string.browse_venue_format, queue.getVenue(), queue.getMunicipality()));
            categoryText.setText(queue.getCategory());

            waitingText.setText(itemView.getContext()
                    .getString(R.string.browse_waiting_format, queue.getPeopleWaiting()));

            if (queue.isOpen()) {
                statusText.setText(R.string.browse_status_open);
                statusText.setTextColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.status_waiting));
                etaText.setText(itemView.getContext()
                        .getString(R.string.browse_eta_format, queue.getEstimatedWaitMinutes()));
            } else {
                statusText.setText(R.string.browse_status_closed);
                statusText.setTextColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.status_expired));
                etaText.setText(R.string.browse_eta_none);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onQueueClick(queue);
                }
            });
        }
    }
}
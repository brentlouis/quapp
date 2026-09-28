package com.example.quapp;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

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
        private final TextView etaUnitText;

        QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.queue_name);
            statusText = itemView.findViewById(R.id.queue_status);
            venueText = itemView.findViewById(R.id.queue_venue);
            waitingText = itemView.findViewById(R.id.queue_waiting);
            etaText = itemView.findViewById(R.id.queue_eta);
            etaUnitText = itemView.findViewById(R.id.queue_eta_unit);
        }

        void bind(final Queue queue, final OnQueueClickListener clickListener) {
            nameText.setText(queue.getName());
            venueText.setText(itemView.getContext().getString(R.string.browse_card_venue_format,
                    itemView.getContext().getString(queue.getCategory().label),
                    queue.getVenue(), queue.getMunicipality()));

            waitingText.setText(itemView.getContext()
                    .getString(R.string.browse_waiting_format, queue.getPeopleWaiting()));

            // Queue states get theme colors, not the ticket status colors: "open" is not
            // "your ticket is waiting". Only open is highlighted; paused and closed stay quiet.
            switch (queue.getStatus()) {
                case OPEN:
                    setStatus(R.string.browse_status_open,
                            com.google.android.material.R.attr.colorPrimaryContainer,
                            com.google.android.material.R.attr.colorOnPrimaryContainer);
                    break;
                case UPCOMING:
                    setStatus(R.string.detail_status_upcoming,
                            com.google.android.material.R.attr.colorSurfaceVariant,
                            com.google.android.material.R.attr.colorOnSurfaceVariant);
                    break;
                case PAUSED:
                    setStatus(R.string.browse_status_paused,
                            com.google.android.material.R.attr.colorSurfaceVariant,
                            com.google.android.material.R.attr.colorOnSurfaceVariant);
                    break;
                case CLOSED:
                    setStatus(R.string.browse_status_closed,
                            com.google.android.material.R.attr.colorSurfaceVariant,
                            com.google.android.material.R.attr.colorOnSurfaceVariant);
                    break;
            }

            // A paused queue is still moving, so its wait is still worth showing.
            if (queue.getStatus() == Queue.Status.CLOSED) {
                etaText.setText(R.string.browse_eta_none);
                etaUnitText.setVisibility(View.GONE);
            } else {
                etaText.setText(String.valueOf(queue.getEstimatedWaitMinutes()));
                etaUnitText.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onQueueClick(queue);
                }
            });
        }

        private void setStatus(int textRes, int containerAttr, int contentAttr) {
            statusText.setText(textRes);
            statusText.setBackgroundTintList(ColorStateList.valueOf(
                    MaterialColors.getColor(statusText, containerAttr)));
            statusText.setTextColor(MaterialColors.getColor(statusText, contentAttr));
        }
    }
}
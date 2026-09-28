package com.example.quapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Read-only list of finished tickets, so unlike the other adapters it has no click listener. */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<Ticket> tickets = new ArrayList<>();

    public void submitTickets(List<Ticket> newTickets) {
        tickets.clear();
        tickets.addAll(newTickets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(tickets.get(position));
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView queueNameText;
        private final TextView statusText;
        private final TextView detailsText;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            queueNameText = itemView.findViewById(R.id.history_queue_name);
            statusText = itemView.findViewById(R.id.history_status);
            detailsText = itemView.findViewById(R.id.history_details);
        }

        void bind(Ticket ticket) {
            Context context = itemView.getContext();

            queueNameText.setText(ticket.getQueueName());
            detailsText.setText(context.getString(R.string.browse_venue_format,
                    context.getString(R.string.history_ticket_format, ticket.getTicketNumber()),
                    ticket.getVenue()));

            // Only finished tickets reach history, so these are the only two cases.
            if (ticket.getStatus() == Ticket.Status.NO_SHOW) {
                statusText.setText(R.string.history_status_no_show);
                statusText.setTextColor(ContextCompat.getColor(context, R.color.status_expired));
            } else {
                statusText.setText(R.string.history_status_served);
                statusText.setTextColor(ContextCompat.getColor(context, R.color.status_served));
            }
        }
    }
}

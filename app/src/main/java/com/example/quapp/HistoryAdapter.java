package com.example.quapp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
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
        // The dashed rule separates rows, so the first row has none above it.
        holder.bind(tickets.get(position), position > 0);
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView queueNameText;
        private final TextView statusText;
        private final TextView detailsText;
        private final TextView dateText;
        private final View rule;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            queueNameText = itemView.findViewById(R.id.history_queue_name);
            statusText = itemView.findViewById(R.id.history_status);
            detailsText = itemView.findViewById(R.id.history_details);
            dateText = itemView.findViewById(R.id.history_date);
            rule = itemView.findViewById(R.id.history_rule);
        }

        void bind(Ticket ticket, boolean showRule) {
            Context context = itemView.getContext();
            rule.setVisibility(showRule ? View.VISIBLE : View.GONE);

            queueNameText.setText(ticket.getQueueName());
            detailsText.setText(context.getString(R.string.history_details_format,
                    ticket.getTicketNumber(), ticket.getVenue()));
            Instant finished = ticket.getFinishedAt();
            dateText.setVisibility(finished == null ? View.GONE : View.VISIBLE);
            if (finished != null) {
                dateText.setText(Format.day(context, finished));
            }

            // A pill: status colour text on its soft ground (DESIGN.md "Status pills").
            // Queue closed is neutral: the owner closed it, so it's nobody's fault.
            int text;
            int color;
            int ground;
            switch (ticket.getStatus()) {
                case NO_SHOW:
                    text = R.string.history_status_no_show;
                    color = R.color.err;
                    ground = R.color.err_soft;
                    break;
                case REMOVED:
                    text = R.string.history_status_removed;
                    color = R.color.err;
                    ground = R.color.err_soft;
                    break;
                case QUEUE_CLOSED:
                    text = R.string.history_status_closed;
                    color = R.color.ink_muted;
                    ground = R.color.paper_sunk;
                    break;
                case SERVED:
                default:
                    text = R.string.history_status_served;
                    color = R.color.ok;
                    ground = R.color.ok_soft;
                    break;
            }
            statusText.setText(text);
            statusText.setTextColor(ContextCompat.getColor(context, color));
            ViewCompat.setBackgroundTintList(statusText, ColorStateList.valueOf(
                    ContextCompat.getColor(context, ground)));
        }
    }
}

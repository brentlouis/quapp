package com.example.quapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WaitingTicketAdapter
        extends RecyclerView.Adapter<WaitingTicketAdapter.WaitingViewHolder> {

    /**
     * What a waiting row can do. Serving and no-shows happen at the counter (the Now Serving
     * card), so only removing is left here (DECISIONS.md, Live console).
     */
    public interface OnTicketActionListener {
        /** Remove from line, with a reason (canvas 57). */
        void onRemoveTicket(Ticket ticket);
    }

    private final List<Ticket> tickets = new ArrayList<>();
    private final OnTicketActionListener actionListener;

    public WaitingTicketAdapter(OnTicketActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitTickets(List<Ticket> newTickets) {
        tickets.clear();
        tickets.addAll(newTickets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WaitingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waiting_ticket, parent, false);
        return new WaitingViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull WaitingViewHolder holder, int position) {
        holder.bind(tickets.get(position), actionListener);
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class WaitingViewHolder extends RecyclerView.ViewHolder {

        private final TextView numberText;
        private final TextView nameText;
        private final TextView phoneText;
        private final TextView tag;
        private final ImageButton actionsButton;

        WaitingViewHolder(@NonNull View itemView) {
            super(itemView);
            numberText = itemView.findViewById(R.id.waiting_number);
            nameText = itemView.findViewById(R.id.waiting_name);
            phoneText = itemView.findViewById(R.id.waiting_phone);
            tag = itemView.findViewById(R.id.waiting_tag);
            actionsButton = itemView.findViewById(R.id.waiting_actions);
        }

        void bind(final Ticket ticket, final OnTicketActionListener actionListener) {
            numberText.setText(itemView.getContext()
                    .getString(R.string.console_ticket_format, ticket.getTicketNumber()));
            nameText.setText(ticket.getHolderName());
            Context context = itemView.getContext();
            // Under the name: when they moved back, or phone and how long they've waited.
            // Walk-ins have no phone, so just the time, plus a tag saying why.
            if (ticket.isMovedBack() && ticket.getMovedBackAt() != null) {
                phoneText.setText(context.getString(R.string.console_moved_at_format,
                        Format.time(context, ticket.getMovedBackAt())));
                tag.setText(R.string.console_moved_back_tag);
                tag.setVisibility(View.VISIBLE);
            } else if (ticket.isWalkIn()) {
                phoneText.setText(joinedAgo(context, ticket));
                tag.setText(R.string.console_walk_in_label);
                tag.setVisibility(View.VISIBLE);
            } else {
                phoneText.setText(context.getString(R.string.console_joined_phone_format,
                        ticket.getHolderPhone(), joinedAgo(context, ticket)));
                tag.setVisibility(View.GONE);
            }

            actionsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showActions(view, ticket, actionListener);
                }
            });
        }

        private void showActions(View anchor, final Ticket ticket,
                                 final OnTicketActionListener actionListener) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.inflate(R.menu.menu_waiting_ticket);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.waiting_action_remove) {
                        actionListener.onRemoveTicket(ticket);
                        return true;
                    }
                    return false;
                }
            });
            popup.show();
        }
    }

    /** "18 min ago", or "Just joined" under a minute. */
    static String joinedAgo(Context context, Ticket ticket) {
        long minutes = Duration.between(ticket.getJoinedAt(), Instant.now()).toMinutes();
        return minutes < 1 ? context.getString(R.string.console_joined_just_now)
                : context.getString(R.string.console_joined_ago_format, (int) minutes);
    }
}

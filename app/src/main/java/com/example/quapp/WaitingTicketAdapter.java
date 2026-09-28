package com.example.quapp;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WaitingTicketAdapter
        extends RecyclerView.Adapter<WaitingTicketAdapter.WaitingViewHolder> {

    public interface OnTicketActionListener {
        void onServeTicket(Ticket ticket);
        void onNoShowTicket(Ticket ticket);
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
        private final ImageButton actionsButton;

        WaitingViewHolder(@NonNull View itemView) {
            super(itemView);
            numberText = itemView.findViewById(R.id.waiting_number);
            nameText = itemView.findViewById(R.id.waiting_name);
            phoneText = itemView.findViewById(R.id.waiting_phone);
            actionsButton = itemView.findViewById(R.id.waiting_actions);
        }

        void bind(final Ticket ticket, final OnTicketActionListener actionListener) {
            numberText.setText(itemView.getContext()
                    .getString(R.string.console_ticket_format, ticket.getTicketNumber()));
            nameText.setText(ticket.getHolderName());
            // Walk-ins have no phone; say so instead of leaving a blank line.
            if (ticket.getHolderPhone().isEmpty()) {
                phoneText.setText(R.string.console_walk_in_label);
            } else {
                phoneText.setText(ticket.getHolderPhone());
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
                    if (item.getItemId() == R.id.waiting_action_serve) {
                        actionListener.onServeTicket(ticket);
                        return true;
                    }
                    if (item.getItemId() == R.id.waiting_action_no_show) {
                        actionListener.onNoShowTicket(ticket);
                        return true;
                    }
                    return false;
                }
            });
            popup.show();
        }
    }
}
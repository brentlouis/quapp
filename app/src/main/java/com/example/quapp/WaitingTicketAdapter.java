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
        private final View tag;
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
            // Walk-ins have no phone: a Walk-in tag instead of an empty line.
            boolean walkIn = ticket.getHolderPhone().isEmpty();
            phoneText.setVisibility(walkIn ? View.GONE : View.VISIBLE);
            phoneText.setText(maskPhone(itemView.getContext(), ticket.getHolderPhone()));
            tag.setVisibility(walkIn ? View.VISIBLE : View.GONE);

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

    /**
     * "0917 ••• 0002": enough for staff to match a person at the counter without showing the
     * whole number on a screen other people can see.
     */
    static String maskPhone(Context context, String phone) {
        if (phone.length() < 8) {
            return phone;
        }
        return context.getString(R.string.console_masked_phone_format,
                phone.substring(0, 4), phone.substring(phone.length() - 4));
    }
}

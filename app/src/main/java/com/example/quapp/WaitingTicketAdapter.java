package com.example.quapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

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
        private final MaterialButton serveButton;
        private final MaterialButton noShowButton;

        WaitingViewHolder(@NonNull View itemView) {
            super(itemView);
            numberText = itemView.findViewById(R.id.waiting_number);
            nameText = itemView.findViewById(R.id.waiting_name);
            phoneText = itemView.findViewById(R.id.waiting_phone);
            serveButton = itemView.findViewById(R.id.waiting_serve);
            noShowButton = itemView.findViewById(R.id.waiting_no_show);
        }

        void bind(final Ticket ticket, final OnTicketActionListener actionListener) {
            numberText.setText(itemView.getContext()
                    .getString(R.string.console_ticket_format, ticket.getTicketNumber()));
            nameText.setText(ticket.getHolderName());
            phoneText.setText(ticket.getHolderPhone());

            serveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    actionListener.onServeTicket(ticket);
                }
            });

            noShowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    actionListener.onNoShowTicket(ticket);
                }
            });
        }
    }
}
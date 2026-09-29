package com.example.quapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The tickets under the spotlight on My tickets (canvas 12), grouped under a label:
 * "In line now", "Coming up" (the queue hasn't opened yet) and "Just finished".
 *
 * Two view types in one list: a label row and a ticket card. The rows are built once per
 * submit, so the label only appears where the group changes.
 */
public class MyTicketsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    private static final int TYPE_LABEL = 0;
    private static final int TYPE_TICKET = 1;

    /** Either a label (a String resource id, as an Integer) or a Ticket. */
    private final List<Object> rows = new ArrayList<>();
    private final OnTicketClickListener listener;

    public MyTicketsAdapter(OnTicketClickListener listener) {
        this.listener = listener;
    }

    /** Tickets already in display order (ActiveTicketStore.tickets() sorts them). */
    public void submitTickets(List<Ticket> tickets) {
        rows.clear();
        int lastLabel = 0;
        for (Ticket ticket : tickets) {
            int label = sectionFor(ticket);
            if (label != lastLabel) {
                rows.add(label);
                lastLabel = label;
            }
            rows.add(ticket);
        }
        notifyDataSetChanged();
    }

    /** Which label a ticket sits under; the spotlight above the list uses the same rule. */
    @StringRes
    public static int sectionFor(Ticket ticket) {
        switch (ticket.getStatus()) {
            case CALLED:
                return R.string.my_tickets_section_needs_you;
            case WAITING:
                return isUpcoming(ticket)
                        ? R.string.my_tickets_section_coming_up : R.string.my_tickets_section_in_line;
            default:
                return R.string.my_tickets_section_done;
        }
    }

    /** Waiting for a queue that hasn't opened yet ("Join early"). */
    static boolean isUpcoming(Ticket ticket) {
        Queue queue = FakeData.queueById(ticket.getQueueId());
        return queue != null && queue.getStatus() == Queue.Status.UPCOMING;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof Ticket ? TYPE_TICKET : TYPE_LABEL;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_LABEL) {
            return new LabelHolder(inflater.inflate(R.layout.item_section_label, parent, false));
        }
        return new TicketHolder(inflater.inflate(R.layout.item_my_ticket, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if (holder instanceof LabelHolder) {
            ((LabelHolder) holder).label.setText((Integer) row);
        } else {
            ((TicketHolder) holder).bind((Ticket) row, listener);
        }
    }

    /** The second line of a card: where it is and how long, in the ticket's own terms. */
    static String detail(Context context, Ticket ticket) {
        int ahead = Math.max(0, ticket.getPosition() - 1);
        switch (ticket.getStatus()) {
            case WAITING:
                if (isUpcoming(ticket)) {
                    Queue queue = FakeData.queueById(ticket.getQueueId());
                    // "1 PM tomorrow", "9 AM Wed, Sep 30": today and tomorrow read lower-case mid-line.
                    String day = Format.day(context, queue.getStartDate());
                    if (!queue.getStartDate().isAfter(Format.today().plusDays(1))) {
                        day = day.toLowerCase(Locale.getDefault());
                    }
                    String opens = context.getString(R.string.my_tickets_opens_format,
                            Format.time(context, queue.getOpensAt()), day);
                    return context.getString(R.string.my_tickets_row_upcoming_format,
                            ticket.getVenue(), opens, ahead);
                }
                return context.getString(R.string.my_tickets_row_waiting_format,
                        ticket.getVenue(), ahead, ticket.getEstimatedWaitMinutes());
            case CALLED:
                return context.getString(R.string.my_tickets_called_hint);
            default:
                return context.getString(R.string.my_tickets_row_finished_format,
                        ticket.getVenue(), context.getString(doneLabel(ticket.getStatus())));
        }
    }

    @StringRes
    static int doneLabel(Ticket.Status status) {
        switch (status) {
            case SERVED:
                return R.string.my_tickets_served;
            case QUEUE_CLOSED:
                return R.string.my_tickets_queue_closed;
            case REMOVED:
                return R.string.my_tickets_removed;
            case NO_SHOW:
            default:
                return R.string.my_tickets_no_show;
        }
    }

    static class LabelHolder extends RecyclerView.ViewHolder {
        final TextView label;

        LabelHolder(View itemView) {
            super(itemView);
            label = (TextView) itemView;
        }
    }

    static class TicketHolder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView queue;
        final TextView detail;

        TicketHolder(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.my_ticket_number);
            queue = itemView.findViewById(R.id.my_ticket_queue);
            detail = itemView.findViewById(R.id.my_ticket_detail);
        }

        void bind(final Ticket ticket, final OnTicketClickListener listener) {
            Context context = itemView.getContext();
            number.setText(context.getString(R.string.ticket_number_format, ticket.getTicketNumber()));
            queue.setText(ticket.getQueueName());
            detail.setText(detail(context, ticket));
            itemView.setContentDescription(context.getString(R.string.my_tickets_open_description,
                    ticket.getQueueName(), ticket.getTicketNumber()));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onTicketClick(ticket);
                }
            });
        }
    }
}

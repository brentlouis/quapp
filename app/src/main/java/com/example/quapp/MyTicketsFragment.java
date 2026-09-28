package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.shape.ShapeAppearanceModel;

/**
 * The My tickets tab (canvas 12b). ActiveTicketStore holds one ticket today, so this shows that
 * ticket as the screen's spotlight, or an empty state. The list of several tickets (canvas 12)
 * comes with the model change for multiple tickets.
 */
public class MyTicketsFragment extends Fragment {

    private TextView count;
    private TextView section;
    private View ticketView;
    private TextView number;
    private TextView label;
    private TextView headline;
    private TextView headlineSuffix;
    private TextView queue;
    private TextView detail;
    private View empty;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_tickets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        count = view.findViewById(R.id.my_tickets_count);
        section = view.findViewById(R.id.my_tickets_section);
        ticketView = view.findViewById(R.id.my_tickets_ticket);
        number = view.findViewById(R.id.my_tickets_number);
        label = view.findViewById(R.id.my_tickets_label);
        headline = view.findViewById(R.id.my_tickets_headline);
        headlineSuffix = view.findViewById(R.id.my_tickets_headline_suffix);
        queue = view.findViewById(R.id.my_tickets_queue);
        detail = view.findViewById(R.id.my_tickets_detail);
        empty = view.findViewById(R.id.my_tickets_empty);

        ticketView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireContext(), ActiveTicketActivity.class));
            }
        });

        // The stub's bottom punch is placed from the right (Material draws that edge right→left),
        // so the shape needs the ticket's width, which only exists after layout.
        ticketView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left != oldRight - oldLeft) {
                    applyTicketShape(v, right - left);
                }
            }
        });

        view.findViewById(R.id.my_tickets_empty_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(QueuerHomeActivity.intent(requireContext(), QueuerHomeActivity.TAB_BROWSE));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        bind();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            bind();
        }
    }

    /** Espresso ticket with its holes top and bottom, centred on the tear line. */
    private void applyTicketShape(View v, int width) {
        // The tear line is 2dp wide and starts at the stub's edge, so its centre is 1dp further in.
        float tearCentre = getResources().getDimension(R.dimen.my_tickets_stub_width)
                + getResources().getDimension(R.dimen.hairline);
        ShapeAppearanceModel shape = TicketShapes.stub(requireContext(),
                R.dimen.radius_md, R.dimen.punch_radius, tearCentre, width);
        v.setBackground(TicketShapes.background(shape,
                ContextCompat.getColor(requireContext(), R.color.spotlight)));
    }

    private void bind() {
        Ticket ticket = ActiveTicketStore.getTicket();
        boolean hasTicket = ticket != null;

        count.setText(getResources().getQuantityString(R.plurals.my_tickets_count,
                hasTicket ? 1 : 0, hasTicket ? 1 : 0));
        section.setVisibility(hasTicket ? View.VISIBLE : View.GONE);
        ticketView.setVisibility(hasTicket ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasTicket ? View.GONE : View.VISIBLE);
        if (!hasTicket) {
            return;
        }

        number.setText(getString(R.string.my_tickets_number, ticket.getTicketNumber()));
        queue.setText(ticket.getVenue() == null ? ticket.getQueueName()
                : ticket.getQueueName() + " · " + ticket.getVenue());
        ticketView.setContentDescription(getString(R.string.my_tickets_open_description,
                ticket.getQueueName(), ticket.getTicketNumber()));

        // Marigold only for a number being called (DESIGN.md: signal only on the spotlight).
        boolean called = ticket.getStatus() == Ticket.Status.CALLED;
        number.setTextColor(ContextCompat.getColor(requireContext(),
                called ? R.color.signal : R.color.on_spotlight));

        switch (ticket.getStatus()) {
            case CALLED:
                section.setText(R.string.my_tickets_section_needs_you);
                label.setText(R.string.my_tickets_label_called);
                headline.setVisibility(View.GONE);
                headlineSuffix.setText(R.string.my_tickets_called);
                detail.setText(R.string.my_tickets_called_hint);
                break;
            case SERVED:
            case NO_SHOW:
            case QUEUE_CLOSED:
            case REMOVED:
                section.setText(R.string.my_tickets_section_done);
                label.setText(doneLabel(ticket.getStatus()));
                headline.setVisibility(View.GONE);
                headlineSuffix.setText(null);
                detail.setText(null);
                break;
            case WAITING:
            default:
                section.setText(R.string.my_tickets_section_in_line);
                label.setText(R.string.my_tickets_label_in_line);
                headline.setVisibility(View.VISIBLE);
                // Position counts the queuer too, so the people ahead are one fewer.
                headline.setText(String.valueOf(Math.max(0, ticket.getPosition() - 1)));
                headlineSuffix.setText(R.string.my_tickets_ahead);
                detail.setText(getString(R.string.my_tickets_wait, ticket.getEstimatedWaitMinutes()));
                break;
        }
    }

    private static int doneLabel(Ticket.Status status) {
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
}

package com.example.quapp;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Locale;

/**
 * The My tickets tab (canvas 12 and 12b). The ticket that matters most is the spotlight: the
 * called card with its timer and I'm here when a queue is calling you, otherwise the espresso
 * stub ticket. Any other tickets are cards below it. With no tickets, an empty state.
 */
public class MyTicketsFragment extends Fragment implements MyTicketsAdapter.OnTicketClickListener {

    private TextView count;
    private TextView section;
    private View ticketView;
    private TextView number;
    private TextView label;
    private TextView headline;
    private TextView headlineSuffix;
    private TextView queue;
    private TextView detail;
    private View called;
    private TextView calledCountdown;
    private CircularProgressIndicator calledRing;
    private View ruleNote;
    private View empty;
    private MyTicketsAdapter adapter;
    private CountDownTimer graceTimer;

    /** The ticket shown as the spotlight, so its click and I'm here know which one. */
    private Ticket spotlight;

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
        called = view.findViewById(R.id.my_tickets_called);
        calledCountdown = view.findViewById(R.id.my_tickets_called_countdown);
        calledRing = view.findViewById(R.id.my_tickets_called_ring);
        ruleNote = view.findViewById(R.id.my_tickets_rule_note);
        empty = view.findViewById(R.id.my_tickets_empty);

        RecyclerView list = view.findViewById(R.id.my_tickets_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MyTicketsAdapter(this);
        list.setAdapter(adapter);

        // The called card is the spotlight with punches halfway down its sides.
        called.setBackground(TicketShapes.spotlightBackground(requireContext()));

        View.OnClickListener openSpotlight = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spotlight != null) {
                    onTicketClick(spotlight);
                }
            }
        };
        ticketView.setOnClickListener(openSpotlight);
        called.setOnClickListener(openSpotlight);

        view.findViewById(R.id.my_tickets_called_here).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spotlight == null) {
                    return;
                }
                ActiveTicketStore.markServed(spotlight.getId());
                startActivity(ActiveTicketActivity.intent(requireContext(), spotlight.getId()));
            }
        });

        view.findViewById(R.id.my_tickets_called_move_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spotlight == null) {
                    return;
                }
                MoreTimeSheet.show(requireContext(), spotlight.getId(), new MoreTimeSheet.OnMovedListener() {
                    @Override
                    public void onMoved(Ticket moved, int places) {
                        bind();
                        Snackbar.make(requireView(), getResources().getQuantityString(R.plurals.more_time_moved,
                                places, places, moved.getTicketNumber()), Snackbar.LENGTH_LONG).show();
                    }
                });
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
    public void onPause() {
        super.onPause();
        cancelGraceTimer();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            cancelGraceTimer();
        } else {
            bind();
        }
    }

    @Override
    public void onTicketClick(Ticket ticket) {
        startActivity(ActiveTicketActivity.intent(requireContext(), ticket.getId()));
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
        cancelGraceTimer();
        List<Ticket> tickets = ActiveTicketStore.tickets();
        boolean any = !tickets.isEmpty();
        spotlight = any ? tickets.get(0) : null;

        bindCount(tickets);
        section.setVisibility(any ? View.VISIBLE : View.GONE);
        ruleNote.setVisibility(any ? View.VISIBLE : View.GONE);
        empty.setVisibility(any ? View.GONE : View.VISIBLE);
        // The first ticket is the spotlight; the list holds the rest.
        adapter.submitTickets(any ? tickets.subList(1, tickets.size()) : tickets);
        if (!any) {
            ticketView.setVisibility(View.GONE);
            called.setVisibility(View.GONE);
            return;
        }

        section.setText(MyTicketsAdapter.sectionFor(spotlight));
        boolean isCalled = spotlight.getStatus() == Ticket.Status.CALLED;
        called.setVisibility(isCalled ? View.VISIBLE : View.GONE);
        ticketView.setVisibility(isCalled ? View.GONE : View.VISIBLE);
        if (isCalled) {
            bindCalled(spotlight);
        } else {
            bindTicket(spotlight);
        }
        // The tab badge follows the tickets, so it's refreshed whenever this list is.
        if (getActivity() instanceof QueuerHomeActivity) {
            ((QueuerHomeActivity) getActivity()).bindTicketBadge();
        }
    }

    /** "2 tickets", or "2 tickets · 1 needs you now" while one is being called. */
    private void bindCount(List<Ticket> tickets) {
        String text = getResources().getQuantityString(R.plurals.my_tickets_count,
                tickets.size(), tickets.size());
        if (!tickets.isEmpty() && tickets.get(0).getStatus() == Ticket.Status.CALLED) {
            text = getString(R.string.my_tickets_count_called_format, text);
        }
        count.setText(text);
    }

    /** Canvas 12: the number in marigold, the grace timer and I'm here. */
    private void bindCalled(Ticket ticket) {
        ((TextView) called.findViewById(R.id.my_tickets_called_number)).setText(
                getString(R.string.ticket_number_format, ticket.getTicketNumber()));
        ((TextView) called.findViewById(R.id.my_tickets_called_queue)).setText(getString(
                R.string.my_tickets_row_finished_format, ticket.getQueueName(), ticket.getVenue()));
        called.setContentDescription(getString(R.string.my_tickets_open_description,
                ticket.getQueueName(), ticket.getTicketNumber()));
        // Moving back is once per ticket, as on the Called screen.
        called.findViewById(R.id.my_tickets_called_move_back)
                .setVisibility(ticket.isMovedBack() ? View.GONE : View.VISIBLE);

        graceTimer = new CountDownTimer(ActiveTicketStore.graceRemainingMs(ticket), 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000L;
                calledCountdown.setText(String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L));
                calledRing.setProgressCompat((int) (millisUntilFinished * calledRing.getMax()
                        / CalledActivity.GRACE_PERIOD_MS), false);
            }

            @Override
            public void onFinish() {
                // The store releases an overdue ticket as soon as it's read.
                bind();
            }
        };
        graceTimer.start();
    }

    /** Canvas 12b: the stub ticket, waiting or just finished. */
    private void bindTicket(Ticket ticket) {
        number.setText(getString(R.string.my_tickets_number, ticket.getTicketNumber()));
        number.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_spotlight));
        queue.setText(getString(R.string.my_tickets_row_finished_format,
                ticket.getQueueName(), ticket.getVenue()));
        ticketView.setContentDescription(getString(R.string.my_tickets_open_description,
                ticket.getQueueName(), ticket.getTicketNumber()));

        if (ticket.getStatus() == Ticket.Status.WAITING) {
            label.setText(R.string.my_tickets_label_in_line);
            headline.setVisibility(View.VISIBLE);
            // Position counts the queuer too, so the people ahead are one fewer.
            headline.setText(String.valueOf(Math.max(0, ticket.getPosition() - 1)));
            headlineSuffix.setText(R.string.my_tickets_ahead);
            detail.setText(MyTicketsAdapter.isUpcoming(ticket)
                    ? MyTicketsAdapter.detail(requireContext(), ticket)
                    : getString(R.string.my_tickets_wait, ticket.getEstimatedWaitMinutes()));
        } else {
            label.setText(MyTicketsAdapter.doneLabel(ticket.getStatus()));
            headline.setVisibility(View.GONE);
            headlineSuffix.setText(null);
            detail.setText(null);
        }
    }

    private void cancelGraceTimer() {
        if (graceTimer != null) {
            graceTimer.cancel();
            graceTimer = null;
        }
    }
}

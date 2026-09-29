package com.example.quapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.time.LocalTime;
import java.util.List;

/**
 * Browse, the queuer's home tab (canvas 05, 33, 34, 40, 41).
 *
 * Three states, decided in {@link #refresh()}:
 * - no town chosen yet: the first-visit question and "Open now across Bohol"
 * - a town, holding a ticket: the "You're in line" banner above the cards
 * - a town, no ticket: "Shortest wait nearby" above the cards
 * The town is remembered in Session; the category and search are per visit.
 */
public class BrowseFragment extends Fragment implements QueueAdapter.OnQueueClickListener {

    private static final String STATE_CATEGORY = "category";
    /** Town chips shown on the first-visit card before "N more". */
    private static final int FIRST_VISIT_TOWNS = 6;

    private Session session;
    private QueueAdapter cardAdapter;
    private OpenNowAdapter openNowAdapter;

    private RecyclerView list;
    private View emptyState;
    private View banner;
    private View highlight;
    private View firstVisit;
    private View filters;
    private TextView count;
    private EditText searchInput;
    private Chip townChip;
    private ChipGroup categoryGroup;
    private Chip allChip;

    // null means "no filter"
    private Category selectedCategory;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = new Session(requireContext());

        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString(STATE_CATEGORY);
            selectedCategory = saved == null ? null : Category.valueOf(saved);
        }

        Grain.groundBehind(view.findViewById(R.id.browse_app_bar));
        list = view.findViewById(R.id.browse_list);
        emptyState = view.findViewById(R.id.browse_empty);
        banner = view.findViewById(R.id.browse_banner);
        highlight = view.findViewById(R.id.browse_highlight);
        firstVisit = view.findViewById(R.id.browse_first_visit);
        filters = view.findViewById(R.id.browse_filters);
        count = view.findViewById(R.id.browse_count);
        searchInput = view.findViewById(R.id.browse_search_input);
        townChip = view.findViewById(R.id.browse_town_chip);
        categoryGroup = view.findViewById(R.id.browse_category_group);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        cardAdapter = new QueueAdapter(this);
        openNowAdapter = new OpenNowAdapter(this);

        // The banner is a slim spotlight with small punches in its sides.
        banner.setBackground(TicketShapes.background(requireContext(),
                TicketShapes.sidePunched(requireContext(), R.dimen.radius_md,
                        R.dimen.punch_radius_banner, 0.5f),
                ContextCompat.getColor(requireContext(), R.color.spotlight)));
        banner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBannerTicket();
            }
        });

        setUpSearch();
        setUpTownChip();
        setUpCategoryChips();
        setUpFirstVisit(view);
    }

    /** Coming back from Queue detail, Join or the ticket screen. */
    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    /** Coming back from another tab. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refresh();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CATEGORY, selectedCategory == null ? null : selectedCategory.name());
    }

    // ---- Setup ----------------------------------------------------------------

    private void setUpSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refresh();
            }
        });
    }

    private void setUpTownChip() {
        View.OnClickListener pick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTown();
            }
        };
        townChip.setOnClickListener(pick);
        townChip.setOnCloseIconClickListener(pick);
    }

    /** "All" plus one chip per category, each with its icon. */
    private void setUpCategoryChips() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        allChip = addCategoryChip(inflater, getString(R.string.browse_filter_all), null);
        Chip toCheck = allChip;
        for (Category category : Category.values()) {
            Chip chip = addCategoryChip(inflater, getString(category.label), category);
            chip.setChipIconResource(category.icon);
            chip.setChipIconVisible(true);
            if (category == selectedCategory) {
                toCheck = chip;
            }
        }
        toCheck.setChecked(true);

        categoryGroup.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                if (checkedIds.isEmpty()) {
                    return;
                }
                Chip checked = group.findViewById(checkedIds.get(0));
                selectedCategory = (Category) checked.getTag();
                refresh();
            }
        });
    }

    /** The tag carries the category the chip filters by; null for "All". */
    private Chip addCategoryChip(LayoutInflater inflater, String label, @Nullable Category category) {
        Chip chip = (Chip) inflater.inflate(R.layout.view_filter_chip, categoryGroup, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(category);
        punchWhenChecked(chip);
        categoryGroup.addView(chip);
        return chip;
    }

    /** A selected chip becomes a punched ticket (DESIGN.md "Punched selection"). */
    private void punchWhenChecked(final Chip chip) {
        final ShapeAppearanceModel pill = chip.getShapeAppearanceModel();
        final ShapeAppearanceModel punched =
                TicketShapes.selection(requireContext(), R.dimen.radius_selection);
        chip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                chip.setShapeAppearanceModel(isChecked ? punched : pill);
            }
        });
    }

    /** First visit: the first few towns as chips, the rest behind "N more". */
    private void setUpFirstVisit(View view) {
        ChipGroup towns = view.findViewById(R.id.browse_town_chips);
        String[] all = getResources().getStringArray(R.array.bohol_municipalities);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < Math.min(FIRST_VISIT_TOWNS, all.length); i++) {
            final String town = all[i];
            Chip chip = (Chip) inflater.inflate(R.layout.view_filter_chip, towns, false);
            chip.setText(town);
            chip.setCheckable(false);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chooseTown(town);
                }
            });
            towns.addView(chip);
        }
        if (all.length > FIRST_VISIT_TOWNS) {
            Chip more = (Chip) inflater.inflate(R.layout.view_filter_chip, towns, false);
            more.setText(getString(R.string.browse_more_towns_format, all.length - FIRST_VISIT_TOWNS));
            more.setCheckable(false);
            more.setCloseIconResource(R.drawable.ic_chevron_down);
            more.setCloseIconVisible(true);
            View.OnClickListener pick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickTown();
                }
            };
            more.setOnClickListener(pick);
            more.setOnCloseIconClickListener(pick);
            towns.addView(more);
        }
    }

    private void pickTown() {
        final String[] towns = getResources().getStringArray(R.array.bohol_municipalities);
        int checked = -1;
        for (int i = 0; i < towns.length; i++) {
            if (towns[i].equals(session.getTown())) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.browse_town_dialog_title)
                .setSingleChoiceItems(towns, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseTown(towns[which]);
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void chooseTown(String town) {
        session.setTown(town);
        refresh();
    }

    private void clearFilters() {
        searchInput.setText(null);
        allChip.setChecked(true); // fires the listener, which clears the category and refreshes
        refresh();
    }

    // ---- Rendering ----------------------------------------------------------

    private void refresh() {
        if (getView() == null) {
            return;
        }
        List<Queue> all = FakeData.queues();
        String town = session.getTown();
        bindGreeting(town == null);

        if (town == null) {
            renderFirstVisit(all);
        } else {
            renderTown(all, town);
        }
    }

    /** "Good morning, Maria" by the hour, or "Welcome, Maria" on a first visit. */
    private void bindGreeting(boolean firstVisit) {
        String name = session.getName();
        int hour = LocalTime.now(Format.MANILA).getHour();
        int greeting = firstVisit ? R.string.browse_welcome
                : hour < 12 ? R.string.browse_good_morning
                : hour < 18 ? R.string.browse_good_afternoon
                : R.string.browse_good_evening;
        String first = name == null ? null : name.trim().split("\\s+")[0];
        TextView view = getView().findViewById(R.id.browse_greeting);
        view.setText(first == null ? getString(greeting)
                : getString(R.string.browse_greeting_name_format, getString(greeting), first));
    }

    private void renderFirstVisit(List<Queue> all) {
        firstVisit.setVisibility(View.VISIBLE);
        banner.setVisibility(View.GONE);
        highlight.setVisibility(View.GONE);
        filters.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        searchInput.setHint(R.string.browse_search_any_hint);

        String query = searchInput.getText().toString();
        List<Queue> queues = query.trim().isEmpty()
                ? QueueFilter.openNowAcrossBohol(all)
                : QueueFilter.forBrowse(QueueFilter.apply(all, query, null, null));
        count.setText(getString(query.trim().isEmpty() ? R.string.browse_open_now_format
                : R.string.browse_results_format, queues.size()));
        list.setAdapter(openNowAdapter);
        openNowAdapter.submitQueues(queues);
        list.setVisibility(View.VISIBLE);
    }

    private void renderTown(List<Queue> all, String town) {
        firstVisit.setVisibility(View.GONE);
        filters.setVisibility(View.VISIBLE);
        searchInput.setHint(R.string.browse_search_hint);
        townChip.setText(town);

        bindBannerOrHighlight(all, town);

        String query = searchInput.getText().toString();
        boolean searching = !query.trim().isEmpty();
        List<Queue> queues = QueueFilter.forBrowse(
                QueueFilter.apply(all, query, selectedCategory, town));

        count.setText(searching ? getString(R.string.browse_results_format, queues.size())
                : getString(R.string.browse_in_town_format, town, queues.size()));
        list.setAdapter(cardAdapter);
        cardAdapter.submitQueues(queues);

        boolean empty = queues.isEmpty();
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            if (!searching && selectedCategory == null) {
                bindEmptyTown(all, town);
            } else {
                bindNoResults(query.trim());
            }
        }
    }

    /** One ticket opens straight to it; with several, My tickets shows them all. */
    private void openBannerTicket() {
        List<Ticket> live = ActiveTicketStore.liveTickets();
        if (live.size() == 1) {
            startActivity(ActiveTicketActivity.intent(requireContext(), live.get(0).getId()));
        } else if (!live.isEmpty()) {
            startActivity(QueuerHomeActivity.intent(requireContext(), QueuerHomeActivity.TAB_TICKETS));
        }
    }

    /**
     * In line: the spotlight banner, for the ticket that matters most (being called, else the
     * soonest). Otherwise: the shortest open wait in town.
     */
    private void bindBannerOrHighlight(List<Queue> all, String town) {
        Ticket ticket = ActiveTicketStore.mostUrgent();
        if (ticket != null) {
            banner.setVisibility(View.VISIBLE);
            highlight.setVisibility(View.GONE);
            boolean called = ticket.getStatus() == Ticket.Status.CALLED;
            TextView number = banner.findViewById(R.id.banner_number);
            number.setText(getString(R.string.ticket_number_format, ticket.getTicketNumber()));
            // Marigold only while you're being called (signal only on the spotlight).
            number.setTextColor(ContextCompat.getColor(requireContext(),
                    called ? R.color.signal : R.color.on_spotlight));
            ((TextView) banner.findViewById(R.id.banner_title)).setText(getString(
                    called ? R.string.browse_banner_called_format : R.string.browse_banner_in_line_format,
                    ticket.getQueueName()));
            int ahead = Math.max(0, ticket.getPosition() - 1);
            ((TextView) banner.findViewById(R.id.banner_detail)).setText(called
                    ? getString(R.string.browse_banner_confirm)
                    : getString(R.string.browse_banner_detail_format, ahead,
                            ticket.getEstimatedWaitMinutes()));
            banner.setContentDescription(getString(R.string.ticket_description,
                    ticket.getTicketNumber(), ticket.getQueueName()));
            return;
        }

        banner.setVisibility(View.GONE);
        final Queue best = QueueFilter.shortestWait(all, town);
        highlight.setVisibility(best == null ? View.GONE : View.VISIBLE);
        if (best == null) {
            return;
        }
        ((TextView) highlight.findViewById(R.id.browse_highlight_name)).setText(best.getName());
        ((TextView) highlight.findViewById(R.id.browse_highlight_where)).setText(getString(
                R.string.browse_highlight_where_format, best.getVenue(), best.getPeopleWaiting()));
        ((TextView) highlight.findViewById(R.id.browse_highlight_wait)).setText(QueueCards.withUnit(
                String.valueOf(best.getEstimatedWaitMinutes()), getString(R.string.card_minutes_unit)));
        highlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onQueueClick(best);
            }
        });
    }

    /** "No queues in Dauis yet", with the town that has the most open. */
    private void bindEmptyTown(List<Queue> all, String town) {
        View view = getView();
        ((TextView) view.findViewById(R.id.browse_empty_title)).setText(
                getString(R.string.browse_empty_town_title, town));
        ((TextView) view.findViewById(R.id.browse_empty_body)).setText(
                getString(R.string.browse_empty_town_body, town));
        MaterialButton button = view.findViewById(R.id.browse_empty_button);
        button.setText(R.string.browse_change_town);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTown();
            }
        });

        final String nearby = QueueFilter.busiestOtherTown(all, town);
        View label = view.findViewById(R.id.browse_nearby_label);
        View row = view.findViewById(R.id.browse_nearby_row);
        label.setVisibility(nearby == null ? View.GONE : View.VISIBLE);
        row.setVisibility(nearby == null ? View.GONE : View.VISIBLE);
        if (nearby == null) {
            return;
        }
        Queue fastest = QueueFilter.shortestWait(all, nearby);
        ListRow.bind(row, R.drawable.ic_map_pin, nearby, getString(R.string.browse_nearby_detail_format,
                QueueFilter.openCount(all, nearby),
                fastest == null ? 0 : fastest.getEstimatedWaitMinutes()));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseTown(nearby);
            }
        });
    }

    /** 'No results for "passport"' or "Nothing in Medical right now". */
    private void bindNoResults(String query) {
        View view = getView();
        String category = selectedCategory == null ? null : getString(selectedCategory.label);
        ((TextView) view.findViewById(R.id.browse_empty_title)).setText(query.isEmpty()
                ? getString(R.string.browse_nothing_in_format, category)
                : getString(R.string.browse_no_results_format, query));
        ((TextView) view.findViewById(R.id.browse_empty_body)).setText(category == null
                ? getString(R.string.browse_no_results_body)
                : getString(R.string.browse_no_results_category_body, category));
        MaterialButton button = view.findViewById(R.id.browse_empty_button);
        button.setText(R.string.browse_empty_action);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFilters();
            }
        });
        view.findViewById(R.id.browse_nearby_label).setVisibility(View.GONE);
        view.findViewById(R.id.browse_nearby_row).setVisibility(View.GONE);
    }

    @Override
    public void onQueueClick(Queue queue) {
        Intent intent = new Intent(requireContext(), QueueDetailActivity.class);
        intent.putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, queue.getId());
        startActivity(intent);
    }
}

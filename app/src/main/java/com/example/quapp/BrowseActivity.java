package com.example.quapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class BrowseActivity extends AppCompatActivity
        implements QueueAdapter.OnQueueClickListener {

    private static final String STATE_CATEGORY = "category";
    private static final String STATE_MUNICIPALITY = "municipality";

    private QueueAdapter queueAdapter;
    private RecyclerView queueList;
    private View emptyState;

    private MaterialCardView ticketCard;
    private TextView ticketTitle;
    private TextView ticketStatus;
    private View ticketDot;

    private TextInputEditText searchInput;
    private ChipGroup categoryGroup;
    private Chip locationChip;
    private Chip allCategoriesChip;

    // null means "no filter" for both.
    private String selectedCategory;
    private String selectedMunicipality;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);
        SystemBars.applyPadding(findViewById(R.id.browse_root));

        if (savedInstanceState != null) {
            selectedCategory = savedInstanceState.getString(STATE_CATEGORY);
            selectedMunicipality = savedInstanceState.getString(STATE_MUNICIPALITY);
        }

        queueList = findViewById(R.id.browse_list);
        emptyState = findViewById(R.id.browse_empty);
        queueList.setLayoutManager(new LinearLayoutManager(this));
        queueList.setHasFixedSize(true);

        queueAdapter = new QueueAdapter(this);
        queueList.setAdapter(queueAdapter);

        findViewById(R.id.browse_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(BrowseActivity.this, ProfileActivity.class));
            }
        });

        setUpTicketCard();
        setUpSearch();
        setUpCategoryChips();
        setUpLocationChip();

        MaterialButton clearButton = findViewById(R.id.browse_empty_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearFilters();
            }
        });
    }

    /**
     * onResume, not onCreate: Browse stays underneath while the queuer joins or
     * finishes a ticket, and these need to be fresh every time it comes back.
     */
    @Override
    protected void onResume() {
        super.onResume();
        bindTicketCard();
        applyFilters();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CATEGORY, selectedCategory);
        outState.putString(STATE_MUNICIPALITY, selectedMunicipality);
    }

    // ---- Active ticket card -------------------------------------------------

    private void setUpTicketCard() {
        ticketCard = findViewById(R.id.browse_ticket_card);
        ticketTitle = findViewById(R.id.browse_ticket_title);
        ticketStatus = findViewById(R.id.browse_ticket_status);
        ticketDot = findViewById(R.id.browse_ticket_dot);

        ticketCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(BrowseActivity.this, ActiveTicketActivity.class));
            }
        });
    }

    private void bindTicketCard() {
        Ticket ticket = ActiveTicketStore.getTicket();

        if (ticket == null) {
            ticketCard.setVisibility(View.GONE);
            return;
        }

        ticketCard.setVisibility(View.VISIBLE);
        ticketTitle.setText(getString(R.string.browse_ticket_format,
                ticket.getTicketNumber(), ticket.getQueueName()));

        int colorRes;
        switch (ticket.getStatus()) {
            case CALLED:
                ticketStatus.setText(R.string.browse_ticket_called);
                colorRes = R.color.status_called;
                break;
            case SERVED:
                ticketStatus.setText(R.string.browse_ticket_served);
                colorRes = R.color.status_served;
                break;
            case NO_SHOW:
                ticketStatus.setText(R.string.browse_ticket_expired);
                colorRes = R.color.status_expired;
                break;
            case WAITING:
            default:
                ticketStatus.setText(getString(R.string.browse_ticket_waiting,
                        ticket.getPosition()));
                colorRes = R.color.status_waiting;
                break;
        }

        ticketDot.getBackground().mutate().setTint(ContextCompat.getColor(this, colorRes));
    }

    // ---- Search and filters -------------------------------------------------

    private void setUpSearch() {
        searchInput = findViewById(R.id.browse_search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                applyFilters();
            }
        });
    }

    private void setUpCategoryChips() {
        categoryGroup = findViewById(R.id.browse_category_group);
        LayoutInflater inflater = LayoutInflater.from(this);

        allCategoriesChip = addCategoryChip(inflater, getString(R.string.browse_filter_all), null);
        Chip chipToCheck = allCategoriesChip;

        for (String category : getResources().getStringArray(R.array.queue_categories)) {
            Chip chip = addCategoryChip(inflater, category, category);
            if (category.equals(selectedCategory)) {
                chipToCheck = chip;
            }
        }

        chipToCheck.setChecked(true);

        categoryGroup.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                if (checkedIds.isEmpty()) {
                    return;
                }
                Chip checked = group.findViewById(checkedIds.get(0));
                selectedCategory = (String) checked.getTag();
                applyFilters();
            }
        });
    }

    /** The tag carries the category the chip filters by; null for "All". */
    private Chip addCategoryChip(LayoutInflater inflater, String label, String category) {
        Chip chip = (Chip) inflater.inflate(R.layout.view_filter_chip, categoryGroup, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(category);
        categoryGroup.addView(chip);
        return chip;
    }

    private void setUpLocationChip() {
        locationChip = findViewById(R.id.browse_location_chip);
        bindLocationChip();

        locationChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Tapping a filter chip toggles it; the checked state should only
                // follow the actual selection, so put it back and ask instead.
                bindLocationChip();
                showLocationPicker();
            }
        });
    }

    private void bindLocationChip() {
        locationChip.setChecked(selectedMunicipality != null);
        locationChip.setText(selectedMunicipality == null
                ? getString(R.string.browse_location_all) : selectedMunicipality);
    }

    private void showLocationPicker() {
        final String[] municipalities = getResources().getStringArray(R.array.bohol_municipalities);

        // Option 0 is "All locations"; the rest shift down by one.
        String[] options = new String[municipalities.length + 1];
        options[0] = getString(R.string.browse_location_all);
        System.arraycopy(municipalities, 0, options, 1, municipalities.length);

        int checkedIndex = 0;
        for (int i = 0; i < municipalities.length; i++) {
            if (municipalities[i].equals(selectedMunicipality)) {
                checkedIndex = i + 1;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.browse_location_dialog_title)
                .setSingleChoiceItems(options, checkedIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedMunicipality = which == 0 ? null : municipalities[which - 1];
                        bindLocationChip();
                        applyFilters();
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void clearFilters() {
        selectedMunicipality = null;
        bindLocationChip();
        allCategoriesChip.setChecked(true); // fires the listener, which clears selectedCategory
        searchInput.setText(null);
        applyFilters();
    }

    private void applyFilters() {
        String query = searchInput.getText() == null ? "" : searchInput.getText().toString();
        List<Queue> matches = QueueFilter.apply(
                FakeData.queues(), query, selectedCategory, selectedMunicipality);

        queueAdapter.submitQueues(matches);

        boolean empty = matches.isEmpty();
        queueList.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onQueueClick(Queue queue) {
        Intent intent = new Intent(this, QueueDetailActivity.class);
        intent.putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, queue.getId());
        startActivity(intent);
    }
}

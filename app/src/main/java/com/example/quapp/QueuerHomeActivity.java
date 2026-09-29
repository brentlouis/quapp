package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.List;

/**
 * The queuer's home: Browse, My tickets and Profile as tabs (DECISIONS.md, hybrid navigation).
 *
 * Tabs are Fragments that get shown and hidden, never replaced. A hidden Fragment keeps its
 * views, so Browse keeps its scroll position and filters while you look at Profile. Everything
 * reached from a tab (Queue detail, Join, the ticket screen) is still an Activity.
 */
public class QueuerHomeActivity extends AppCompatActivity {

    /** Optional: which tab to open, one of the TAB_ values. */
    public static final String EXTRA_TAB = "com.example.quapp.EXTRA_TAB";
    public static final String TAB_BROWSE = "browse";
    public static final String TAB_TICKETS = "tickets";
    public static final String TAB_PROFILE = "profile";

    private BottomNavigationView nav;
    private OnBackPressedCallback backToBrowse;

    /** Opens this screen on a tab, reusing the existing instance if it's already in the stack. */
    public static Intent intent(Context context, String tab) {
        Intent intent = new Intent(context, QueuerHomeActivity.class);
        intent.putExtra(EXTRA_TAB, tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queuer_home);
        SystemBars.applyPaddingExceptBottom(findViewById(R.id.queuer_home_root));

        nav = findViewById(R.id.queuer_home_nav);
        // The notched ticket shape for the active tab (DESIGN.md section 5, punched selection)
        nav.setItemActiveIndicatorShapeAppearance(
                TicketShapes.selection(this, R.dimen.radius_selection));

        nav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                showTab(item.getItemId());
                return true;
            }
        });

        // Back from Tickets or Profile goes to Browse first; Back from Browse leaves the app.
        backToBrowse = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                nav.setSelectedItemId(R.id.nav_browse);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backToBrowse);

        // On a fresh start pick the tab; after rotation the FragmentManager and the nav restore
        // themselves, and adding the Fragments again would stack duplicates.
        if (savedInstanceState == null) {
            nav.setSelectedItemId(itemIdFor(getIntent().getStringExtra(EXTRA_TAB)));
            showTab(nav.getSelectedItemId());
        } else {
            backToBrowse.setEnabled(nav.getSelectedItemId() != R.id.nav_browse);
        }
    }

    /** Called instead of onCreate when intent() brings an existing instance forward. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String tab = intent.getStringExtra(EXTRA_TAB);
        if (tab != null) {
            nav.setSelectedItemId(itemIdFor(tab));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindTicketBadge();
    }

    /** Shows one tab's Fragment and hides the others, creating it the first time. */
    private void showTab(int itemId) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);

        int[] ids = {R.id.nav_browse, R.id.nav_tickets, R.id.nav_profile};
        for (int id : ids) {
            String tag = tagFor(id);
            Fragment fragment = fm.findFragmentByTag(tag);
            if (id == itemId) {
                if (fragment == null) {
                    tx.add(R.id.queuer_home_container, create(id), tag);
                } else {
                    tx.show(fragment);
                }
            } else if (fragment != null) {
                tx.hide(fragment);
            }
        }
        // commitNow, not commit: runs immediately, so a second showTab right after this one
        // finds the Fragment instead of adding a duplicate.
        tx.commitNow();
        backToBrowse.setEnabled(itemId != R.id.nav_browse);
    }

    /**
     * On the My tickets tab: "#43" while you hold one ticket, the count ("2") with several
     * (canvas 12). Espresso and marigold while one of them is being called.
     */
    void bindTicketBadge() {
        List<Ticket> live = ActiveTicketStore.liveTickets();
        if (live.isEmpty()) {
            nav.removeBadge(R.id.nav_tickets);
            return;
        }
        // Sorted with a called ticket first, so the first one says whether anyone is calling you.
        Ticket first = live.get(0);
        BadgeDrawable badge = nav.getOrCreateBadge(R.id.nav_tickets);
        badge.setText(live.size() == 1
                ? getString(R.string.nav_ticket_badge, first.getTicketNumber())
                : getString(R.string.nav_ticket_count, live.size()));
        boolean called = first.getStatus() == Ticket.Status.CALLED;
        // Called: an espresso stub with marigold text. Otherwise ink with paper text.
        badge.setBackgroundColor(ContextCompat.getColor(this, called ? R.color.spotlight : R.color.ink));
        badge.setBadgeTextColor(ContextCompat.getColor(this, called ? R.color.signal : R.color.paper_raised));
    }

    private static Fragment create(int itemId) {
        if (itemId == R.id.nav_tickets) {
            return new MyTicketsFragment();
        } else if (itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return new BrowseFragment();
    }

    private static String tagFor(int itemId) {
        if (itemId == R.id.nav_tickets) {
            return TAB_TICKETS;
        } else if (itemId == R.id.nav_profile) {
            return TAB_PROFILE;
        }
        return TAB_BROWSE;
    }

    private static int itemIdFor(@Nullable String tab) {
        if (TAB_TICKETS.equals(tab)) {
            return R.id.nav_tickets;
        } else if (TAB_PROFILE.equals(tab)) {
            return R.id.nav_profile;
        }
        return R.id.nav_browse;
    }
}

package com.example.quapp;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * The owner's home: Queues, Today and Profile as tabs. Works exactly like QueuerHomeActivity
 * (Fragments shown and hidden, never replaced; Back goes to the first tab, then leaves).
 * Create Queue, Live Console and Insights are still Activities opened from the tabs.
 */
public class OwnerHomeActivity extends AppCompatActivity {

    private static final String TAG_QUEUES = "queues";
    private static final String TAG_TODAY = "today";
    private static final String TAG_PROFILE = "profile";

    private BottomNavigationView nav;
    private OnBackPressedCallback backToQueues;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_home);
        SystemBars.applyPaddingExceptBottom(findViewById(R.id.owner_home_root));

        nav = findViewById(R.id.owner_home_nav);
        nav.setItemActiveIndicatorShapeAppearance(
                TicketShapes.selection(this, R.dimen.radius_selection));
        // The nav is a tear-off stub like the join dock: raised paper, top corners bitten out
        nav.setBackground(TicketShapes.stubDockBackground(this));

        nav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                showTab(item.getItemId());
                return true;
            }
        });

        backToQueues = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                nav.setSelectedItemId(R.id.nav_queues);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backToQueues);

        if (savedInstanceState == null) {
            showTab(R.id.nav_queues);
        } else {
            backToQueues.setEnabled(nav.getSelectedItemId() != R.id.nav_queues);
        }
    }

    /** Shows one tab's Fragment and hides the others, creating it the first time. */
    private void showTab(int itemId) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);

        int[] ids = {R.id.nav_queues, R.id.nav_today, R.id.nav_profile};
        for (int id : ids) {
            String tag = tagFor(id);
            Fragment fragment = fm.findFragmentByTag(tag);
            if (id == itemId) {
                if (fragment == null) {
                    tx.add(R.id.owner_home_container, create(id), tag);
                } else {
                    tx.show(fragment);
                }
            } else if (fragment != null) {
                tx.hide(fragment);
            }
        }
        tx.commitNow();
        backToQueues.setEnabled(itemId != R.id.nav_queues);
    }

    private static Fragment create(int itemId) {
        if (itemId == R.id.nav_today) {
            return new TodayFragment();
        } else if (itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return new OwnerQueuesFragment();
    }

    private static String tagFor(int itemId) {
        if (itemId == R.id.nav_today) {
            return TAG_TODAY;
        } else if (itemId == R.id.nav_profile) {
            return TAG_PROFILE;
        }
        return TAG_QUEUES;
    }
}

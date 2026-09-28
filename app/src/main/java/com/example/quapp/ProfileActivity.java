package com.example.quapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Temporary: hosts ProfileFragment for the owner dashboard's profile button. It goes away when
 * the owner home gets its own tabs (Queues, Today, Profile), just like the queuer side.
 */
public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_host);
        SystemBars.applyPadding(findViewById(R.id.fragment_host));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_host, ProfileFragment.newInstance(true))
                    .commit();
        }
    }
}

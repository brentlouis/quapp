package com.example.quapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SystemBars.applyPadding(findViewById(R.id.splash_root));

        Session session = new Session(this);
        // Existing accounts retain their role and bypass first-install guidance.
        startActivity(session.isLoggedIn() || OnboardingActivity.completed(this)
                ? session.homeIntent(this)
                : new android.content.Intent(this, OnboardingActivity.class));
        finish();
    }
}

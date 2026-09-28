package com.example.quapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 3000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SystemBars.applyPadding(findViewById(R.id.splash_root));

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // A remembered session skips Login and Role Select entirely.
                Session session = new Session(SplashActivity.this);
                startActivity(session.homeIntent(SplashActivity.this));
                finish();
            }
        }, SPLASH_DELAY_MS);
    }
}
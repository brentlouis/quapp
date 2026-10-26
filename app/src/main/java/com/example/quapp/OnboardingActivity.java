package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** First-install guidance only; completion is independent of account/session preferences. */
public class OnboardingActivity extends AppCompatActivity {
    static boolean completed(Context context) {
        return context.getSharedPreferences("quapp_onboarding", MODE_PRIVATE).getBoolean("completed", false);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_onboarding);
        SystemBars.applyPadding(findViewById(R.id.onboarding_root));
        androidx.core.view.ViewCompat.setAccessibilityHeading(findViewById(R.id.onboarding_title), true);
        findViewById(R.id.onboarding_next).setOnClickListener(v -> open(RegisterActivity.class));
        findViewById(R.id.onboarding_sign_in).setOnClickListener(v -> open(LoginActivity.class));
        findViewById(R.id.onboarding_skip).setOnClickListener(v -> {
            markCompleted();
            startActivity(QueuerHomeActivity.intent(this, QueuerHomeActivity.TAB_BROWSE));
            finish();
        });
    }

    private void open(Class<?> destination) {
        markCompleted();
        startActivity(new Intent(this, destination).putExtra("from_welcome", true));
    }

    private void markCompleted() {
        getSharedPreferences("quapp_onboarding", MODE_PRIVATE).edit().putBoolean("completed", true).apply();
    }
}

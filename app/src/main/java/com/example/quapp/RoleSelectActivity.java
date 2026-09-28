package com.example.quapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class RoleSelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);
        SystemBars.applyPadding(findViewById(R.id.role_root));

        MaterialCardView queuerCard = findViewById(R.id.role_queuer_card);
        MaterialCardView ownerCard = findViewById(R.id.role_owner_card);

        queuerCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseRole(Session.Role.QUEUER);
            }
        });

        ownerCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseRole(Session.Role.OWNER);
            }
        });
    }

    /** Remembered, so next launch goes straight to this side. Switching lives in Profile. */
    private void chooseRole(Session.Role role) {
        Session session = new Session(this);
        session.setRole(role);
        startActivity(session.homeIntent(this));
    }
}
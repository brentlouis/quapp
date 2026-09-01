package com.example.quapp;

import android.content.Intent;
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
                startActivity(new Intent(RoleSelectActivity.this, BrowseActivity.class));
            }
        });

        ownerCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RoleSelectActivity.this, OwnerDashboardActivity.class));
            }
        });
    }
}
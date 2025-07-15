package com.henry.tripcraft.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.henry.tripcraft.R;
import com.henry.tripcraft.utils.UserUtils;

public class MainActivity extends AppCompatActivity {

    private TextView greetingText;
    private CardView CreateFirstPlan;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        greetingText = findViewById(R.id.greetingText);
        CreateFirstPlan = findViewById(R.id.createPlanButton);

        setUsername();

        CreateFirstPlan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CityActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void setUsername() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String displayName = UserUtils.getDisplayName(this, currentUser);
            greetingText.setText(getString(R.string.hello_username, displayName));
        } else {
            greetingText.setText(getString(R.string.hello_username, "Guest"));
        }

    }
}
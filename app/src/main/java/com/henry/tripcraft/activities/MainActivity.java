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

public class MainActivity extends AppCompatActivity {

    private TextView greetingText;
    private TextView subtitleText;
    private CardView emptyStateCard;
    private CardView tipsCard;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        greetingText = findViewById(R.id.greetingText);
        subtitleText = findViewById(R.id.subtitleText);
        emptyStateCard = findViewById(R.id.emptyStateCard);

        // Set username
        setUsername();

        // Setup click listener for the "Make your first plan" button
        emptyStateCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CityActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Redirect to login if not authenticated
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setUsername() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Try SharedPreferences first
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String savedName = prefs.getString("displayName", null);

            String displayName = "Guest";

            if (savedName != null && !savedName.isEmpty()) {
                displayName = savedName;
            } else if (currentUser.getDisplayName() != null &&
                    !currentUser.getDisplayName().isEmpty() &&
                    !currentUser.getDisplayName().equals("User Name")) {
                // Use Firebase name if it's not the default "User Name"
                displayName = currentUser.getDisplayName();
            } else {
                // Fall back to email username
                String email = currentUser.getEmail();
                if (email != null && !email.isEmpty()) {
                    displayName = email.split("@")[0];
                }
            }

            greetingText.setText("Hello, " + displayName + "!");
        } else {
            greetingText.setText("Hello, Guest!");
        }
    }
}
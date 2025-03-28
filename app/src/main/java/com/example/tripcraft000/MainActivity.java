package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Button generateNewPlanButton;
    private Button savedPlansButton;
    private TextView usernameText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        usernameText = findViewById(R.id.usernameText);
        generateNewPlanButton = findViewById(R.id.generateNewPlanButton);
        savedPlansButton = findViewById(R.id.savedPlansButton);

        setUsername();

        View profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        generateNewPlanButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CityActivity.class);
            startActivityForResult(intent, 1);
        });

        savedPlansButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SavedPlansActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            Intent intent = new Intent(MainActivity.this, SavedPlansActivity.class);
            startActivity(intent);
        }
    }

    private void setUsername() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String username = currentUser.getDisplayName();
            if (username != null && !username.isEmpty()) {
                usernameText.setText(username);
            } else {
                String email = currentUser.getEmail();
                if (email != null && !email.isEmpty()) {
                    usernameText.setText(email.split("@")[0]);
                } else {
                    usernameText.setText("Guest");
                }
            }

            // For debugging
            System.out.println("Current display name: " + username);
        } else {
            usernameText.setText("Guest");
        }
    }
}

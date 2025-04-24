package com.example.tripcraft000;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.app.ActivityOptions;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private FloatingActionButton generateNewPlanButton;
    private TextView usernameText;
    private TextView sloganText;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        usernameText = findViewById(R.id.usernameText);
        sloganText = findViewById(R.id.sloganText);
        generateNewPlanButton = findViewById(R.id.generateNewPlanButton);
        View yourPlansButton = findViewById(R.id.yourPlansButton);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Set username from Firebase or SharedPreferences
        setUsername();

        // Setup bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Hide center item to make room for FAB
        bottomNavigationView.getMenu().findItem(R.id.navigation_placeholder).setEnabled(false);

        // Setup click listeners
        generateNewPlanButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CityActivity.class);

            // Create the zoom animation using ActivityOptions
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    MainActivity.this,
                    Pair.create(generateNewPlanButton, "transitionButton")
            );

            startActivityForResult(intent, 1, options.toBundle());
        });

        yourPlansButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SavedPlansActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.navigation_home) {
            // Already on home, do nothing or refresh
            return true;
        } else if (id == R.id.profileButton) {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }

        return false;
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
            // Try SharedPreferences first
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String savedName = prefs.getString("displayName", null);

            System.out.println("USERNAME DEBUG: SharedPrefs name: " + savedName);
            System.out.println("USERNAME DEBUG: Firebase name: " + currentUser.getDisplayName());

            if (savedName != null && !savedName.isEmpty()) {
                usernameText.setText(savedName);
            } else if (currentUser.getDisplayName() != null &&
                    !currentUser.getDisplayName().isEmpty() &&
                    !currentUser.getDisplayName().equals("User Name")) {
                // Use Firebase name if it's not the default "User Name"
                usernameText.setText(currentUser.getDisplayName());
            } else {
                // Fall back to email username
                String email = currentUser.getEmail();
                if (email != null && !email.isEmpty()) {
                    usernameText.setText(email.split("@")[0]);
                } else {
                    usernameText.setText("Guest");
                }
            }
        } else {
            usernameText.setText("Guest");
        }
    }
}
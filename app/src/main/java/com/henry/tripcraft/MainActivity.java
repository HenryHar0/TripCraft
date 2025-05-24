package com.henry.tripcraft;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

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
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Set username from Firebase or SharedPreferences
        setUsername();

        // Setup bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Set home as selected by default
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.navigation_home) {
            // Already on home, do nothing or refresh
            return true;
        } else if (id == R.id.navigation_saved_plan) {
            // Navigate to SavedPlansActivity (formerly yourPlansButton functionality)
            Intent intent = new Intent(MainActivity.this, SavedPlansActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.navigation_plus) {
            // Navigate to CityActivity (formerly generateNewPlanButton functionality)
            Intent intent = new Intent(MainActivity.this, CityActivity.class);
            startActivityForResult(intent, 1);
            return true;
        } else if (id == R.id.navigation_ai) {
            // Navigate to RestaurantActivity
            Intent intent = new Intent(MainActivity.this, RestaurantActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.profileButton) {
            // Navigate to ProfileActivity
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
    protected void onResume() {
        super.onResume();
        // Ensure home is selected when returning to MainActivity
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
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
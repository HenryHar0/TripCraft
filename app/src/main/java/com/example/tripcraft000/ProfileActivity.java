package com.example.tripcraft000;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        profileImage = findViewById(R.id.profileImage);
        TextView nicknameText = findViewById(R.id.nicknameText);
        TextView emailText = findViewById(R.id.emailText);

        setUserProfile(currentUser, nicknameText, emailText);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.logoutButton).setOnClickListener(v -> showLogoutConfirmation());
    }

    private void setUserProfile(FirebaseUser currentUser, TextView nicknameText, TextView emailText) {
        if (currentUser != null) {
            // Debug current Firebase values
            System.out.println("PROFILE DEBUG: Current User UID: " + currentUser.getUid());
            System.out.println("PROFILE DEBUG: Current User Email: " + currentUser.getEmail());
            System.out.println("PROFILE DEBUG: Current Display Name: " + currentUser.getDisplayName());

            // Try SharedPreferences first
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String savedName = prefs.getString("displayName", null);

            System.out.println("PROFILE DEBUG: SharedPrefs name: " + savedName);

            String email = currentUser.getEmail() != null ? currentUser.getEmail() : "No Email";
            emailText.setText("Email: " + email);

            // First try to use the saved name from SharedPreferences
            if (savedName != null && !savedName.isEmpty()) {
                nicknameText.setText("Nickname: " + savedName);
                System.out.println("PROFILE DEBUG: Using SharedPrefs name");
            }
            // Then try Firebase name if it's not the default "User Name"
            else if (currentUser.getDisplayName() != null &&
                    !currentUser.getDisplayName().isEmpty() &&
                    !currentUser.getDisplayName().equals("User Name")) {
                nicknameText.setText("Nickname: " + currentUser.getDisplayName());
                System.out.println("PROFILE DEBUG: Using Firebase name");
            }
            // Fall back to email username
            else if (email != null && !email.equals("No Email")) {
                String usernameFromEmail = email.split("@")[0];
                nicknameText.setText("Nickname: " + usernameFromEmail);
                System.out.println("PROFILE DEBUG: Using email-derived name");
            }
            // Last resort
            else {
                nicknameText.setText("Nickname: Guest");
                System.out.println("PROFILE DEBUG: Using Guest name");
            }

            // Still reload the user data to keep it fresh for next time
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser refreshedUser = mAuth.getCurrentUser();
                    System.out.println("PROFILE DEBUG: After reload - Display name: " +
                            (refreshedUser != null ? refreshedUser.getDisplayName() : "null user"));
                }
            });
        } else {
            nicknameText.setText("Nickname: Guest");
            emailText.setText("Email: N/A");
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to log out?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(ProfileActivity.this, SignUpActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
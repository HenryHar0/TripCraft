package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class VerifyEmailActivity extends AppCompatActivity {

    private Button buttonProceed;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        setContentView(R.layout.activity_verify_email);

        buttonProceed = findViewById(R.id.buttonProceed);

        buttonProceed.setOnClickListener(v -> checkEmailVerification());
    }

    private void checkEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        String name = "User Name";

                        updateUserProfile(user, name);
                    } else {
                        // If email is not verified, show a message and stay on the page
                        Toast.makeText(VerifyEmailActivity.this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(VerifyEmailActivity.this, "Failed to reload user information.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateUserProfile(FirebaseUser user, String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name).build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(VerifyEmailActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(VerifyEmailActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(VerifyEmailActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}




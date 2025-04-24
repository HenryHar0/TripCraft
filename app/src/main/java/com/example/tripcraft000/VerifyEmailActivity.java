package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class VerifyEmailActivity extends AppCompatActivity {

    private Button buttonOpenEmail;
    private TextView textViewResend;
    private FirebaseAuth mAuth;
    private boolean isCheckingVerification = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        setContentView(R.layout.activity_verify_email);

        buttonOpenEmail = findViewById(R.id.buttonOpenEmail);
        textViewResend = findViewById(R.id.textViewResend);

        // Open email app when button is clicked
        buttonOpenEmail.setOnClickListener(v -> openEmailApp());

        // Set up resend verification email functionality
        textViewResend.setOnClickListener(v -> resendVerificationEmail());

        // Start continuous verification check
        startVerificationCheck();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart verification check when app comes back to foreground
        startVerificationCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop checking when app goes to background
        isCheckingVerification = false;
    }

    private void startVerificationCheck() {
        if (!isCheckingVerification) {
            isCheckingVerification = true;
            // Check immediately once
            checkEmailVerification();

            // Then set up continuous checking
            new Thread(() -> {
                while (isCheckingVerification) {
                    try {
                        // Check every 3 seconds
                        Thread.sleep(3000);

                        // Make sure we're still checking
                        if (isCheckingVerification) {
                            runOnUiThread(this::checkEmailVerification);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void openEmailApp() {
        // Intent to open the email app
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "No email app found. Please check your email manually.", Toast.LENGTH_LONG).show();
        }
    }

    private void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(VerifyEmailActivity.this, "Verification email sent again!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(VerifyEmailActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void checkEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Get fresh instance after reload
                    FirebaseUser refreshedUser = mAuth.getCurrentUser();
                    if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                        // Stop checking once verified
                        isCheckingVerification = false;

                        // Get existing name or use email username
                        String name = refreshedUser.getDisplayName();
                        if (name == null || name.isEmpty()) {
                            String email = refreshedUser.getEmail();
                            name = email != null ? email.split("@")[0] : "User";
                        }

                        updateUserProfile(refreshedUser, name);
                    }
                    // If not verified yet, we'll check again in 3 seconds
                }
            });
        } else {
            // User is not logged in, stop checking
            isCheckingVerification = false;
        }
    }

    private void updateUserProfile(FirebaseUser user, String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name).build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(VerifyEmailActivity.this, "Email verified! Redirecting...", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(VerifyEmailActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(VerifyEmailActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
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

        // Check if email verification has been completed
        checkEmailVerification();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check verification status when the app resumes
        checkEmailVerification();
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
                    if (user.isEmailVerified()) {
                        String name = "User Name";
                        updateUserProfile(user, name);
                    }
                    // If not verified, stay on the page and wait
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
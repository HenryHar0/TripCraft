package com.henry.tripcraft.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.henry.tripcraft.R;

public class VerifyEmailActivity extends AppCompatActivity {

    private TextView textViewResend, textViewWaiting;
    private MaterialButton buttonBack;
    private ImageView imageViewEmail;
    private CardView cardViewStatus;
    private FirebaseAuth mAuth;
    private boolean isCheckingVerification = false;
    private Animation pulseAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        setContentView(R.layout.activity_verify_email);

        // Initialize views
        imageViewEmail = findViewById(R.id.imageViewEmail);
        textViewResend = findViewById(R.id.textViewResend);
        textViewWaiting = findViewById(R.id.textViewWaiting);
        buttonBack = findViewById(R.id.buttonBack);
        cardViewStatus = findViewById(R.id.cardViewStatus);

        // Set up pulse animation for the waiting text
        setupPulseAnimation();

        // Set up resend verification email functionality
        textViewResend.setOnClickListener(v -> resendVerificationEmail());

        // Set up back button to return to SignupActivity
        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(VerifyEmailActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

        // Start continuous verification check
        startVerificationCheck();
    }

    private void setupPulseAnimation() {
        pulseAnimation = new AlphaAnimation(0.5f, 1.0f);
        pulseAnimation.setDuration(1000);
        pulseAnimation.setRepeatMode(Animation.REVERSE);
        pulseAnimation.setRepeatCount(Animation.INFINITE);
        textViewWaiting.startAnimation(pulseAnimation);
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

    private void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Show loading state
            textViewResend.setEnabled(false);
            textViewResend.setText(R.string.sending);

            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Show success animation
                            showSuccessAnimation();
                            Toast.makeText(VerifyEmailActivity.this, R.string.verification_email_sent, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(VerifyEmailActivity.this, R.string.failed_to_send_email, Toast.LENGTH_SHORT).show();
                        }
                        // Reset button text
                        textViewResend.setText(R.string.resend_verification_email);
                        textViewResend.setEnabled(true);
                    });
        }
    }

    private void showSuccessAnimation() {
        // Flash the card view green briefly to indicate success
        AlphaAnimation flashAnimation = new AlphaAnimation(0.0f, 1.0f);
        flashAnimation.setDuration(300);
        flashAnimation.setRepeatMode(Animation.REVERSE);
        flashAnimation.setRepeatCount(1);

        cardViewStatus.setCardBackgroundColor(getResources().getColor(R.color.success, null));
        cardViewStatus.startAnimation(flashAnimation);

        flashAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                cardViewStatus.setCardBackgroundColor(getResources().getColor(R.color.background, null));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
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
                        textViewWaiting.clearAnimation();

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
            textViewWaiting.clearAnimation();
        }
    }

    private void updateUserProfile(FirebaseUser user, String name) {
        // Show verification success UI changes
        imageViewEmail.setImageResource(R.drawable.ic_check);
        imageViewEmail.setColorFilter(getResources().getColor(R.color.success, null));
        textViewWaiting.setText(R.string.email_verified);
        textViewWaiting.setTextColor(getResources().getColor(R.color.success, null));

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name).build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Show success feedback with animation
                cardViewStatus.setCardBackgroundColor(getResources().getColor(R.color.success, null));

                Toast.makeText(VerifyEmailActivity.this, R.string.email_verified_redirecting, Toast.LENGTH_SHORT).show();

                // Add a slight delay before redirecting for better UX
                cardViewStatus.postDelayed(() -> {
                    Intent intent = new Intent(VerifyEmailActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }, 1500);
            } else {
                Toast.makeText(VerifyEmailActivity.this, R.string.failed_to_update_profile, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private MaterialButton buttonSignUp, buttonGoogleSignUp;
    private TextView textViewLoginTopRight;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_signup);

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        textViewLoginTopRight = findViewById(R.id.textViewLoginTopRight);
        buttonGoogleSignUp = findViewById(R.id.buttonGoogleSignUp);

        buttonSignUp.setOnClickListener(v -> signUp());
        textViewLoginTopRight.setOnClickListener(v -> login());
        buttonGoogleSignUp.setOnClickListener(v -> googleSignUp());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signUp() {
        final String[] name = {editTextName.getText().toString().trim()};
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Basic validation...

        // Show loading state if needed

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            System.out.println("SIGNUP DEBUG: User created successfully with email: " + email);
                            System.out.println("SIGNUP DEBUG: Attempting to set display name to: " + name[0]);

                            // IMPORTANT: Make sure name is not empty or null
                            if (name[0] == null || name[0].isEmpty()) {
                                System.out.println("SIGNUP DEBUG: WARNING! Name is empty or null!");
                                // Use a fallback name if necessary
                                name[0] = email.split("@")[0]; // Use part of email as name
                            }

                            // Store final name for use in lambda
                            final String finalName = name[0];

                            // Create profile update request
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(finalName)
                                    .build();

                            // Update the profile
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            System.out.println("SIGNUP DEBUG: Profile update task completed successfully");

                                            // Add a short delay before checking
                                            new android.os.Handler().postDelayed(() -> {
                                                // Get a fresh instance of the user
                                                FirebaseUser updatedUser = FirebaseAuth.getInstance().getCurrentUser();
                                                if (updatedUser != null) {
                                                    System.out.println("SIGNUP DEBUG: After delay, display name is: " + updatedUser.getDisplayName());

                                                    // Also save to SharedPreferences as a backup
                                                    getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                                            .edit()
                                                            .putString("displayName", finalName)
                                                            .apply();

                                                    // Now continue with email verification
                                                    sendVerificationEmail(updatedUser);
                                                }
                                            }, 2000); // 2 second delay
                                        } else {
                                            System.out.println("SIGNUP DEBUG: Profile update FAILED: " +
                                                    (profileTask.getException() != null ?
                                                            profileTask.getException().getMessage() : "No error message"));

                                            // Save to SharedPreferences anyway as fallback
                                            getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                                    .edit()
                                                    .putString("displayName", finalName)
                                                    .apply();

                                            // Continue with email verification despite profile update failure
                                            sendVerificationEmail(user);
                                        }
                                    });
                        }
                    } else {
                        // Handle account creation failure...
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(emailTask -> {
            // Hide progress indicator
            // progressBar.setVisibility(View.GONE);

            if (emailTask.isSuccessful()) {
                Toast.makeText(SignUpActivity.this,
                        "Verification email sent to " + user.getEmail(),
                        Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(SignUpActivity.this, VerifyEmailActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(SignUpActivity.this,
                        "Failed to send verification email: " + emailTask.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void login() {
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    private void googleSignUp() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(acct.getDisplayName())
                                    .build();
                            user.updateProfile(profileUpdates);

                            Toast.makeText(SignUpActivity.this, "Google sign-in successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
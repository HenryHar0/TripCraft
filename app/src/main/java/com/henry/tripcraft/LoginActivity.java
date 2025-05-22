package com.henry.tripcraft;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private MaterialButton buttonLogin, buttonGoogleLogin;
    private TextView textViewSignUpTopRight, textViewForgotPassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;

    private MaterialButton buttonTestUser;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Initialize UI components
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin);
        textViewSignUpTopRight = findViewById(R.id.textViewSignUpTopRight);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        buttonTestUser = findViewById(R.id.buttonTestUser);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set click listeners
        buttonLogin.setOnClickListener(v -> loginUser());
        textViewSignUpTopRight.setOnClickListener(v -> goToSignUp());
        textViewForgotPassword.setOnClickListener(v -> handleForgotPassword());
        buttonGoogleLogin.setOnClickListener(v -> handleGoogleSignIn());

        // Add click listener for the test user button
        buttonTestUser.setOnClickListener(v -> loginWithTestUser());
    }

    private void loginWithTestUser() {
        // Use predefined test user credentials
        String testEmail = "individualproject2025@gmail.com";
        String testPassword = "Samsung2025";

        // Display the credentials in the input fields
        editTextEmail.setText(testEmail);
        editTextPassword.setText(testPassword);

        // Login with the test credentials
        loginWithEmail(testEmail, testPassword);
    }

    private void loginUser() {
        String emailOrUsername = editTextEmail.getText().toString().trim().toLowerCase();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(emailOrUsername) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // First check if input is email (contains @) or username
        if (emailOrUsername.contains("@")) {
            // It's an email, login directly
            loginWithEmail(emailOrUsername, password);
        } else {
            // It's a username, look up the email
            findEmailByUsername(emailOrUsername, password);
        }
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Please verify your email first", Toast.LENGTH_SHORT).show();
                                // Optionally send to verification screen
                                Intent intent = new Intent(LoginActivity.this, VerifyEmailActivity.class);
                                startActivity(intent);
                            }
                        }
                    } else {
                        Exception exception = task.getException();
                        Log.e("LoginActivity", "Authentication failed", exception);
                        Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                        (exception != null ? exception.getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }

                });
    }

    private void findEmailByUsername(String username, String password) {
        mDatabase.child("usernames").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String userId = dataSnapshot.getValue(String.class);
                    if (userId != null) {
                        // Get the user's email
                        mDatabase.child("users").child(userId).child("email").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot emailSnapshot) {
                                if (emailSnapshot.exists()) {
                                    String email = emailSnapshot.getValue(String.class);
                                    if (email != null) {
                                        // Now login with email
                                        loginWithEmail(email, password);
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Account error. Please contact support.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "Account error. Please contact support.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToSignUp() {
        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        startActivity(intent);
    }

    private void handleForgotPassword() {
        String emailOrUsername = editTextEmail.getText().toString().trim().toLowerCase();

        if (TextUtils.isEmpty(emailOrUsername)) {
            Toast.makeText(this, "Please enter your email or username", Toast.LENGTH_SHORT).show();
            return;
        }

        // If input is a username, find associated email first
        if (!emailOrUsername.contains("@")) {
            mDatabase.child("usernames").child(emailOrUsername).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String userId = dataSnapshot.getValue(String.class);
                        if (userId != null) {
                            mDatabase.child("users").child(userId).child("email").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot emailSnapshot) {
                                    if (emailSnapshot.exists()) {
                                        String email = emailSnapshot.getValue(String.class);
                                        if (email != null) {
                                            sendPasswordResetEmail(email);
                                        }
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Could not find email for this username", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(LoginActivity.this, "Database error", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(LoginActivity.this, "Database error", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Input is an email, send reset email directly
            sendPasswordResetEmail(emailOrUsername);
        }
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent to " + email, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to send reset email: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleGoogleSignIn() {
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
                            String userId = user.getUid();
                            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()) {
                                        // New Google sign-in, redirect to SignUpActivity for completing profile
                                        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        // Existing user, go to MainActivity
                                        Toast.makeText(LoginActivity.this, "Google sign-in successful", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(LoginActivity.this, "Database error", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
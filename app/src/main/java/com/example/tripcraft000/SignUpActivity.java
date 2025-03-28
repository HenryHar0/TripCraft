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
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Set the display name for the user
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            // Now send the verification email after the profile has been updated
                                            user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                                if (emailTask.isSuccessful()) {
                                                    Toast.makeText(SignUpActivity.this,
                                                            "Verification email sent to " + user.getEmail(),
                                                            Toast.LENGTH_SHORT).show();

                                                    Intent intent = new Intent(SignUpActivity.this, VerifyEmailActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    Toast.makeText(SignUpActivity.this,
                                                            "Failed to send verification email.",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            Toast.makeText(SignUpActivity.this,
                                                    "Failed to save username.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(SignUpActivity.this,
                                "Sign up failed. Please try again.",
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
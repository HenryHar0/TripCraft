package com.henry.tripcraft;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private TextInputEditText etFullName, etUsername, etEmail, etPassword, etConfirm;
    private MaterialButton btnSignUp, btnGoogle;
    private FirebaseAuth auth;
    private DatabaseReference db;
    private GoogleSignInClient googleClient;
    private static final int RC_GOOGLE = 9001;

    private static final String TAG = "SignUpActivity"; // Added tag for logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            Log.d(TAG, "User is already verified. Redirecting to MainActivity.");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_signup);
        bindViews();
        setupListeners();
        initGoogleSignIn();
    }

    private void bindViews() {
        Log.d(TAG, "Binding views.");
        etFullName = findViewById(R.id.editTextFullName);
        etUsername = findViewById(R.id.editTextName);
        etEmail = findViewById(R.id.editTextEmail);
        etPassword = findViewById(R.id.editTextPassword);
        etConfirm = findViewById(R.id.editTextConfirmPassword);
        btnSignUp = findViewById(R.id.buttonSignUp);
        btnGoogle = findViewById(R.id.buttonGoogleSignUp);
    }

    private void setupListeners() {
        Log.d(TAG, "Setting up listeners.");
        btnSignUp.setOnClickListener(v -> handleSignUp());
        btnGoogle.setOnClickListener(v -> startActivityForResult(
                googleClient.getSignInIntent(), RC_GOOGLE));
        findViewById(R.id.textViewLoginTopRight)
                .setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

    private void initGoogleSignIn() {
        Log.d(TAG, "Initializing Google Sign-In.");
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, options);
    }

    private void handleSignUp() {
        Log.d(TAG, "Handling sign up.");
        String name = etFullName.getText().toString().trim();
        String user = etUsername.getText().toString().trim().toLowerCase();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String conf = etConfirm.getText().toString();

        if (!validateInputs(name, user, email, pass, conf)) {
            Toast.makeText(this, "Validation failed", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Checking if username exists in database.");
        db.child("usernames").child(user)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        Log.d(TAG, "onDataChange: " + snap.exists());
                        if (snap.exists()) {
                            etUsername.setError("Username already taken");
                            etUsername.requestFocus();
                        } else {
                            createUser(name, user, email, pass);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                        Toast.makeText(SignUpActivity.this,
                                error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs(String name, String user, String email,
                                   String pass, String conf) {
        Log.d(TAG, "Validating inputs.");
        if (TextUtils.isEmpty(name)) return setError(etFullName, "Full name is required");
        if (TextUtils.isEmpty(user)) return setError(etUsername, "Username is required");
        if (TextUtils.isEmpty(email)) return setError(etEmail, "Email is required");
        if (TextUtils.isEmpty(pass)) return setError(etPassword, "Password is required");
        if (pass.length() < 6) return setError(etPassword, "Password must be ≥6 chars");
        if (!pass.equals(conf)) return setError(etConfirm, "Passwords do not match");
        return true;
    }

    private boolean setError(TextInputEditText et, String msg) {
        Log.d(TAG, "Error: " + msg);
        et.setError(msg);
        et.requestFocus();
        return false;
    }

    private void createUser(String name, String user, String email, String pass) {
        Log.d(TAG, "createUser called with email: " + email);
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    Log.d(TAG, "Auth complete: " + task.isSuccessful());
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Auth failed: " + task.getException().getMessage());
                        Toast.makeText(this,
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    FirebaseUser u = auth.getCurrentUser();
                    if (u == null) return;
                    updateUserProfile(u, name, () -> saveUserData(u, name, user, email));
                });
    }

    private void updateUserProfile(FirebaseUser u, String name, Runnable onSuccess) {
        Log.d(TAG, "Updating user profile.");
        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                .setDisplayName(name).build();
        u.updateProfile(req).addOnCompleteListener(t -> {
            Log.d(TAG, "Profile update completed.");
            onSuccess.run();
        });
    }

    private void saveUserData(FirebaseUser u, String name,
                              String user, String email) {
        Log.d(TAG, "Saving user data.");
        String uid = u.getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", name);
        data.put("username", user);
        data.put("email", email);

        db.child("users").child(uid).setValue(data);
        db.child("usernames").child(user).setValue(uid);
        db.child("emailToUsername")
                .child(email.replace(".", ",")).setValue(user);

        sendVerification(u);
    }

    private void sendVerification(FirebaseUser u) {
        Log.d(TAG, "Sending email verification.");
        u.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Verification email sent.");
                        Toast.makeText(this,
                                "Verification email sent to " + u.getEmail(),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Failed to send verification email: " + task.getException().getMessage());
                        Toast.makeText(this,
                                "Failed to send email: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    startActivity(new Intent(SignUpActivity.this, VerifyEmailActivity.class));
                    finish();
                });
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_GOOGLE) return;
        try {
            Log.d(TAG, "Handling Google sign-in result.");
            GoogleSignInAccount acc = GoogleSignIn
                    .getSignedInAccountFromIntent(data)
                    .getResult(ApiException.class);
            AuthCredential cred = GoogleAuthProvider
                    .getCredential(acc.getIdToken(), null);
            auth.signInWithCredential(cred)
                    .addOnCompleteListener(this, task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Google sign-in failed: " + task.getException().getMessage());
                            return;
                        }
                        FirebaseUser u = auth.getCurrentUser();
                        if (u == null) return;
                        String email = u.getEmail();
                        String base = email != null ?
                                email.split("@")[0] : "user" + System.currentTimeMillis();
                        resolveGoogleUsername(base, acc.getDisplayName(), email, u);
                    });
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in failed: " + e.getMessage());
            Toast.makeText(this,
                    "Google sign in failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void resolveGoogleUsername(String base, String name,
                                       String email, FirebaseUser u) {
        Log.d(TAG, "Resolving Google username.");
        db.child("usernames").child(base)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        String finalUser = snap.exists() ?
                                base + System.currentTimeMillis() : base;
                        updateUserProfile(u, name,
                                () -> saveUserData(u, name, finalUser, email));
                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                    }
                });
    }
}

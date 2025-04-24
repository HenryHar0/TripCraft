package com.example.tripcraft000;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // UI components
    private ImageView profileImage;
    private TextView profileName;
    private TextView profileUsername;
    private TextView usernameValue;
    private TextView bioValue;
    private TextView emailValue;
    private MaterialButton editProfileButton;
    private ImageButton settingsButton;
    private Toolbar toolbar;

    // Firebase components
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference usersRef;
    private StorageReference storageReference;
    private FirebaseUser currentUser;

    // For image selection
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        storageReference = FirebaseStorage.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI
        initializeViews();
        setupToolbar();
        setupButtons();
        setupImagePicker();

        // Load user data
        loadUserData();
    }

    private void initializeViews() {
        profileImage    = findViewById(R.id.profileImage);
        profileName     = findViewById(R.id.profileName);
        profileUsername = findViewById(R.id.profileUsername);
        usernameValue   = findViewById(R.id.usernameValue);
        bioValue        = findViewById(R.id.bioValue);
        emailValue      = findViewById(R.id.emailValue);
        editProfileButton = findViewById(R.id.editProfileButton);
        settingsButton    = findViewById(R.id.settingsButton);
        toolbar           = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupButtons() {
        editProfileButton.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class))
        );

        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, SettingsActivity.class))
        );

        profileImage.setOnClickListener(v -> openImagePicker());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .circleCrop()
                                    .into(profileImage);
                            uploadImageToFirebase();
                        }
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Image"));
    }

    private void uploadImageToFirebase() {
        if (currentUser == null || selectedImageUri == null) return;

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        StorageReference profileImagesRef =
                storageReference.child("profile_images/" + currentUser.getUid() + ".jpg");

        profileImagesRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        profileImagesRef.getDownloadUrl().addOnSuccessListener(uri ->
                                updateProfileImageUrl(uri.toString())
                        )
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateProfileImageUrl(String imageUrl) {
        if (currentUser == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", imageUrl);

        usersRef.child(currentUser.getUid())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadUserData() {
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Email from Auth
        emailValue.setText(currentUser.getEmail());

        // Listen for Realtime Database changes
        usersRef.child(currentUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snap) {
                        if (!snap.exists()) {
                            createUserDocument();
                            return;
                        }

                        String fullName = snap.child("fullName").getValue(String.class);
                        profileName.setText(fullName != null ? fullName : "-");

                        // existing fields
                        String name = snap.child("name").getValue(String.class);
                        String username = snap.child("username").getValue(String.class);
                        String bio = snap.child("bio").getValue(String.class);
                        String profileImageUrl = snap.child("profileImageUrl").getValue(String.class);

                        profileUsername.setText(username != null ? "@" + username : "-");
                        usernameValue.setText(username != null ? username : "-");
                        bioValue.setText(bio != null ? bio : "No bio yet.");

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(ProfileActivity.this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(profileImage);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ProfileActivity.this,
                                "Failed to load user data: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserDocument() {
        if (currentUser == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", currentUser.getDisplayName() != null
                ? currentUser.getDisplayName() : "User");
        userData.put("fullName", currentUser.getDisplayName() != null
                ? currentUser.getDisplayName() : "User");  // <-- NEW
        userData.put("username", "user" + currentUser.getUid().substring(0, 5));
        userData.put("email", currentUser.getEmail());
        userData.put("bio", "");
        userData.put("profileImageUrl", "");
        userData.put("createdAt", System.currentTimeMillis());

        usersRef.child(currentUser.getUid())
                .setValue(userData)
                .addOnSuccessListener(aVoid -> loadUserData())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to create user profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }
}

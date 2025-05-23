package com.henry.tripcraft;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

public class ProfileActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    // UI components
    private ImageView profileImage;
    private TextView profileName;
    private TextView profileUsername;
    private TextView usernameValue;
    private TextView bioValue;
    private TextView emailValue;
    private ImageButton settingsButton;
    private Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;

    // Firebase components
    private FirebaseAuth mAuth;
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
        initializeFirebase();

        // Initialize UI
        initializeViews();
        setupToolbar();
        setupNavigationAndButtons();
        setupImagePicker();

        // Load user data
        loadUserData();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        storageReference = FirebaseStorage.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileUsername = findViewById(R.id.profileUsername);
        usernameValue = findViewById(R.id.usernameValue);
        bioValue = findViewById(R.id.bioValue);
        emailValue = findViewById(R.id.emailValue);
        settingsButton = findViewById(R.id.settingsButton);
        toolbar = findViewById(R.id.toolbar);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupNavigationAndButtons() {
        // Set up bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.profileButton);

        // Set up FAB



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
                            updateProfileImagePreview();
                            uploadImageToFirebase();
                        }
                    }
                }
        );
    }

    private void updateProfileImagePreview() {
        Glide.with(this)
                .load(selectedImageUri)
                .circleCrop()
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(profileImage);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Image"));
    }

    private void uploadImageToFirebase() {
        if (currentUser == null || selectedImageUri == null) return;

        showToast("Uploading image...");

        StorageReference profileImagesRef =
                storageReference.child("profile_images/" + currentUser.getUid() + ".jpg");

        profileImagesRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        profileImagesRef.getDownloadUrl().addOnSuccessListener(this::updateProfileImageUrl)
                )
                .addOnFailureListener(e ->
                        showToast("Upload failed: " + e.getMessage())
                );
    }

    private void updateProfileImageUrl(Uri uri) {
        updateProfileImageUrl(uri.toString());
    }

    private void updateProfileImageUrl(String imageUrl) {
        if (currentUser == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", imageUrl);

        usersRef.child(currentUser.getUid())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid ->
                        showToast("Profile image updated")
                )
                .addOnFailureListener(e ->
                        showToast("Failed to update profile: " + e.getMessage())
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

                        updateUIWithUserData(snap);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        showToast("Failed to load user data: " + error.getMessage());
                    }
                });
    }

    private void updateUIWithUserData(DataSnapshot snap) {
        String fullName = snap.child("fullName").getValue(String.class);
        String username = snap.child("username").getValue(String.class);
        String bio = snap.child("bio").getValue(String.class);
        String profileImageUrl = snap.child("profileImageUrl").getValue(String.class);

        // Update UI elements
        profileName.setText(fullName != null ? fullName : "-");
        profileUsername.setText(username != null ? "@" + username : "-");
        usernameValue.setText(username != null ? username : "-");
        bioValue.setText(bio != null && !bio.isEmpty() ? bio : "No bio yet.");

        // Load profile image
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(ProfileActivity.this)
                    .load(profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(profileImage);
        }
    }

    private void createUserDocument() {
        if (currentUser == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", currentUser.getDisplayName() != null
                ? currentUser.getDisplayName() : "User");
        userData.put("fullName", currentUser.getDisplayName() != null
                ? currentUser.getDisplayName() : "User");
        userData.put("username", "user" + currentUser.getUid().substring(0, 5));
        userData.put("email", currentUser.getEmail());
        userData.put("bio", "");
        userData.put("profileImageUrl", "");
        userData.put("createdAt", System.currentTimeMillis());

        usersRef.child(currentUser.getUid())
                .setValue(userData)
                .addOnSuccessListener(aVoid -> loadUserData())
                .addOnFailureListener(e ->
                        showToast("Failed to create user profile: " + e.getMessage())
                );
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.navigation_home) {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        }
        else if(itemId == R.id.navigation_plus) {
            startActivity(new Intent(ProfileActivity.this, CityActivity.class));
        }
        else if(itemId == R.id.navigation_saved_plan){
            startActivity(new Intent(this, SavedPlansActivity.class));
            return true;
        }else if (itemId == R.id.profileButton) {
            // Already on profile screen
            return true;
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();

        // Make sure navigation is correctly selected
        bottomNavigationView.setSelectedItemId(R.id.profileButton);
    }
}
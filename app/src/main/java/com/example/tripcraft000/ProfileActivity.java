package com.example.tripcraft000;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private SeekBar redSeekBar, greenSeekBar, blueSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        profileImage = findViewById(R.id.profileImage);
        TextView nicknameText = findViewById(R.id.nicknameText);
        TextView emailText = findViewById(R.id.emailText);

        if (currentUser != null) {
            String nickname = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "No Nickname";
            String email = currentUser.getEmail() != null ? currentUser.getEmail() : "No Email";

            nicknameText.setText("Nickname: " + nickname);
            emailText.setText("Email: " + email);
        }

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        findViewById(R.id.logoutButton).setOnClickListener(v -> showLogoutConfirmation());


    }
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to log out?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(ProfileActivity.this, SignUpActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}

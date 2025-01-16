package com.example.tripcraft000;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String nickname = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "No Nickname";
            String email = currentUser.getEmail() != null ? currentUser.getEmail() : "No Email";

            TextView nicknameText = findViewById(R.id.nicknameText);
            TextView emailText = findViewById(R.id.emailText);
            ImageView profileImage = findViewById(R.id.profileImage);

            nicknameText.setText("Nickname: " + nickname);
            emailText.setText("Email: " + email);
            profileImage.setImageResource(R.drawable.ic_default_pfp);
        }

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }
}

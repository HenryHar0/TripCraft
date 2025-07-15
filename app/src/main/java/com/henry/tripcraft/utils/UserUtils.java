package com.henry.tripcraft.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseUser;

public class UserUtils {
    public static String getDisplayName(Context context, FirebaseUser user) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String savedName = prefs.getString("displayName", null);

        if (savedName != null && !savedName.isEmpty()) {
            return savedName;
        } else if (user.getDisplayName() != null &&
                !user.getDisplayName().isEmpty() &&
                !user.getDisplayName().equals("User Name")) {
            return user.getDisplayName();
        } else {
            String email = user.getEmail();
            if (email != null && email.contains("@")) {
                return email.split("@")[0];
            }
        }
        return "Guest";
    }
}


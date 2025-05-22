package com.henry.tripcraft;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private TextView textCacheSize;
    private TextView textAppVersion;
    private TextView textCurrentLanguage;
    private TextView textCurrentCurrency;
    private SwitchCompat switchNotifications;
    private MaterialButton btnSignOut;

    private static final String PREFS_NAME = "TripCraftPrefs";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_CURRENCY = "app_currency";

    private static final String[] LANGUAGE_OPTIONS = {"English (US)", "Spanish", "French", "German", "Chinese", "Japanese"};
    private static final String[] LANGUAGE_CODES = {"en_US", "es", "fr", "de", "zh", "ja"};

    private static final String[] CURRENCY_OPTIONS = {"USD ($)", "EUR (€)", "GBP (£)", "JPY (¥)", "CNY (¥)", "INR (₹)"};
    private static final String[] CURRENCY_CODES = {"USD", "EUR", "GBP", "JPY", "CNY", "INR"};

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        initViews();
        setClickListeners();
        loadSettings();
    }

    private void initViews() {
        textCacheSize = findViewById(R.id.text_cache_size);
        textAppVersion = findViewById(R.id.text_app_version);
        textCurrentLanguage = findViewById(R.id.text_current_language);
        textCurrentCurrency = findViewById(R.id.text_current_currency);
        switchNotifications = findViewById(R.id.switch_notifications);
        btnSignOut = findViewById(R.id.btn_sign_out);
    }

    private void setClickListeners() {
        findViewById(R.id.setting_profile).setOnClickListener(v -> openEditProfile());
        findViewById(R.id.setting_password).setOnClickListener(v -> openChangePassword());
        findViewById(R.id.setting_notifications).setOnClickListener(v -> toggleNotifications());
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> saveNotificationPreference(isChecked));
        findViewById(R.id.setting_language).setOnClickListener(v -> openLanguageSettings());
        findViewById(R.id.setting_currency).setOnClickListener(v -> openCurrencySettings());
        findViewById(R.id.setting_data_usage).setOnClickListener(v -> openDataUsageSettings());
        findViewById(R.id.setting_cache).setOnClickListener(v -> clearCache());
        findViewById(R.id.setting_help).setOnClickListener(v -> openHelpCenter());
        findViewById(R.id.setting_feedback).setOnClickListener(v -> openFeedback());
        findViewById(R.id.setting_about).setOnClickListener(v -> openAbout());
        btnSignOut.setOnClickListener(v -> signOut());
    }

    private void loadSettings() {
        String versionName;
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            versionName = "1.0.0";
        }
        textAppVersion.setText("Version " + versionName);
        textCacheSize.setText(calculateCacheSize());

        String savedLanguageCode = sharedPreferences.getString(KEY_LANGUAGE, LANGUAGE_CODES[0]);
        int languageIndex = 0;
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(savedLanguageCode)) {
                languageIndex = i;
                break;
            }
        }
        textCurrentLanguage.setText(LANGUAGE_OPTIONS[languageIndex]);

        String savedCurrencyCode = sharedPreferences.getString(KEY_CURRENCY, CURRENCY_CODES[0]);
        int currencyIndex = 0;
        for (int i = 0; i < CURRENCY_CODES.length; i++) {
            if (CURRENCY_CODES[i].equals(savedCurrencyCode)) {
                currencyIndex = i;
                break;
            }
        }
        textCurrentCurrency.setText(CURRENCY_OPTIONS[currencyIndex]);

        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
        switchNotifications.setChecked(notificationsEnabled);
    }

    private String calculateCacheSize() {
        try {
            File cacheDir = getCacheDir();
            File appDir = new File(getApplicationInfo().dataDir);
            long size = getFolderSize(cacheDir) + getFolderSize(appDir) / 1024;

            if (size >= 1024) {
                float sizeInMB = size / 1024f;
                return new DecimalFormat("#.#").format(sizeInMB) + " MB";
            } else {
                return size + " KB";
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private long getFolderSize(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length() / 1024;
                    } else {
                        size += getFolderSize(file);
                    }
                }
            }
        }
        return size;
    }

    private void openEditProfile() {}

    private void openChangePassword() {}

    private void toggleNotifications() {
        switchNotifications.toggle();
    }

    private void saveNotificationPreference(boolean isEnabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_NOTIFICATIONS, isEnabled);
        editor.apply();
        Toast.makeText(this, "Notifications " + (isEnabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
    }

    private void openLanguageSettings() {
        String savedLanguageCode = sharedPreferences.getString(KEY_LANGUAGE, LANGUAGE_CODES[0]);
        int selectedIndex = 0;
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(savedLanguageCode)) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Language")
                .setSingleChoiceItems(LANGUAGE_OPTIONS, selectedIndex, (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_LANGUAGE, LANGUAGE_CODES[which]);
                    editor.apply();
                    textCurrentLanguage.setText(LANGUAGE_OPTIONS[which]);
                    setLocale(LANGUAGE_CODES[which]);
                    dialog.dismiss();
                    Toast.makeText(this, "Language changed to " + LANGUAGE_OPTIONS[which], Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode.split("_")[0]);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void openCurrencySettings() {
        String savedCurrencyCode = sharedPreferences.getString(KEY_CURRENCY, CURRENCY_CODES[0]);
        int selectedIndex = 0;
        for (int i = 0; i < CURRENCY_CODES.length; i++) {
            if (CURRENCY_CODES[i].equals(savedCurrencyCode)) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Currency")
                .setSingleChoiceItems(CURRENCY_OPTIONS, selectedIndex, (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_CURRENCY, CURRENCY_CODES[which]);
                    editor.apply();
                    textCurrentCurrency.setText(CURRENCY_OPTIONS[which]);
                    dialog.dismiss();
                    Toast.makeText(this, "Currency changed to " + CURRENCY_OPTIONS[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openDataUsageSettings() {}

    private void clearCache() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear Cache")
                .setMessage("Are you sure you want to clear the app cache? This will free up storage space but may slow down app loading temporarily.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    try {
                        File cacheDir = getCacheDir();
                        deleteDir(cacheDir);
                        textCacheSize.setText(calculateCacheSize());
                        Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to clear cache: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.exists()) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                if (children != null) {
                    for (String child : children) {
                        boolean success = deleteDir(new File(dir, child));
                        if (!success) return false;
                    }
                }
            }
            return dir.delete();
        }
        return false;
    }

    private void openHelpCenter() {}

    private void openFeedback() {}

    private void openAbout() {}

    private void signOut() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("user_id");
                    editor.remove("user_token");
                    editor.apply();
                    Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
        finish();
    }
}

package com.henry.tripcraft;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SavedPlansActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private RecyclerView savedPlansList;
    private List<SavedPlan> savedPlans;
    private SavedPlansAdapter adapter;
    private TripPlanStorageManager storageManager;
    private TripNotificationManager notificationManager;
    private BottomNavigationView bottomNavigationView;

    // Dialog views
    private View dialogContainer;
    private TextView tripInfoText;
    private RecyclerView tripPlanRecyclerView;
    private Button closeButton;
    private Button deleteButton;
    private Button shareButton;

    private static final String PREFS_NAME = "TripPlanPrefs";
    private static final String SLOT_NAMES_PREFS = "SlotNamesPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans_saved);

        // Initialize managers
        notificationManager = new TripNotificationManager(this);
        storageManager = new TripPlanStorageManager(this, notificationManager);

        savedPlansList = findViewById(R.id.savedPlansList);
        savedPlansList.setLayoutManager(new LinearLayoutManager(this));
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Initialize dialog views from included layout
        initializeDialogViews();

        savedPlans = new ArrayList<>();
        adapter = new SavedPlansAdapter(savedPlans, this::showPlanDetails, this::showRenameDialog);
        savedPlansList.setAdapter(adapter);

        loadSavedPlans();

        // Setup bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Set saved plans as selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_saved_plan);
    }

    private void initializeDialogViews() {
        // Get the included dialog layout
        dialogContainer = findViewById(R.id.customDialogContainer);

        if (dialogContainer != null) {
            tripInfoText = dialogContainer.findViewById(R.id.tripInfoText);
            tripPlanRecyclerView = dialogContainer.findViewById(R.id.tripPlanRecyclerView);
            closeButton = dialogContainer.findViewById(R.id.closeButton);
            deleteButton = dialogContainer.findViewById(R.id.deleteButton);
            shareButton = dialogContainer.findViewById(R.id.shareButton);

            // Initially hide the dialog
            dialogContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.navigation_home) {
            Intent intent = new Intent(SavedPlansActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.navigation_saved_plan) {
            // Already on saved plans, do nothing
            return true;
        } else if (id == R.id.navigation_plus) {
            Intent intent = new Intent(SavedPlansActivity.this, CityActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.navigation_ai) {
            Intent intent = new Intent(SavedPlansActivity.this, ChatActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.profileButton) {
            Intent intent = new Intent(SavedPlansActivity.this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }

        return false;
    }

    private void loadSavedPlans() {
        savedPlans.clear();

        for (int i = 0; i < 5; i++) {
            TripPlanStorageManager.TripPlan tripPlan = storageManager.getTripPlanFromSlot(i);

            SavedPlan plan = new SavedPlan();
            plan.setSlot(i);

            // Load custom slot name or use default
            String customName = getCustomSlotName(i);
            plan.setTitle(customName != null ? customName : "Slot " + (i + 1));

            if (tripPlan != null) {
                // Create a summary for display
                String summary = createPlanSummary(tripPlan);
                plan.setDetails(summary);
                plan.setEmpty(false);
                plan.setTripPlan(tripPlan);
            } else {
                // Check for legacy format
                String legacyPlan = storageManager.getPlanFromSlot(i);
                if (legacyPlan != null && !legacyPlan.equals("Empty Slot")) {
                    plan.setDetails(legacyPlan);
                    plan.setEmpty(false);
                } else {
                    plan.setDetails("Empty Slot");
                    plan.setEmpty(true);
                }
            }

            savedPlans.add(plan);
        }

        adapter.notifyDataSetChanged();
    }

    private String getCustomSlotName(int slot) {
        SharedPreferences prefs = getSharedPreferences(SLOT_NAMES_PREFS, MODE_PRIVATE);
        return prefs.getString("slot_name_" + slot, null);
    }

    private void saveCustomSlotName(int slot, String name) {
        SharedPreferences prefs = getSharedPreferences(SLOT_NAMES_PREFS, MODE_PRIVATE);
        if (name == null || name.trim().isEmpty() || name.equals("Slot " + (slot + 1))) {
            // Remove custom name if it's empty or default
            prefs.edit().remove("slot_name_" + slot).apply();
        } else {
            prefs.edit().putString("slot_name_" + slot, name.trim()).apply();
        }
    }

    private void showRenameDialog(int slot) {
        // Create custom dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename_slot, null);

        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        EditText nameInput = dialogView.findViewById(R.id.slotNameInput);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        titleText.setText("Rename Slot " + (slot + 1));

        // Set current name
        String currentName = savedPlans.get(slot).getTitle();
        if (currentName.startsWith("Slot ")) {
            nameInput.setHint("Enter custom name");
        } else {
            nameInput.setText(currentName);
            nameInput.setSelection(currentName.length());
        }

        AlertDialog dialog = builder.setView(dialogView).create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();

            if (newName.length() > 25) {
                nameInput.setError("Name too long (max 25 characters)");
                return;
            }

            saveCustomSlotName(slot, newName);
            loadSavedPlans(); // Refresh the list
            dialog.dismiss();

            String displayName = newName.isEmpty() ? "Slot " + (slot + 1) : newName;
            Toast.makeText(this, "Renamed to: " + displayName, Toast.LENGTH_SHORT).show();
        });

        // Show keyboard automatically
        nameInput.requestFocus();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
    }

    private String createPlanSummary(TripPlanStorageManager.TripPlan tripPlan) {
        StringBuilder summary = new StringBuilder();
        summary.append("Destination: ").append(tripPlan.destination).append("\n");
        summary.append("Duration: ").append(tripPlan.duration).append("\n");

        if (tripPlan.activitiesListData != null && !tripPlan.activitiesListData.isEmpty()) {
            int totalPlaces = 0;
            for (List<PlaceData> dayPlaces : tripPlan.activitiesListData) {
                totalPlaces += dayPlaces.size();
            }
            summary.append("Days: ").append(tripPlan.activitiesListData.size()).append("\n");
            summary.append("Total Places: ").append(totalPlaces);
        } else {
            summary.append("No activities planned");
        }

        return summary.toString();
    }

    private void showPlanDetails(int slot) {
        SavedPlan savedPlan = savedPlans.get(slot);

        if (savedPlan.isEmpty()) {
            Toast.makeText(SavedPlansActivity.this, "No plan saved in this slot.", Toast.LENGTH_SHORT).show();
            return;
        }

        // If it's a new format trip plan, show with DayByDayAdapter
        if (savedPlan.getTripPlan() != null) {
            showTripPlanWithAdapter(savedPlan.getTripPlan(), slot);
        } else {
            // Show legacy format
            showLegacyPlanDetails(savedPlan, slot);
        }
    }

    private void showTripPlanWithAdapter(TripPlanStorageManager.TripPlan tripPlan, final int slot) {
        if (dialogContainer == null) {
            Toast.makeText(this, "Dialog not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String tripInfo = "Destination: " + tripPlan.destination + "\n" +
                "Duration: " + tripPlan.duration + "\n" +
                "Weather: " + tripPlan.weather + "\n" +
                "Start Date: " + tripPlan.startDate;

        if (tripInfoText != null) {
            tripInfoText.setText(tripInfo);
        }

        if (tripPlan.activitiesListData != null && !tripPlan.activitiesListData.isEmpty() && tripPlanRecyclerView != null) {
            DayByDayAdapter adapter = new DayByDayAdapter(tripPlan.activitiesListData, tripPlan.apiKey);
            tripPlanRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            tripPlanRecyclerView.setAdapter(adapter);
        }

        // Set up button click listeners
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialogContainer.setVisibility(View.GONE));
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                dialogContainer.setVisibility(View.GONE);
                confirmDelete(slot);
            });
        }

        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                // Implement share functionality
                sharePlan(tripPlan);
            });
        }

        dialogContainer.setVisibility(View.VISIBLE);
    }

    private void sharePlan(TripPlanStorageManager.TripPlan tripPlan) {
        String shareText = "Check out my trip plan!\n\n" +
                "Destination: " + tripPlan.destination + "\n" +
                "Duration: " + tripPlan.duration + "\n" +
                "Start Date: " + tripPlan.startDate;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Trip Plan"));
    }

    private void showLegacyPlanDetails(SavedPlan savedPlan, int slot) {
        new AlertDialog.Builder(this)
                .setTitle("Plan Details - " + savedPlan.getTitle())
                .setMessage(savedPlan.getDetails())
                .setPositiveButton("Close", null)
                .setNegativeButton("Delete", (dialog, which) -> confirmDelete(slot))
                .show();
    }

    private void confirmDelete(int slot) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Plan")
                .setMessage("Are you sure you want to delete this plan?")
                .setPositiveButton("Yes", (dialog, which) -> deletePlan(slot))
                .setNegativeButton("No", null)
                .show();
    }

    private void deletePlan(int slot) {
        storageManager.deletePlanFromSlot(slot);

        // Update the saved plan in the list
        SavedPlan plan = savedPlans.get(slot);
        plan.setDetails("Empty Slot");
        plan.setEmpty(true);
        plan.setTripPlan(null);

        adapter.notifyItemChanged(slot);

        Toast.makeText(this, "Plan deleted from " + plan.getTitle(), Toast.LENGTH_SHORT).show();
    }

    public void savePlanToSlot(int slot) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedPlanKey = "PlanSlot_" + slot;

        if (sharedPreferences.contains(selectedPlanKey) && !savedPlans.get(slot).isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Overwrite Slot")
                    .setMessage("This slot already contains a plan. Do you want to overwrite it?")
                    .setPositiveButton("Yes", (dialog, which) -> performSave(slot))
                    .setNegativeButton("No", null)
                    .show();
        } else {
            performSave(slot);
        }
    }

    private void performSave(int slot) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedPlanKey = "PlanSlot_" + slot;

        // Create sample trip plan (in real usage, this would come from actual data)
        TripPlanStorageManager.TripPlan samplePlan = new TripPlanStorageManager.TripPlan();
        samplePlan.destination = "Sample Destination";
        samplePlan.duration = "3 Days";
        samplePlan.weather = "Sunny";
        samplePlan.city = "Sample City";
        samplePlan.startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Gson gson = new Gson();
        String planJson = gson.toJson(samplePlan);
        sharedPreferences.edit().putString(selectedPlanKey, planJson).apply();

        // Update the list
        loadSavedPlans();

        Toast.makeText(this, "Plan saved in " + savedPlans.get(slot).getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning to this activity
        loadSavedPlans();
        // Ensure saved plans is selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_saved_plan);
    }

    @Override
    public void onBackPressed() {
        // If dialog is visible, hide it instead of closing activity
        if (dialogContainer != null && dialogContainer.getVisibility() == View.VISIBLE) {
            dialogContainer.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    public static class SavedPlan {
        private int slot;
        private String title;
        private String details;
        private boolean isEmpty;
        private TripPlanStorageManager.TripPlan tripPlan;

        public int getSlot() {
            return slot;
        }

        public void setSlot(int slot) {
            this.slot = slot;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public void setEmpty(boolean empty) {
            isEmpty = empty;
        }

        public TripPlanStorageManager.TripPlan getTripPlan() {
            return tripPlan;
        }

        public void setTripPlan(TripPlanStorageManager.TripPlan tripPlan) {
            this.tripPlan = tripPlan;
        }
    }
}
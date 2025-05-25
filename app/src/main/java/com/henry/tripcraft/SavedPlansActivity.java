package com.henry.tripcraft;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class SavedPlansActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private RecyclerView savedPlansList;
    private List<SavedPlan> savedPlans;
    private SavedPlansAdapter adapter;
    private TripPlanStorageManager storageManager;
    private TripNotificationManager notificationManager;
    private BottomNavigationView bottomNavigationView;

    private static final String SLOT_NAMES_PREFS = "SlotNamesPrefs";

    // Add this as a class variable
    private AlertDialog currentTripDialog;

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

        savedPlans = new ArrayList<>();
        adapter = new SavedPlansAdapter(savedPlans, this::showPlanDetails, this::showRenameDialog);
        savedPlansList.setAdapter(adapter);

        // Setup bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Set saved plans as selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_saved_plan);

        // Load saved plans
        loadSavedPlans();
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
            Intent intent = new Intent(SavedPlansActivity.this, RestaurantActivity.class);
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to view saved plans", Toast.LENGTH_SHORT).show();
            return;
        }

        savedPlans.clear();

        // Create initial empty slots
        for (int i = 0; i < 5; i++) {
            SavedPlan plan = new SavedPlan();
            plan.setSlot(i);

            // Load custom slot name or use default
            String customName = getCustomSlotName(i);
            plan.setTitle(customName != null ? customName : "Slot " + (i + 1));
            plan.setDetails("Loading...");
            plan.setEmpty(true);

            savedPlans.add(plan);
        }

        adapter.notifyDataSetChanged();

        // Load each slot asynchronously
        for (int i = 0; i < 5; i++) {
            final int slot = i;
            storageManager.getTripPlanFromSlot(slot, tripPlan -> {
                runOnUiThread(() -> {
                    SavedPlan plan = savedPlans.get(slot);

                    if (tripPlan != null) {
                        String summary = createPlanSummary(tripPlan);
                        plan.setDetails(summary);
                        plan.setEmpty(false);
                        plan.setTripPlan(tripPlan);
                    } else {
                        plan.setDetails("Empty Slot");
                        plan.setEmpty(true);
                        plan.setTripPlan(null);
                    }

                    adapter.notifyItemChanged(slot);
                });
            });
        }
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

            // Update the title in the list immediately
            String displayName = newName.isEmpty() ? "Slot " + (slot + 1) : newName;
            savedPlans.get(slot).setTitle(displayName);
            adapter.notifyItemChanged(slot);

            dialog.dismiss();
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

        // Get the trip plan and show it in a proper dialog
        storageManager.getTripPlanFromSlot(slot, tripPlan -> {
            if (tripPlan != null) {
                runOnUiThread(() -> showTripPlanDialog(tripPlan, slot));
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Failed to load trip plan", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Shows the trip plan dialog using the new dialog_trip_plan_like layout
     */
    private void showTripPlanDialog(TripPlanStorageManager.TripPlan tripPlan, int slot) {
        // Dismiss any existing dialog first
        if (currentTripDialog != null && currentTripDialog.isShowing()) {
            currentTripDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_trip_plan_like, null);

        // Find views in the dialog
        TextView tripTitle = dialogView.findViewById(R.id.tripTitle);
        TextView tripInfoText = dialogView.findViewById(R.id.tripInfoText);
        RecyclerView tripPlanRecyclerView = dialogView.findViewById(R.id.tripPlanRecyclerView);
        Button closeButton = dialogView.findViewById(R.id.closeButton);
        Button shareButton = dialogView.findViewById(R.id.shareButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);

        // Set trip title
        tripTitle.setText("Trip to " + tripPlan.destination);

        // Set trip information
        String tripInfo = "Destination: " + tripPlan.destination + "\n" +
                "Duration: " + tripPlan.duration + "\n" +
                "Weather: " + (tripPlan.weather != null ? tripPlan.weather : "N/A") + "\n" +
                "Start Date: " + (tripPlan.startDate != null ? tripPlan.startDate : "N/A");

        tripInfoText.setText(tripInfo);

        // Set up RecyclerView with trip activities
        if (tripPlan.activitiesListData != null && !tripPlan.activitiesListData.isEmpty()) {
            // IMPORTANT: Pass 'this' as the context so the adapter can call dismissTripDialog()
            DayByDayAdapter adapter = new DayByDayAdapter(tripPlan.activitiesListData, tripPlan.apiKey, this);
            tripPlanRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            tripPlanRecyclerView.setAdapter(adapter);
        }

        // Create dialog without default buttons since we're using custom ones
        currentTripDialog = builder.setView(dialogView).create();

        // Set up button click listeners
        closeButton.setOnClickListener(v -> {
            currentTripDialog.dismiss();
            currentTripDialog = null;
        });

        shareButton.setOnClickListener(v -> {
            sharePlan(tripPlan);
        });

        deleteButton.setOnClickListener(v -> {
            currentTripDialog.dismiss();
            currentTripDialog = null;
            confirmDelete(slot);
        });

        currentTripDialog.show();
    }

    // Public method that can be called from DayByDayAdapter to dismiss dialog
    public void dismissTripDialog() {
        if (currentTripDialog != null && currentTripDialog.isShowing()) {
            currentTripDialog.dismiss();
            currentTripDialog = null;
        }
    }

    // Updated method to show fragments - now public so DayByDayAdapter can call it
    public void showFragment(Fragment fragment) {
        dismissTripDialog(); // Dismiss dialog first

        // Show the fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // Add to back stack so user can navigate back
                .commit();

        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
    }

    private void sharePlan(TripPlanStorageManager.TripPlan tripPlan) {
        StringBuilder shareText = new StringBuilder();
        shareText.append("Check out my trip plan!\n\n");
        shareText.append("Destination: ").append(tripPlan.destination).append("\n");
        shareText.append("Duration: ").append(tripPlan.duration).append("\n");

        if (tripPlan.startDate != null) {
            shareText.append("Start Date: ").append(tripPlan.startDate).append("\n");
        }

        if (tripPlan.activitiesListData != null && !tripPlan.activitiesListData.isEmpty()) {
            shareText.append("\nDaily Activities:\n");
            for (int day = 0; day < tripPlan.activitiesListData.size(); day++) {
                List<PlaceData> dayPlaces = tripPlan.activitiesListData.get(day);
                shareText.append("Day ").append(day + 1).append(": ");
                for (int i = 0; i < dayPlaces.size(); i++) {
                    if (i > 0) shareText.append(", ");
                    shareText.append(dayPlaces.get(i).getName());
                }
                shareText.append("\n");
            }
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Trip Plan"));
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

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning to this activity
        loadSavedPlans();
        // Ensure saved plans is selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_saved_plan);
    }

    // Handle back button press to hide fragments
    @Override
    public void onBackPressed() {
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer.getVisibility() == View.VISIBLE) {
            // Hide fragment container and return to main view
            fragmentContainer.setVisibility(View.GONE);
            // Clear fragment back stack
            getSupportFragmentManager().popBackStack();
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
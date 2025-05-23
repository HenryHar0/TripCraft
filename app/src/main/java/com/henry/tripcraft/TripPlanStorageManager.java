package com.henry.tripcraft;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages saving, loading, and managing trip plans in local storage
 */
public class TripPlanStorageManager {
    private static final String PREFS_NAME = "TripPlanPrefs";
    private static final int MAX_SLOTS = 5;

    private final Context context;
    private final TripNotificationManager notificationManager;
    private final Gson gson;

    public TripPlanStorageManager(Context context, TripNotificationManager notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
        this.gson = new Gson();
    }

    /**
     * Shows dialog to choose a save slot, then handles saving the trip plan
     */
    public void showSaveTripPlanDialog(
            String destination,
            String duration,
            List<List<PlaceData>> activitiesListData,
            String weather,
            String city,
            String startDate) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select a slot to save the plan");

        String[] slots = new String[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            String slotData = sharedPreferences.getString("PlanSlot_" + i, "Empty Slot");
            if (!slotData.equals("Empty Slot")) {
                // Extract destination from saved data for display
                String displayText = extractDestinationFromSavedData(slotData);
                slots[i] = "Slot " + (i + 1) + ": " + displayText;
            } else {
                slots[i] = "Slot " + (i + 1) + ": Empty Slot";
            }
        }

        builder.setItems(slots, (dialog, which) -> {
            String selectedSlotKey = "PlanSlot_" + which;
            if (sharedPreferences.contains(selectedSlotKey) &&
                    !sharedPreferences.getString(selectedSlotKey, "").equals("Empty Slot")) {
                confirmOverwrite(which, destination, duration, activitiesListData, weather, city, startDate);
            } else {
                savePlanToSlot(which, destination, duration, activitiesListData, weather, city, startDate);
            }
        });

        builder.show();
    }

    /**
     * Shows confirmation dialog before overwriting an existing slot
     */
    private void confirmOverwrite(
            int slot,
            String destination,
            String duration,
            List<List<PlaceData>> activitiesListData,
            String weather,
            String city,
            String startDate) {

        new AlertDialog.Builder(context)
                .setTitle("Overwrite Slot")
                .setMessage("Do you want to overwrite this slot?")
                .setPositiveButton("Yes", (dialog, which) ->
                        savePlanToSlot(slot, destination, duration, activitiesListData, weather, city, startDate))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Saves trip plan data to the selected slot
     */
    private void savePlanToSlot(
            int slot,
            String destination,
            String duration,
            List<List<PlaceData>> activitiesListData,
            String weather,
            String city,
            String startDate) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Create a TripPlan object to store all data
        TripPlan tripPlan = new TripPlan();
        tripPlan.destination = destination;
        tripPlan.duration = duration;
        tripPlan.activitiesListData = activitiesListData != null ? activitiesListData : new ArrayList<>();
        tripPlan.weather = weather;
        tripPlan.city = city;
        tripPlan.startDate = startDate;

        // Convert to JSON and save
        String tripPlanJson = gson.toJson(tripPlan);
        editor.putString("PlanSlot_" + slot, tripPlanJson);
        editor.apply();

        boolean notificationsEnabled = sharedPreferences.getBoolean("NotificationsEnabled", false);
        if (notificationsEnabled && startDate != null && !startDate.isEmpty()) {
            notificationManager.scheduleNotifications(startDate, city);
        }

        Toast.makeText(context, "Plan saved to Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a saved trip plan in a dialog with DayByDayAdapter
     */
    public void showSavedTripPlan(int slot) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedData = sharedPreferences.getString("PlanSlot_" + slot, "Empty Slot");

        if (savedData.equals("Empty Slot")) {
            Toast.makeText(context, "No plan saved in this slot", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            TripPlan tripPlan = gson.fromJson(savedData, TripPlan.class);

            // Create dialog with RecyclerView
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_trip_plan_view, null);

            // Set up the RecyclerView with DayByDayAdapter
            RecyclerView recyclerView = dialogView.findViewById(R.id.tripPlanRecyclerView);
            DayByDayAdapter adapter = new DayByDayAdapter(tripPlan.activitiesListData, tripPlan.apiKey);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(adapter);

            builder.setView(dialogView)
                    .setTitle("Trip Plan: " + tripPlan.destination)
                    .setPositiveButton("Close", null)
                    .setNegativeButton("Delete", (dialog, which) -> {
                        confirmDelete(slot);
                    })
                    .show();

        } catch (Exception e) {
            // Handle legacy format or corrupted data
            showLegacyTripPlan(savedData, slot);
        }
    }

    /**
     * Handle legacy saved data format
     */
    private void showLegacyTripPlan(String savedData, int slot) {
        new AlertDialog.Builder(context)
                .setTitle("Saved Trip Plan")
                .setMessage(savedData)
                .setPositiveButton("Close", null)
                .setNegativeButton("Delete", (dialog, which) -> {
                    confirmDelete(slot);
                })
                .show();
    }

    /**
     * Confirms deletion of a saved plan
     */
    private void confirmDelete(int slot) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Plan")
                .setMessage("Are you sure you want to delete this plan?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    deletePlanFromSlot(slot);
                    Toast.makeText(context, "Plan deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Retrieves a saved plan from a specific slot
     * @param slot The slot number (0-based index)
     * @return The saved plan details, or "Empty Slot" if no plan exists
     */
    public String getPlanFromSlot(int slot) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("PlanSlot_" + slot, "Empty Slot");
    }

    /**
     * Gets the trip plan object from a specific slot
     */
    public TripPlan getTripPlanFromSlot(int slot) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedData = sharedPreferences.getString("PlanSlot_" + slot, "Empty Slot");

        if (savedData.equals("Empty Slot")) {
            return null;
        }

        try {
            return gson.fromJson(savedData, TripPlan.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Deletes a plan from a specific slot
     * @param slot The slot number (0-based index)
     */
    public void deletePlanFromSlot(int slot) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("PlanSlot_" + slot, "Empty Slot");
        editor.apply();
    }

    /**
     * Extracts destination from saved data for display purposes
     */
    private String extractDestinationFromSavedData(String savedData) {
        try {
            TripPlan tripPlan = gson.fromJson(savedData, TripPlan.class);
            return tripPlan.destination;
        } catch (Exception e) {
            // Handle legacy format
            if (savedData.contains("Destination: ")) {
                String[] lines = savedData.split("\n");
                for (String line : lines) {
                    if (line.startsWith("Destination: ")) {
                        return line.substring("Destination: ".length());
                    }
                }
            }
            return "Saved Plan";
        }
    }

    /**
     * Inner class to represent a complete trip plan
     */
    public static class TripPlan {
        public String destination;
        public String duration;
        public List<List<PlaceData>> activitiesListData;
        public String weather;
        public String city;
        public String startDate;
        public String apiKey;
    }
}
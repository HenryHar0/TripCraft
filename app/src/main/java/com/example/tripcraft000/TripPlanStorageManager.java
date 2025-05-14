package com.example.tripcraft000;


import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.List;

/**
 * Manages saving, loading, and managing trip plans in local storage
 */
public class TripPlanStorageManager {
    private static final String PREFS_NAME = "TripPlanPrefs";
    private static final int MAX_SLOTS = 5;

    private final Context context;
    private final TripNotificationManager notificationManager;

    public TripPlanStorageManager(Context context, TripNotificationManager notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
    }

    /**
     * Shows dialog to choose a save slot, then handles saving the trip plan
     */
    public void showSaveTripPlanDialog(
            String destination,
            String duration,
            List<String> activitiesListData,
            String weather,
            String city,
            String startDate) {

        final String activities;
        if (activitiesListData != null && !activitiesListData.isEmpty()) {
            activities = TextUtils.join(", ", activitiesListData);
        } else {
            activities = "No activities planned";
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select a slot to save the plan");

        String[] slots = new String[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            String slotData = sharedPreferences.getString("PlanSlot_" + i, "Empty Slot");
            slots[i] = "Slot " + (i + 1) + ": " + slotData;
        }

        builder.setItems(slots, (dialog, which) -> {
            String selectedSlotKey = "PlanSlot_" + which;
            if (sharedPreferences.contains(selectedSlotKey) &&
                    !sharedPreferences.getString(selectedSlotKey, "").equals("Empty Slot")) {
                confirmOverwrite(which, destination, duration, activities, weather, city, startDate);
            } else {
                savePlanToSlot(which, destination, duration, activities, weather, city, startDate);
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
            String activities,
            String weather,
            String city,
            String startDate) {

        new AlertDialog.Builder(context)
                .setTitle("Overwrite Slot")
                .setMessage("Do you want to overwrite this slot?")
                .setPositiveButton("Yes", (dialog, which) ->
                        savePlanToSlot(slot, destination, duration, activities, weather, city, startDate))
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
            String activities,
            String weather,
            String city,
            String startDate) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String planDetails = "Destination: " + destination +
                "\nDuration: " + duration +
                "\nActivities: " + activities +
                "\nWeather: " + weather;

        editor.putString("PlanSlot_" + slot, planDetails);
        editor.apply();

        boolean notificationsEnabled = sharedPreferences.getBoolean("NotificationsEnabled", false);
        if (notificationsEnabled && startDate != null && !startDate.isEmpty()) {
            notificationManager.scheduleNotifications(startDate, city);
        }

        Toast.makeText(context, "Plan saved to Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
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
     * Deletes a plan from a specific slot
     * @param slot The slot number (0-based index)
     */
    public void deletePlanFromSlot(int slot) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("PlanSlot_" + slot, "Empty Slot");
        editor.apply();
    }
}

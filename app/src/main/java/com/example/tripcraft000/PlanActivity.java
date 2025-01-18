package com.example.tripcraft000;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class PlanActivity extends AppCompatActivity {

    private TextView planTitle, destinationLabel, destinationValue, durationLabel, durationValue, activitiesLabel;
    private ListView activitiesList;
    private Button savePlanButton, editPlanButton, backToMainButton;

    private static final String PREFS_NAME = "TripPlanPrefs";

    private String startDate, endDate, city;
    private ArrayAdapter<String> activitiesAdapter;
    private ArrayList<String> activitiesListData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        planTitle = findViewById(R.id.planTitle);
        destinationLabel = findViewById(R.id.destinationLabel);
        destinationValue = findViewById(R.id.destinationValue);
        durationLabel = findViewById(R.id.durationLabel);
        durationValue = findViewById(R.id.durationValue);
        activitiesLabel = findViewById(R.id.activitiesLabel);
        activitiesList = findViewById(R.id.activitiesList);

        savePlanButton = findViewById(R.id.savePlanButton);
        editPlanButton = findViewById(R.id.editPlanButton);
        backToMainButton = findViewById(R.id.backToMainButton);

        Intent intent = getIntent();
        startDate = intent.getStringExtra("start_date");
        endDate = intent.getStringExtra("end_date");
        city = intent.getStringExtra("city");

        if (city != null) {
            destinationValue.setText(city);
        }

        if (startDate != null && endDate != null) {
            calculateDuration();
        }

        generateRandomActivities();

        savePlanButton.setOnClickListener(v -> saveTripPlan());
        editPlanButton.setOnClickListener(v -> editTripPlan());
        backToMainButton.setOnClickListener(v -> goBackToMainMenu());
    }

    private void calculateDuration() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            if (start != null && end != null) {
                long differenceInMillis = end.getTime() - start.getTime();
                long days = differenceInMillis / (1000 * 60 * 60 * 24);
                durationValue.setText(days + " days");
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Error calculating duration", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateRandomActivities() {
        String[] activityPool = {
                "Visit local museum",
                "Go hiking",
                "Try traditional food",
                "Explore a local market",
                "Relax at a park",
                "Take a boat ride",
                "Attend a local event",
                "Visit a historic site",
                "Take a guided tour",
                "Try a cooking class"
        };

        activitiesListData = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 5; i++) {
            int index = random.nextInt(activityPool.length);
            activitiesListData.add(activityPool[index]);
        }

        activitiesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, activitiesListData);
        activitiesList.setAdapter(activitiesAdapter);
    }

    private void saveTripPlan() {
        String destination = destinationValue.getText().toString();
        String duration = durationValue.getText().toString();
        String activities = String.join(", ", activitiesListData);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a slot to save the plan");

        String[] slots = new String[5];
        for (int i = 0; i < 5; i++) {
            String slotData = sharedPreferences.getString("PlanSlot_" + i, "Empty Slot");
            slots[i] = "Slot " + (i + 1) + ": " + slotData;
        }

        builder.setItems(slots, (dialog, which) -> {
            String selectedSlotKey = "PlanSlot_" + which;
            if (sharedPreferences.contains(selectedSlotKey)) {
                confirmOverwrite(which, destination, duration, activities);
            } else {
                savePlanToSlot(which, destination, duration, activities);
            }
        });

        builder.show();
    }

    private void confirmOverwrite(int slot, String destination, String duration, String activities) {
        new AlertDialog.Builder(this)
                .setTitle("Overwrite Slot")
                .setMessage("Do you want to overwrite this slot?")
                .setPositiveButton("Yes", (dialog, which) -> savePlanToSlot(slot, destination, duration, activities))
                .setNegativeButton("No", null)
                .show();
    }

    private void savePlanToSlot(int slot, String destination, String duration, String activities) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String planDetails = "Destination: " + destination + "\nDuration: " + duration + "\nActivities: " + activities;
        editor.putString("PlanSlot_" + slot, planDetails);
        editor.apply();

        Toast.makeText(this, "Plan saved to Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
    }

    private void editTripPlan() {
        Toast.makeText(this, "Edit plan functionality coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void goBackToMainMenu() {
        Intent intent = new Intent(PlanActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

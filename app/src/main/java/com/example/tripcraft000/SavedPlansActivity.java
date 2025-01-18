package com.example.tripcraft000;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SavedPlansActivity extends AppCompatActivity {

    private ListView savedPlansList;
    private ArrayList<String> savedPlans;
    private ArrayAdapter<String> adapter;

    private static final String PREFS_NAME = "TripPlanPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans_saved);

        savedPlansList = findViewById(R.id.savedPlansList);

        savedPlans = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, savedPlans);
        savedPlansList.setAdapter(adapter);

        loadSavedPlans();

        savedPlansList.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPlanKey = "PlanSlot_" + position;
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String planDetails = sharedPreferences.getString(selectedPlanKey, null);

            if (planDetails != null) {
                showPlanDetails(position, planDetails);
            } else {
                Toast.makeText(SavedPlansActivity.this, "No plan saved in this slot.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.backButton).setOnClickListener(v -> {
            Intent intent = new Intent(SavedPlansActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadSavedPlans() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        for (int i = 0; i < 5; i++) {
            String planDetails = sharedPreferences.getString("PlanSlot_" + i, "Empty Slot");
            savedPlans.add("Slot " + (i + 1) + ": " + planDetails);
        }
        adapter.notifyDataSetChanged();
    }

    private void showPlanDetails(int slot, String planDetails) {
        new AlertDialog.Builder(this)
                .setTitle("Plan Details")
                .setMessage("Slot " + (slot + 1) + ":\n" + planDetails)
                .setPositiveButton("OK", null)
                .setNegativeButton("Delete", (dialog, which) -> deletePlan(slot))
                .show();
    }

    private void deletePlan(int slot) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedPlanKey = "PlanSlot_" + slot;

        sharedPreferences.edit().remove(selectedPlanKey).apply();

        savedPlans.set(slot, "Slot " + (slot + 1) + ": Empty Slot");
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Plan deleted from Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
    }

    public void savePlanToSlot(int slot) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedPlanKey = "PlanSlot_" + slot;

        if (sharedPreferences.contains(selectedPlanKey)) {
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

        String planDetails = "Sample Plan Data";
        String creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        sharedPreferences.edit().putString(selectedPlanKey, planDetails + " (Created: " + creationDate + ")").apply();

        savedPlans.set(slot, "Slot " + (slot + 1) + ": " + planDetails);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Plan saved in Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
    }
}

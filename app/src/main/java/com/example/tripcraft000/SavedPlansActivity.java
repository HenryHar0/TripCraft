package com.example.tripcraft000;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SavedPlansActivity extends AppCompatActivity {

    private RecyclerView savedPlansList;
    private List<SavedPlan> savedPlans;
    private SavedPlansAdapter adapter;

    private static final String PREFS_NAME = "TripPlanPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans_saved);

        savedPlansList = findViewById(R.id.savedPlansList);
        savedPlansList.setLayoutManager(new LinearLayoutManager(this));

        savedPlans = new ArrayList<>();
        adapter = new SavedPlansAdapter(savedPlans, this::showPlanDetails);
        savedPlansList.setAdapter(adapter);

        loadSavedPlans();

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SavedPlansActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadSavedPlans() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        savedPlans.clear();

        for (int i = 0; i < 5; i++) {
            String planKey = "PlanSlot_" + i;
            String planDetails = sharedPreferences.getString(planKey, null);

            SavedPlan plan = new SavedPlan();
            plan.setSlot(i);

            if (planDetails != null && !planDetails.equals("Empty Slot")) {
                plan.setTitle("Slot " + (i + 1));
                plan.setDetails(planDetails);
                plan.setEmpty(false);
            } else {
                plan.setTitle("Slot " + (i + 1));
                plan.setDetails("Empty Slot");
                plan.setEmpty(true);
            }

            savedPlans.add(plan);
        }

        adapter.notifyDataSetChanged();
    }

    private void showPlanDetails(int slot) {
        if (savedPlans.get(slot).isEmpty()) {
            Toast.makeText(SavedPlansActivity.this, "No plan saved in this slot.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Plan Details")
                .setMessage("Slot " + (slot + 1) + ":\n" + savedPlans.get(slot).getDetails())
                .setPositiveButton("OK", null)
                .setNegativeButton("Delete", (dialog, which) -> deletePlan(slot))
                .show();
    }

    private void deletePlan(int slot) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedPlanKey = "PlanSlot_" + slot;

        sharedPreferences.edit().remove(selectedPlanKey).apply();

        SavedPlan plan = savedPlans.get(slot);
        plan.setDetails("Empty Slot");
        plan.setEmpty(true);

        adapter.notifyItemChanged(slot);

        Toast.makeText(this, "Plan deleted from Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
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

        String planDetails = "Sample Plan Data";
        String creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        String fullDetails = planDetails + " (Created: " + creationDate + ")";

        sharedPreferences.edit().putString(selectedPlanKey, fullDetails).apply();

        SavedPlan plan = savedPlans.get(slot);
        plan.setDetails(fullDetails);
        plan.setEmpty(false);

        adapter.notifyItemChanged(slot);

        Toast.makeText(this, "Plan saved in Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
    }

    public static class SavedPlan {
        private int slot;
        private String title;
        private String details;
        private boolean isEmpty;

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
    }
}
package com.example.tripcraft000;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private EditText cityInput, daysInput;
    private Button interestButton1, interestButton2, interestButton3, generatePlanButton, saveButton, loadButton;
    private TextView generatedPlanText;
    private ListView savedPlansList;
    private String selectedCity = "";
    private String selectedInterests = "";
    private int stayDuration = 0;

    private static final String PREFS_NAME = "TripCraftPrefs";
    private static final String CITY_KEY = "city";
    private static final String INTERESTS_KEY = "interests";
    private static final String DAYS_KEY = "days";
    private static final String DATE_KEY = "date";

    private static final String[] SAVE_SLOTS = {"Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityInput = findViewById(R.id.cityInput);
        daysInput = findViewById(R.id.daysInput);
        interestButton1 = findViewById(R.id.interestButton1);
        interestButton2 = findViewById(R.id.interestButton2);
        interestButton3 = findViewById(R.id.interestButton3);
        generatePlanButton = findViewById(R.id.generatePlanButton);
        saveButton = findViewById(R.id.saveButton);
        loadButton = findViewById(R.id.loadButton);
        generatedPlanText = findViewById(R.id.generatedPlanText);
        savedPlansList = findViewById(R.id.savedPlansList);

        interestButton1.setOnClickListener(v -> toggleInterest("Interest 1"));
        interestButton2.setOnClickListener(v -> toggleInterest("Interest 2"));
        interestButton3.setOnClickListener(v -> toggleInterest("Interest 3"));

        generatePlanButton.setOnClickListener(v -> generatePlan());

        saveButton.setOnClickListener(v -> showSaveSlotDialog());

        loadButton.setOnClickListener(v -> showLoadSlotDialog());

        savedPlansList.setOnItemClickListener((parent, view, position, id) -> loadSavedPlan(SAVE_SLOTS[position]));
    }

    private void toggleInterest(String interest) {
        if (selectedInterests.contains(interest)) {
            selectedInterests = selectedInterests.replace(interest + ", ", "");
        } else {
            selectedInterests += interest + ", ";
        }
    }

    private void generatePlan() {
        selectedCity = cityInput.getText().toString().trim();
        String daysStr = daysInput.getText().toString().trim();

        if (selectedCity.isEmpty()) {
            generatedPlanText.setText("Please enter a city.");
            return;
        }

        if (selectedInterests.isEmpty()) {
            generatedPlanText.setText("Please select at least one interest.");
            return;
        }

        if (daysStr.isEmpty()) {
            generatedPlanText.setText("Please enter the number of days.");
            return;
        }

        stayDuration = Integer.parseInt(daysStr);

        String plan = "Your Trip to " + selectedCity + " for " + stayDuration + " days.\n" +
                "Interests: " + selectedInterests + "\n" +
                "We will generate the best plan for you based on proximity and reviews!";

        generatedPlanText.setText(plan);
    }

    private void showSaveSlotDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose a Save Slot");

        builder.setItems(SAVE_SLOTS, (dialog, which) -> saveToSlot(SAVE_SLOTS[which]));

        builder.show();
    }

    private void saveToSlot(String slotName) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CITY_KEY + slotName, selectedCity);
        editor.putString(INTERESTS_KEY + slotName, selectedInterests);
        editor.putString(DAYS_KEY + slotName, String.valueOf(stayDuration));
        editor.putString(DATE_KEY + slotName, currentDate);
        editor.apply();

        Toast.makeText(this, "Plan saved to " + slotName + " on " + currentDate, Toast.LENGTH_SHORT).show();

        generatedPlanText.setText("Plan saved on: " + currentDate);
    }

    private void showLoadSlotDialog() {
        String[] slotsWithDates = new String[SAVE_SLOTS.length];

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        for (int i = 0; i < SAVE_SLOTS.length; i++) {
            String slotName = SAVE_SLOTS[i];
            String savedDate = sharedPreferences.getString(DATE_KEY + slotName, "Not saved yet");

            slotsWithDates[i] = SAVE_SLOTS[i] + " - Saved on: " + savedDate;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose a Slot to Load");

        builder.setItems(slotsWithDates, (dialog, which) -> loadSavedPlan(SAVE_SLOTS[which]));

        builder.show();
    }

    private void loadSavedPlan(String slotName) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String city = sharedPreferences.getString(CITY_KEY + slotName, "");
        String interests = sharedPreferences.getString(INTERESTS_KEY + slotName, "");
        String days = sharedPreferences.getString(DAYS_KEY + slotName, "");
        String date = sharedPreferences.getString(DATE_KEY + slotName, "");

        if (city.isEmpty()) {
            Toast.makeText(this, "No plan saved in " + slotName, Toast.LENGTH_SHORT).show();
            return;
        }

        cityInput.setText(city);
        selectedInterests = interests;
        daysInput.setText(days);

        Toast.makeText(this, "Loaded plan from " + slotName + " (Saved on: " + date + ")", Toast.LENGTH_SHORT).show();
    }
}

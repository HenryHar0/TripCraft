package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class TimeActivity extends AppCompatActivity {

    private static final String TAG = "TimeActivity";
    private static final int MAX_HOURS_PER_DAY = 24;
    private static final int MIN_HOURS_PER_DAY = 0;

    // UI Components
    private RecyclerView daysRecyclerView;
    private EditText bulkHoursInput;
    private Button applyBulkButton, continueButton;

    // Data
    private HashMap<Integer, Integer> hoursPerDay;
    private DayAdapter dayAdapter;
    private int totalDays;

    // Trip data from previous screens
    private TripData tripData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time);

        initViews();
        loadTripData();
        setupDaysRecyclerView();
        setupListeners();
        validateContinueButton(); // Initial validation
    }

    private void initViews() {
        daysRecyclerView = findViewById(R.id.daysRecyclerView);
        bulkHoursInput = findViewById(R.id.bulkHoursInput);
        applyBulkButton = findViewById(R.id.applyBulkButton);
        continueButton = findViewById(R.id.continueButton);

        // Disable continue button initially
        continueButton.setEnabled(false);
        continueButton.setAlpha(0.5f);
    }

    private void loadTripData() {
        Intent incomingIntent = getIntent();
        if (incomingIntent == null) {
            showErrorAndFinish("No data received");
            return;
        }

        try {
            tripData = new TripData(
                    incomingIntent.getStringExtra("city"),
                    incomingIntent.getStringExtra("start_date"),
                    incomingIntent.getStringExtra("end_date"),
                    incomingIntent.getDoubleExtra("city_lat", 0),
                    incomingIntent.getDoubleExtra("city_lng", 0),
                    incomingIntent.getStringArrayListExtra("selected_categories"),
                    incomingIntent.getStringArrayListExtra("selected_place_ids")
            );

            // Validate essential data
            if (TextUtils.isEmpty(tripData.getCity()) ||
                    TextUtils.isEmpty(tripData.getStartDate()) ||
                    TextUtils.isEmpty(tripData.getEndDate())) {
                showErrorAndFinish("Missing essential trip data");
                return;
            }

            Log.d(TAG, "Loaded trip data: " + tripData);
            totalDays = calculateTotalDays(tripData.getStartDate(), tripData.getEndDate());

            if (totalDays <= 0) {
                showErrorAndFinish("Invalid date range");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading trip data", e);
            showErrorAndFinish("Error loading trip data");
        }
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void setupDaysRecyclerView() {
        hoursPerDay = new HashMap<>();
        dayAdapter = new DayAdapter(totalDays, hoursPerDay, this::onHoursChanged);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        daysRecyclerView.setLayoutManager(layoutManager);

        // Add dividers between items
        DividerItemDecoration divider = new DividerItemDecoration(
                daysRecyclerView.getContext(), layoutManager.getOrientation());
        daysRecyclerView.addItemDecoration(divider);

        daysRecyclerView.setAdapter(dayAdapter);
    }

    private void setupListeners() {
        applyBulkButton.setOnClickListener(v -> applyBulkHours());
        continueButton.setOnClickListener(v -> {
            if (allDaysHaveValidHours()) {
                proceedToPlanActivity();
            } else {
                showMissingHoursWarning();
            }
        });
    }

    private void onHoursChanged(int dayIndex, int hours) {
        // Callback when hours are changed for a specific day
        hoursPerDay.put(dayIndex, hours);
        validateContinueButton();
    }

    private void applyBulkHours() {
        String bulkText = bulkHoursInput.getText().toString().trim();
        if (bulkText.isEmpty()) {
            Toast.makeText(this, "Please enter hours", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int bulkHours = Integer.parseInt(bulkText);
            if (bulkHours < MIN_HOURS_PER_DAY || bulkHours > MAX_HOURS_PER_DAY) {
                Toast.makeText(this, "Hours must be between " + MIN_HOURS_PER_DAY +
                        " and " + MAX_HOURS_PER_DAY, Toast.LENGTH_SHORT).show();
                return;
            }

            for (int i = 0; i < totalDays; i++) {
                hoursPerDay.put(i, bulkHours);
            }
            dayAdapter.notifyDataSetChanged();
            validateContinueButton();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean allDaysHaveValidHours() {
        for (int i = 0; i < totalDays; i++) {
            Integer hours = hoursPerDay.get(i);
            if (hours == null || hours <= 0) {
                return false;
            }
        }
        return true;
    }

    private void showMissingHoursWarning() {
        StringBuilder missingDays = new StringBuilder("Please set hours for day(s): ");
        boolean first = true;

        for (int i = 0; i < totalDays; i++) {
            Integer hours = hoursPerDay.get(i);
            if (hours == null || hours <= 0) {
                if (!first) {
                    missingDays.append(", ");
                }
                missingDays.append(i + 1);
                first = false;
            }
        }

        Toast.makeText(this, missingDays.toString(), Toast.LENGTH_LONG).show();
    }

    private void validateContinueButton() {
        boolean allDaysValid = allDaysHaveValidHours();
        continueButton.setEnabled(allDaysValid);
        continueButton.setAlpha(allDaysValid ? 1.0f : 0.5f);
    }

    private void proceedToPlanActivity() {
        Intent planIntent = new Intent(TimeActivity.this, PlanActivity.class);

        // Pass all trip data
        planIntent.putExtra("city", tripData.getCity());
        planIntent.putExtra("start_date", tripData.getStartDate());
        planIntent.putExtra("end_date", tripData.getEndDate());
        planIntent.putExtra("city_lat", tripData.getCityLat());
        planIntent.putExtra("city_lng", tripData.getCityLng());
        planIntent.putStringArrayListExtra("selected_categories", tripData.getSelectedCategories());
        planIntent.putStringArrayListExtra("selected_place_ids", tripData.getSelectedPlaceIds());

        // Pass hours per day
        ArrayList<Integer> hoursList = new ArrayList<>();
        for (int i = 0; i < totalDays; i++) {
            hoursList.add(Objects.requireNonNull(hoursPerDay.get(i)));
        }
        planIntent.putIntegerArrayListExtra("hours_per_day", hoursList);

        startActivity(planIntent);
        finish();
    }

    private int calculateTotalDays(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);

            if (startDate != null && endDate != null) {
                long diffInMillis = Math.abs(endDate.getTime() - startDate.getTime());
                long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
                return (int) diffInDays + 1;
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates", e);
            Toast.makeText(this, "Error calculating trip days", Toast.LENGTH_SHORT).show();
        }
        return 1; // Default at least 1 day
    }

    /**
     * Data class to hold trip information
     */
    private static class TripData {
        private final String city;
        private final String startDate;
        private final String endDate;
        private final double cityLat;
        private final double cityLng;
        private final ArrayList<String> selectedCategories;
        private final ArrayList<String> selectedPlaceIds;

        public TripData(String city, String startDate, String endDate,
                        double cityLat, double cityLng,
                        ArrayList<String> selectedCategories,
                        ArrayList<String> selectedPlaceIds) {
            this.city = city;
            this.startDate = startDate;
            this.endDate = endDate;
            this.cityLat = cityLat;
            this.cityLng = cityLng;
            this.selectedCategories = selectedCategories;
            this.selectedPlaceIds = selectedPlaceIds;
        }

        public String getCity() { return city; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public double getCityLat() { return cityLat; }
        public double getCityLng() { return cityLng; }
        public ArrayList<String> getSelectedCategories() { return selectedCategories; }
        public ArrayList<String> getSelectedPlaceIds() { return selectedPlaceIds; }

        @Override
        public String toString() {
            return "TripData{" +
                    "city='" + city + '\'' +
                    ", startDate='" + startDate + '\'' +
                    ", endDate='" + endDate + '\'' +
                    ", totalCategories=" + (selectedCategories != null ? selectedCategories.size() : 0) +
                    ", totalPlaceIds=" + (selectedPlaceIds != null ? selectedPlaceIds.size() : 0) +
                    '}';
        }
    }
}
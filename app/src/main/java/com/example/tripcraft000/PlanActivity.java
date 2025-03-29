package com.example.tripcraft000;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlanActivity extends AppCompatActivity {

    private TextView planTitle, weatherInfo;
    private TextView destinationValue, durationValue;
    private RecyclerView activitiesList, chosenActivitiesRecycler;
    private Button savePlanButton, editPlanButton, backToMainButton;

    private static final String PREFS_NAME = "TripPlanPrefs";
    private static final String TAG = "PlanActivity";
    private static final String NOAA_POINTS_BASE_URL = "https://api.weather.gov/points/";
    private static final String NOAA_FORECAST_BASE_URL = "https://api.weather.gov/gridpoints/";

    private String startDate, endDate, city;
    private double latitude, longitude;
    private ArrayList<String> activitiesListData;
    private ActivityAdapter activitiesAdapter;
    private ExecutorService executorService;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        // Initialize services
        executorService = Executors.newSingleThreadExecutor();
        httpClient = new OkHttpClient();

        // Initialize views
        planTitle = findViewById(R.id.planTitle);
        destinationValue = findViewById(R.id.destinationValue);
        durationValue = findViewById(R.id.durationValue);
        weatherInfo = findViewById(R.id.weatherInfo);
        activitiesList = findViewById(R.id.activitiesList);
        chosenActivitiesRecycler = findViewById(R.id.chosenActivitiesRecycler);

        savePlanButton = findViewById(R.id.savePlanButton);
        editPlanButton = findViewById(R.id.editPlanButton);
        backToMainButton = findViewById(R.id.backToMainButton);

        // Retrieve intent extras
        Intent intent = getIntent();
        startDate = intent.getStringExtra("start_date");
        endDate = intent.getStringExtra("end_date");
        city = intent.getStringExtra("selected_city");
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);
        ArrayList<String> selectedCategories = intent.getStringArrayListExtra("selected_categories");

        // Set up chosen activities
        if (selectedCategories != null && !selectedCategories.isEmpty()) {
            setupChosenActivities(selectedCategories);
        }

        // Set destination
        if (city != null) {
            destinationValue.setText(city);
        }

        // Calculate duration
        if (startDate != null && endDate != null) {
            calculateDuration();
        }

        // Fetch weather information
        if (latitude != 0.0 && longitude != 0.0) {
            fetchWeatherInfo();
        }

        // Generate random activities
        generateRandomActivities();

        // Set button click listeners
        savePlanButton.setOnClickListener(v -> saveTripPlan());
        editPlanButton.setOnClickListener(v -> editTripPlan());
        backToMainButton.setOnClickListener(v -> goBackToMainMenu());
    }

    private void fetchWeatherInfo() {
        executorService.execute(() -> {
            try {
                // Step 1: Get Grid Points
                String pointsUrl = NOAA_POINTS_BASE_URL + latitude + "," + longitude;
                String gridResponse = makeHttpRequest(pointsUrl);
                JSONObject gridPointsJson = new JSONObject(gridResponse);

                String gridId = gridPointsJson.getJSONObject("properties").getString("gridId");
                int gridX = gridPointsJson.getJSONObject("properties").getInt("gridX");
                int gridY = gridPointsJson.getJSONObject("properties").getInt("gridY");

                // Step 2: Get Forecast
                String forecastUrl = NOAA_FORECAST_BASE_URL + gridId + "/" + gridX + "," + gridY + "/forecast";
                String forecastResponse = makeHttpRequest(forecastUrl);
                JSONObject forecastJson = new JSONObject(forecastResponse);

                JSONObject period = forecastJson.getJSONObject("properties")
                        .getJSONArray("periods")
                        .getJSONObject(0);

                final String temperature = period.getString("temperature") + "°" +
                        period.getString("temperatureUnit");
                final String forecast = period.getString("shortForecast");

                runOnUiThread(() -> {
                    weatherInfo.setText(String.format("Temperature: %s\nForecast: %s",
                            temperature, forecast));
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching weather", e);
                runOnUiThread(() -> {
                    weatherInfo.setText("Unable to fetch weather information");
                });
            }
        });
    }

    private String makeHttpRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "(tripcraft, harutyunyanhenry5@gmail.com)")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    private void setupChosenActivities(ArrayList<String> categories) {
        // Set up horizontal RecyclerView for chosen activities
        chosenActivitiesRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Create and set adapter for chosen activities
        ActivityCategoryAdapter categoryAdapter = new ActivityCategoryAdapter(categories);
        chosenActivitiesRecycler.setAdapter(categoryAdapter);
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

        // Create and set adapter for the activities RecyclerView
        activitiesAdapter = new ActivityAdapter(activitiesListData);
        activitiesList.setLayoutManager(new LinearLayoutManager(this));
        activitiesList.setAdapter(activitiesAdapter);
    }

    private void saveTripPlan() {
        String destination = destinationValue.getText().toString();
        String duration = durationValue.getText().toString();
        String activities = TextUtils.join(", ", activitiesListData);
        String weather = weatherInfo.getText().toString();

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
                confirmOverwrite(which, destination, duration, activities, weather);
            } else {
                savePlanToSlot(which, destination, duration, activities, weather);
            }
        });

        builder.show();
    }

    private void confirmOverwrite(int slot, String destination, String duration, String activities, String weather) {
        new AlertDialog.Builder(this)
                .setTitle("Overwrite Slot")
                .setMessage("Do you want to overwrite this slot?")
                .setPositiveButton("Yes", (dialog, which) -> savePlanToSlot(slot, destination, duration, activities, weather))
                .setNegativeButton("No", null)
                .show();
    }

    private void savePlanToSlot(int slot, String destination, String duration, String activities, String weather) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String planDetails = "Destination: " + destination +
                "\nDuration: " + duration +
                "\nActivities: " + activities +
                "\nWeather: " + weather;
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

    // Adapter for displaying chosen activity categories
    private class ActivityCategoryAdapter extends RecyclerView.Adapter<ActivityCategoryAdapter.ViewHolder> {
        private ArrayList<String> categories;

        public ActivityCategoryAdapter(ArrayList<String> categories) {
            this.categories = categories;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(
                    android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(categories.get(position));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }

    // Adapter for displaying activities
    private class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
        private ArrayList<String> activities;

        public ActivityAdapter(ArrayList<String> activities) {
            this.activities = activities;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(
                    android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(activities.get(position));
        }

        @Override
        public int getItemCount() {
            return activities.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
package com.example.tripcraft000;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.tripcraft000.models.PointResponse;
import com.example.tripcraft000.models.WeatherResponse;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PlanActivity extends AppCompatActivity {

    private TextView planTitle, destinationLabel, destinationValue, durationLabel, durationValue, activitiesLabel, weatherInfo;
    private ListView activitiesList;
    private Button savePlanButton, editPlanButton, deletePlanButton, backToMainButton;

    private static final String PREFS_NAME = "TripPlanPrefs";
    private static final String NOAA_API_URL = "https://api.weather.gov/points/";
    private static final String NOAA_API_TOKEN = "uTcUeWpkpWorKDtgNhAEhydHEhoTPLZM";

    private String startDate, endDate, city;
    private double lat, lng;
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
        weatherInfo = findViewById(R.id.weatherInfo);

        savePlanButton = findViewById(R.id.savePlanButton);
        editPlanButton = findViewById(R.id.editPlanButton);
        deletePlanButton = findViewById(R.id.deletePlanButton);
        backToMainButton = findViewById(R.id.backToMainButton);

        Intent intent = getIntent();
        startDate = intent.getStringExtra("start_date");
        endDate = intent.getStringExtra("end_date");
        city = intent.getStringExtra("city");
        lat = intent.getDoubleExtra("lat", 0);
        lng = intent.getDoubleExtra("lng", 0);

        if (city != null) {
            destinationValue.setText(city);
        }

        if (startDate != null && endDate != null) {
            calculateDuration();
        }

        generateRandomActivities();

        // Fetch weather data
        fetchWeatherData(lat, lng);

        savePlanButton.setOnClickListener(v -> saveTripPlan());
        editPlanButton.setOnClickListener(v -> editTripPlan());
        deletePlanButton.setOnClickListener(v -> deleteTripPlan());
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
            e.printStackTrace();
            Toast.makeText(this, "Error calculating duration", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchWeatherData(double lat, double lng) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weather.gov/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherAPI weatherAPI = retrofit.create(WeatherAPI.class);

        // Step 1: Fetch point data (we get forecast URL)
        String pointsUrl = "https://api.weather.gov/points/" + lat + "," + lng;

        Call<PointResponse> call = weatherAPI.getPointData(pointsUrl);

        call.enqueue(new Callback<PointResponse>() {
            @Override
            public void onResponse(Call<PointResponse> call, Response<PointResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Get forecast URL from PointResponse
                    String forecastUrl = response.body().properties.forecast;

                    // Step 2: Fetch forecast data
                    fetchForecastData(forecastUrl);
                } else {
                    Toast.makeText(PlanActivity.this, "Failed to fetch weather data.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PointResponse> call, Throwable t) {
                Toast.makeText(PlanActivity.this, "Error fetching weather data: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchForecastData(String forecastUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weather.gov/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherAPI weatherAPI = retrofit.create(WeatherAPI.class);

        Call<WeatherResponse> call = weatherAPI.getForecast(forecastUrl);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Parse and display weather info
                    WeatherResponse weatherResponse = response.body();
                    if (weatherResponse.properties.periods != null && !weatherResponse.properties.periods.isEmpty()) {
                        StringBuilder weatherData = new StringBuilder("Weather Forecast: \n");
                        for (WeatherResponse.Period period : weatherResponse.properties.periods) {
                            weatherData.append(period.name).append(": ").append(period.detailedForecast).append("\n");
                        }
                        // Update the UI
                        PlanActivity.this.runOnUiThread(() -> weatherInfo.setText(weatherData.toString()));
                    }
                } else {
                    Toast.makeText(PlanActivity.this, "Failed to fetch weather forecast.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(PlanActivity.this, "Error fetching forecast data: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }






    private void parseWeatherData(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray periods = jsonResponse.getJSONObject("properties").getJSONArray("periods");

            if (periods.length() > 0) {
                JSONObject firstPeriod = periods.getJSONObject(0);
                String detailedForecast = firstPeriod.getString("detailedForecast");

                weatherInfo.setText(detailedForecast);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing weather data.", Toast.LENGTH_SHORT).show();
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
                .setMessage("This slot already contains a plan. Do you want to overwrite it?")
                .setPositiveButton("Yes", (dialog, which) -> savePlanToSlot(slot, destination, duration, activities))
                .setNegativeButton("No", null)
                .show();
    }

    private void savePlanToSlot(int slot, String destination, String duration, String activities) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        String planData = destination + " | " + duration + " | " + activities + " (Created: " + creationDate + ")";
        editor.putString("PlanSlot_" + slot, planData);
        editor.apply();

        Toast.makeText(this, "Plan saved in Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
    }

    private void editTripPlan() {
        Toast.makeText(this, "Editing trip plans is not implemented yet.", Toast.LENGTH_SHORT).show();
    }

    private void deleteTripPlan() {
        Toast.makeText(this, "Deleting trip plans is not implemented in this activity.", Toast.LENGTH_SHORT).show();
    }

    private void goBackToMainMenu() {
        Intent intent = new Intent(PlanActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

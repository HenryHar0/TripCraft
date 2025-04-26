package com.example.tripcraft000;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;


import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.LocationBias;
import com.google.android.libraries.places.api.model.LocationRestriction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class PlanActivity extends AppCompatActivity {

    private TextView planTitle, weatherInfo;
    private TextView destinationValue, durationValue;
    private RecyclerView activitiesList, chosenActivitiesRecycler;
    private Button savePlanButton, editPlanButton, backToMainButton;

    private static final String PREFS_NAME = "TripPlanPrefs";
    private static final String NOTIFICATION_PREF = "NotificationsEnabled";
    private static final String FIRST_TIME_KEY = "FirstTimeUser";
    private static final String TAG = "PlanActivity";
    private static final String NOAA_POINTS_BASE_URL = "https://api.weather.gov/points/";
    private static final String NOAA_FORECAST_BASE_URL = "https://api.weather.gov/gridpoints/";
    private static final String CHANNEL_ID = "trip_reminder_channel";
    private static final int ONE_WEEK_NOTIFICATION_ID = 1;
    private static final int ONE_DAY_NOTIFICATION_ID = 2;

    private String startDate, endDate, city;
    private double latitude, longitude;
    private ArrayList<String> activitiesListData;
    private ExecutorService executorService;
    private OkHttpClient httpClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private PlacesClient placesClient;
    private ArrayList<String> placesData = new ArrayList<>();
    private ActivityAdapter placesAdapter;
    private LatLngBounds cityBounds;
    private RectangularBounds rectangularBounds;
    private Button showMandatoryButton;
    private ArrayList<String> selectedPlaceIds;
    private boolean showingSelectedPlaces = false;
    private ArrayList<String> allPlacesData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        // Initialize services
        executorService = Executors.newSingleThreadExecutor();
        httpClient = new OkHttpClient();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCYnYiiqrHO0uwKoxNQLA_mKEIuX1aRyL4");
        }
        placesClient = Places.createClient(this);

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
        showMandatoryButton = findViewById(R.id.showMandatoryButton);

        allPlacesData = new ArrayList<>();
        showMandatoryButton.setOnClickListener(v -> toggleSelectedPlaces());

        // Setup notification permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(NOTIFICATION_PREF, true);
                        editor.apply();

                        // Schedule notifications if start date is available
                        if (startDate != null && !startDate.isEmpty()) {
                            scheduleNotifications(startDate);
                        }

                        Toast.makeText(this, "Trip reminders will be sent before your journey!", Toast.LENGTH_SHORT).show();
                    } else {
                        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(NOTIFICATION_PREF, false);
                        editor.apply();

                        Toast.makeText(this, "Notifications disabled. You can enable them later in settings.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Create notification channel
        createNotificationChannel();

        // Retrieve intent extras
        Intent intent = getIntent();
        startDate = intent.getStringExtra("start_date");
        endDate = intent.getStringExtra("end_date");
        city = intent.getStringExtra("city");
        latitude = getIntent().getDoubleExtra("city_lat", 0.0);
        longitude = getIntent().getDoubleExtra("city_lng", 0.0);
        ArrayList<String> selectedCategories = intent.getStringArrayListExtra("selected_categories");
        selectedPlaceIds = intent.getStringArrayListExtra("selected_place_ids");

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

        fetchWeatherInfo();


        // Set button click listeners
        savePlanButton.setOnClickListener(v -> saveTripPlan());
        editPlanButton.setOnClickListener(v -> editTripPlan());
        backToMainButton.setOnClickListener(v -> goBackToMainMenu());


        // Check if first time user
        checkFirstTimeUser();
    }

    private void checkFirstTimeUser() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstTime = sharedPreferences.getBoolean(FIRST_TIME_KEY, true);

        if (isFirstTime) {
            showNotificationExplanationDialog();

            // Mark as not first time anymore
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(FIRST_TIME_KEY, false);
            editor.apply();
        }
    }

    private void showNotificationExplanationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip Reminders");

        // Blue-styled explanation text
        TextView messageView = new TextView(this);
        messageView.setText("TripCraft can send you helpful reminders about your upcoming trips!\n\n" +
                "• Get notified one week before your trip to start packing\n" +
                "• Receive a final reminder the day before departure\n\n" +
                "Would you like to enable trip reminders?");
        messageView.setPadding(30, 30, 30, 30);
        messageView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        messageView.setTextSize(16);

        builder.setView(messageView);

        builder.setPositiveButton("Enable", (dialog, which) -> {
            requestNotificationPermission();
        });

        builder.setNegativeButton("Not Now", (dialog, which) -> {
            Toast.makeText(this, "You can enable notifications later in settings", Toast.LENGTH_SHORT).show();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // For older versions, no runtime permission needed
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(NOTIFICATION_PREF, true);
            editor.apply();

            if (startDate != null && !startDate.isEmpty()) {
                scheduleNotifications(startDate);
            }

            Toast.makeText(this, "Trip reminders enabled!", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Trip Reminders";
            String description = "Notifications for upcoming trips";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleNotifications(String tripDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date tripStartDate = dateFormat.parse(tripDate);

            if (tripStartDate != null) {
                scheduleNotification(tripStartDate, 7, ONE_WEEK_NOTIFICATION_ID,
                        "Trip to " + city + " is in one week!",
                        "Time to start planning and packing for your adventure.");

                scheduleNotification(tripStartDate, 1, ONE_DAY_NOTIFICATION_ID,
                        "Your trip to " + city + " is tomorrow!",
                        "Final preparations for your journey to " + city + ".");
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error scheduling notifications", e);
        }
    }

    private void scheduleNotification(Date tripDate, int daysBefore, int notificationId, String title, String message) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tripDate);
        calendar.add(Calendar.DAY_OF_YEAR, -daysBefore);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    private void fetchPlaceById(String placeId) {
        // Define the fields to return
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.TYPES,
                Place.Field.RATING
        );

        // Create a fetch request
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        // Execute the request
        placesClient.fetchPlace(request)
                .addOnSuccessListener((response) -> {
                    Place place = response.getPlace();
                    String placeType = "";

                    // Get the first place type and format it
                    if (place.getPlaceTypes() != null) {
                        List<String> placeTypes = place.getPlaceTypes();
                        Log.d("PlaceTypes", "Place types: " + placeTypes.toString());

                        if (!placeTypes.isEmpty()) {
                            for (String type : placeTypes) {
                                placeType = getFormattedPlaceType(type);
                                if (placeType != null) {
                                    break; // Exit loop when a valid formatted type is found
                                }
                            }

                            if (placeType == null) {
                                placeType = "📍 Place"; // Default if none of the types are recognized
                            }
                        }
                    }




                    // Format the place information
                    String placeInfo = placeType + ": " + place.getName();

                    // Add to the displayed list
                    placesData.add("★ " + placeInfo);
                    placesAdapter.notifyDataSetChanged();

                    Log.d("PlaceDetails", "Successfully fetched: " + placeInfo);
                })
                .addOnFailureListener((exception) -> {
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        Log.e("PlaceDetails", "Place not found: " + apiException.getStatusCode());

                        // Add a placeholder entry if the place couldn't be found
                        placesData.add("★ Unknown Place (ID: " + placeId + ")");
                        placesAdapter.notifyDataSetChanged();
                    }
                });
    }
    private void toggleSelectedPlaces() {
        if (showingSelectedPlaces) {
            // Show all places
            placesData.clear();
            placesData.addAll(allPlacesData);
            placesAdapter.notifyDataSetChanged();
            showMandatoryButton.setText("Show Selected");
            showingSelectedPlaces = false;
        } else {
            // Show only selected places
            if (allPlacesData.isEmpty()) {
                allPlacesData.addAll(placesData);
                Log.d("ToggleDebug", "Backed up placesData to allPlacesData");
            }

            // Clear the list to show only selected places
            placesData.clear();

            // Check if we have any selected place IDs
            if (selectedPlaceIds == null || selectedPlaceIds.isEmpty()) {
                placesData.add("No places selected for this trip");
                placesAdapter.notifyDataSetChanged();
            } else {
                // Show a loading message while we fetch place details
                placesData.add("Loading selected places...");
                placesAdapter.notifyDataSetChanged();

                // Clear the list again to remove the loading message once we get real data
                placesData.clear();

                // Fetch details for each selected place ID
                for (String placeId : selectedPlaceIds) {
                    fetchPlaceById(placeId);
                }
            }

            showMandatoryButton.setText("Show All Places");
            showingSelectedPlaces = true;
        }
    }
    private void fetchWeatherInfo() {
        executorService.execute(() -> {
            try {
                // 0️⃣ Read trip start date from Intent (format “YYYY-MM-DD”)
                String startDateStr = getIntent().getStringExtra("start_date");
                int month;
                try {
                    month = Integer.parseInt(startDateStr.split("-")[1]); // “01”→1 … “12”→12
                } catch (Exception e) {
                    month = java.time.LocalDate.now().getMonthValue(); // fallback to current month
                }

                // 1️⃣ Build the Open-Meteo climate API URL
                String apiUrl = "https://climate-api.open-meteo.com/v1/climate" +
                        "?latitude=" + latitude +
                        "&longitude=" + longitude +
                        "&start_date=1991-01-01" +
                        "&end_date=2020-12-31" +
                        "&model=ERA5" +
                        "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum" +
                        "&temperature_unit=celsius" +
                        "&precipitation_unit=mm" +
                        "&format=json";

                // 2️⃣ Fetch & parse JSON
                String jsonResponse = makeHttpRequest(apiUrl);
                JSONObject root = new JSONObject(jsonResponse);
                JSONObject daily = root.getJSONObject("daily");

                JSONArray tempsMax = daily.getJSONArray("temperature_2m_max");
                JSONArray tempsMin = daily.getJSONArray("temperature_2m_min");
                JSONArray precs = daily.getJSONArray("precipitation_sum");

                // 3️⃣ Calculate average values for the target month
                double sumTempMax = 0;
                double sumTempMin = 0;
                double sumPrecip = 0;
                int count = 0;

                for (int i = 0; i < tempsMax.length(); i++) {
                    // Assuming the dates are in order and correspond to the data arrays
                    String dateStr = daily.getJSONArray("time").getString(i);
                    int dataMonth = Integer.parseInt(dateStr.split("-")[1]);
                    if (dataMonth == month) {
                        sumTempMax += tempsMax.getDouble(i);
                        sumTempMin += tempsMin.getDouble(i);
                        sumPrecip += precs.getDouble(i);
                        count++;
                    }
                }

                double avgTempMax = sumTempMax / count;
                double avgTempMin = sumTempMin / count;
                double avgPrecip = sumPrecip / count;

                // 4️⃣ Format for display
                String monthName = new java.text.DateFormatSymbols()
                        .getMonths()[month - 1];
                final String display = String.format(
                        "%s averages (1991–2020):\n" +
                                "🌡 Max temp: %.1f °C\n" +
                                "🌡 Min temp: %.1f °C\n" +
                                "💧 Precipitation: %.1f mm",
                        monthName, avgTempMax, avgTempMin, avgPrecip
                );

                // 5️⃣ Push to UI
                runOnUiThread(() -> weatherInfo.setText(display));

            } catch (Exception e) {
                Log.e(TAG, "Error fetching historical climate data", e);
                runOnUiThread(() ->
                        weatherInfo.setText("Unable to fetch climate averages"));
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

        // Set up activities list RecyclerView
        activitiesList.setLayoutManager(new LinearLayoutManager(this));
        placesData = new ArrayList<>();

        // Show loading indicator
        setupPlacesLoading();

        // Fetch places for the given categories
        fetchPlacesForCategories(categories);
    }

    private void checkForEmptyResults() {
        runOnUiThread(() -> {
            if (placesData.isEmpty() || (placesData.size() == 1 && placesData.get(0).contains("Loading"))) {
                placesData.clear();
                placesData.add("No places found in this city. Try adjusting your categories.");
                placesAdapter.notifyDataSetChanged();
            }
        });
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


    private void saveTripPlan() {
        final String destination = destinationValue.getText().toString();
        final String duration = durationValue.getText().toString();
        final String activities;

        if (activitiesListData != null && !activitiesListData.isEmpty()) {
            activities = TextUtils.join(", ", activitiesListData);
        } else {
            activities = "No activities planned";
        }

        final String weather = weatherInfo.getText().toString();

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

        boolean notificationsEnabled = sharedPreferences.getBoolean(NOTIFICATION_PREF, false);
        if (notificationsEnabled && startDate != null && !startDate.isEmpty()) {
            scheduleNotifications(startDate);
        }

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

    private class ActivityCategoryAdapter extends RecyclerView.Adapter<ActivityCategoryAdapter.ViewHolder> {
        private ArrayList<String> categories;

        public ActivityCategoryAdapter(ArrayList<String> categories) {
            this.categories = categories;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(
                    R.layout.item_activity_category, parent, false);
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

    private class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
        private ArrayList<String> activities;

        public ActivityAdapter(ArrayList<String> activities) {
            this.activities = activities;
        }


        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(
                    R.layout.place_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String placeInfo = activities.get(position);
            holder.textView.setText(placeInfo);

            // If it's a mandatory place (has star emoji), highlight it with blue
            if (placeInfo.contains("🌟")) {
                holder.textView.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                holder.textView.setTypeface(null, Typeface.BOLD);
                holder.textView.setBackgroundResource(R.drawable.mandatory_place_background);
                holder.textView.setPadding(16, 12, 16, 12);
            }
            // Your existing highlighting code
            else if (placeInfo.contains("📸") || placeInfo.contains("🍽") ||
                    placeInfo.contains("🌳") || placeInfo.contains("🏛")) {
                holder.textView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            }
        }

        @Override
        public int getItemCount() {
            return activities.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.place_name);
            }
        }
    }

    private void getBoundariesFromPlacesAPI(String cityName) {
        if (!Places.isInitialized()) {
            Places.initialize(this, "AIzaSyCYnYiiqrHO0uwKoxNQLA_mKEIuX1aRyL4");
        }

        PlacesClient placesClient = Places.createClient(this);

        // Create a FindAutocompletePredictionsRequest for the city
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(cityName)
                .setTypeFilter(TypeFilter.CITIES)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener((response) -> {
                    if (response.getAutocompletePredictions().size() > 0) {
                        String placeId = response.getAutocompletePredictions().get(0).getPlaceId();

                        // Get the place details to retrieve the viewport bounds
                        List<Place.Field> placeFields = Arrays.asList(Place.Field.VIEWPORT);
                        FetchPlaceRequest placeRequest = FetchPlaceRequest.builder(placeId, placeFields).build();

                        placesClient.fetchPlace(placeRequest)
                                .addOnSuccessListener((fetchPlaceResponse) -> {
                                    Place place = fetchPlaceResponse.getPlace();
                                    if (place.getViewport() != null) {
                                        // Use the precise city bounds
                                        cityBounds = place.getViewport();

                                        // Create RectangularBounds for LocationBias
                                        rectangularBounds = RectangularBounds.newInstance(
                                                cityBounds.southwest,
                                                cityBounds.northeast
                                        );

                                        Log.d("Geocoding", "Retrieved precise city bounds for " + cityName);
                                    } else {
                                        // Fall back to the approximate method
                                    }
                                })
                                .addOnFailureListener((exception) -> {
                                    Log.e("Geocoding", "Place not found: " + exception.getMessage());
                                });
                    } else {
                        Log.e("Geocoding", "No predictions found for " + cityName);
                    }
                })
                .addOnFailureListener((exception) -> {
                    Log.e("Geocoding", "Prediction fetching failed: " + exception.getMessage());
                });
    }


    private void setupPlacesLoading() {
        placesData.clear();
        placesData.add("Loading places for your trip...");
        placesAdapter = new ActivityAdapter(placesData);
        activitiesList.setAdapter(placesAdapter);
    }

    private void fetchPlacesForCategories(ArrayList<String> selectedCategories) {
        placesData.clear();
        getBoundariesFromPlacesAPI(city);

        // Create an empty list to store the specific types to search for
        List<String> typesToSearch = new ArrayList<>();


        for (String category : selectedCategories) {
            Log.d("SelectedCategory", category.toLowerCase());
            switch (category) {
                case "🏛 Museum":
                    typesToSearch.add("museum");
                    break;
                case "📸 Tourist Attraction":
                    typesToSearch.add("landmark");
                    typesToSearch.add("historical_landmark");
                    typesToSearch.add("historical_site");
                    typesToSearch.add("tourist_attraction");
                    break;
                case "🍽 Restaurant":
                    typesToSearch.add("restaurant");
                    break;
                case "☕ Cafe":
                    typesToSearch.add("cafe");
                    break;
                case "🍹 Bar":
                    typesToSearch.add("bar");
                    break;
                case "🛍 Shopping Mall":
                    typesToSearch.add("shopping_mall");
                    break;
                case "🎭 Theater":
                    typesToSearch.add("theater");
                    break;
                case "🎬 Cinema":
                    typesToSearch.add("movie_theater");
                    break;
                case "🎶 Night Club":
                    typesToSearch.add("night_club");
                    break;
                case "🌳 Park":
                    typesToSearch.add("park");
                    break;
                case "🏖 Beach":
                    typesToSearch.add("beach");
                    break;
                case "🏞 Nature Spot":
                    typesToSearch.add("natural_feature");
                    break;
                case "🖼 Art Gallery":
                    typesToSearch.add("art_gallery");
                    break;
                case "🙏 Place of Worship":
                    typesToSearch.add("place_of_worship");
                    typesToSearch.add("church");
                    typesToSearch.add("hindu_temple");
                    typesToSearch.add("mosque");
                    typesToSearch.add("synagogue");
                    break;
                case "🦁 Zoo":
                    typesToSearch.add("zoo");
                    break;
                case "🐠 Aquarium":
                    typesToSearch.add("aquarium");
                    break;
                case "🎢 Amusement Park":
                    typesToSearch.add("amusement_park");
                    break;
                case "🚂 Train Station":
                    typesToSearch.add("train_station");
                    break;
                case "🚇 Metro Station":
                    typesToSearch.add("subway_station");
                    break;
            }
        }

        // Fetch places for each specific type
        for (String placeType : typesToSearch) {
            fetchPlacesOfType(placeType);
        }

        // Schedule to check results
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            checkForEmptyResults();
            // Update the UI
            runOnUiThread(() -> {
                if (!placesData.isEmpty() && allPlacesData.isEmpty()) {
                    allPlacesData.addAll(placesData);
                }
            });
        }, 5, TimeUnit.SECONDS);
    }

    private String getFormattedPlaceType(String type) {
        switch (type) {
            // High-value tourist categories
            case "tourist_attraction": return "📸 Tourist Attraction";
            case "museum": return "🏛 Museum";
            case "landmark": return "🏛 Landmark";
            case "historical_landmark":
            case "historical_site": return "🏰 Historical Site";

            // Food and drink
            case "restaurant": return "🍽 Restaurant";
            case "cafe": return "☕ Cafe";
            case "bar": return "🍹 Bar";

            // Entertainment
            case "shopping_mall": return "🛍 Shopping Mall";
            case "theater": return "🎭 Theater";
            case "movie_theater": return "🎬 Cinema";
            case "night_club": return "🎶 Night Club";

            // Nature and outdoors
            case "park": return "🌳 Park";
            case "beach": return "🏖 Beach";
            case "natural_feature": return "🏞 Nature Spot";

            // Cultural sites
            case "art_gallery": return "🖼 Art Gallery";
            case "place_of_worship":
            case "church":
            case "hindu_temple":
            case "mosque":
            case "synagogue": return "🙏 Place of Worship";

            // Family attractions
            case "zoo": return "🦁 Zoo";
            case "aquarium": return "🐠 Aquarium";
            case "amusement_park": return "🎢 Amusement Park";

            // Only include transportation that tourists need
            case "train_station": return "🚂 Train Station";
            case "subway_station": return "🚇 Metro Station";

            default: return null; // Filter out other categories
        }
    }

    private void fetchPlacesOfType(String placeType) {
        // Skip place types that don't match our formatted list
        String formattedType = getFormattedPlaceType(placeType);
        if (formattedType == null) {
            return;
        }

        // Fix: Use rectangularBounds (which is RectangularBounds) as LocationBias
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationBias(rectangularBounds) // Changed from cityBounds to rectangularBounds
                .setTypesFilter(Arrays.asList(placeType))
                .setQuery(city)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener((response) -> {
                    // Limit to 2 results per type to manage quota
                    int count = 0;
                    for (com.google.android.libraries.places.api.model.AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                        if (count >= 2) break;

                        String placeName = prediction.getPrimaryText(null).toString();

                        // Use our formatted place type instead of raw type
                        String placeEntry = formattedType + ": " + placeName;

                        placesData.add(placeEntry);
                        count++;
                    }

                    // Update the UI with the places we found
                    runOnUiThread(() -> {
                        if (placesAdapter == null) {
                            placesAdapter = new ActivityAdapter(placesData);
                            activitiesList.setAdapter(placesAdapter);
                        } else {
                            placesAdapter.notifyDataSetChanged();
                        }
                    });
                })
                .addOnFailureListener((exception) -> {
                    if (exception instanceof ApiException) {
                        Log.e(TAG, "Place lookup failed: " + exception.getMessage());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
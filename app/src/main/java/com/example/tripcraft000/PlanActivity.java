package com.example.tripcraft000;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
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
    private ActivityAdapter activitiesAdapter;
    private ExecutorService executorService;
    private OkHttpClient httpClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private PlacesClient placesClient;
    private ArrayList<String> placesData = new ArrayList<>();
    private ActivityAdapter placesAdapter;
    private LatLngBounds cityBounds;
    private RectangularBounds rectangularBounds;
    private Button showMandatoryButton;
    private ArrayList<String> mandatoryPlaceIds;
    private boolean showingMandatoryPlaces = false;
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
        showMandatoryButton.setOnClickListener(v -> toggleMandatoryPlaces());

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
        city = intent.getStringExtra("selected_city");
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);
        ArrayList<String> selectedCategories = intent.getStringArrayListExtra("selected_categories");
        mandatoryPlaceIds = intent.getStringArrayListExtra("mandatory_place_ids");

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

    private void toggleMandatoryPlaces() {
        if (showingMandatoryPlaces) {
            // Switch back to all places
            placesData.clear();
            placesData.addAll(allPlacesData);
            placesAdapter.notifyDataSetChanged();
            showMandatoryButton.setText("Show Must Visit");
            showingMandatoryPlaces = false;
        } else {
            // First time switching to mandatory, back up all places
            if (allPlacesData.isEmpty() && !placesData.isEmpty()) {
                allPlacesData.addAll(placesData);
            }

            // Show only mandatory places
            ArrayList<String> mandatoryPlaces = new ArrayList<>();
            for (String placeId : mandatoryPlaceIds) {
                // Find the place in allPlacesData
                for (String placeInfo : allPlacesData) {
                    if (placeInfo.contains(placeId)) {
                        mandatoryPlaces.add("🌟 " + placeInfo);
                        break;
                    }
                }
            }

            // If no mandatory places found
            if (mandatoryPlaces.isEmpty()) {
                mandatoryPlaces.add("No must-visit places selected for this trip");
            }

            placesData.clear();
            placesData.addAll(mandatoryPlaces);
            placesAdapter.notifyDataSetChanged();
            showMandatoryButton.setText("Show All Places");
            showingMandatoryPlaces = true;
        }
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

    // Adapter for displaying chosen activity categories
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

    // Adapter for displaying activities
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

    private void calculateCityBounds() {
        // Approximate city boundaries based on center coordinates
        // This is a simplified approach; a real implementation would use geocoding to get precise city bounds
        double radius = 5000; // 5km radius around city center
        double latRadian = Math.toRadians(latitude);

        // Calculate approximate lat/lng deltas for the radius
        double latDelta = radius / 111320.0; // 1 degree of latitude is approximately 111.32 km
        double lngDelta = radius / (111320.0 * Math.cos(latRadian));

        LatLng southwest = new LatLng(latitude - latDelta, longitude - lngDelta);
        LatLng northeast = new LatLng(latitude + latDelta, longitude + lngDelta);

        cityBounds = new LatLngBounds(southwest, northeast);

        // Create RectangularBounds for LocationBias (fixing the type mismatch)
        rectangularBounds = RectangularBounds.newInstance(
                southwest,
                northeast
        );
    }

    private void setupPlacesLoading() {
        placesData.clear();
        placesData.add("Loading places for your trip...");
        placesAdapter = new ActivityAdapter(placesData);
        activitiesList.setAdapter(placesAdapter);
    }

    private void fetchPlacesForCategories(ArrayList<String> selectedCategories) {
        placesData.clear();
        calculateCityBounds();

        // Define all place types we want to search for using PlaceTypes instead of deprecated Place.Type
        List<String> allPlaceTypes = Arrays.asList(
                // Tourist attractions
                PlaceTypes.TOURIST_ATTRACTION, PlaceTypes.MUSEUM, "landmark",

                // Food and drink
                PlaceTypes.RESTAURANT, PlaceTypes.CAFE, PlaceTypes.BAR,

                // Entertainment
                PlaceTypes.SHOPPING_MALL, PlaceTypes.MOVIE_THEATER, "night_club",

                // Nature and outdoors
                PlaceTypes.PARK, "beach", "natural_feature",

                // Cultural sites
                PlaceTypes.ART_GALLERY, "place_of_worship", PlaceTypes.CHURCH,
                "hindu_temple", "mosque", "synagogue",

                // Family attractions
                PlaceTypes.ZOO, PlaceTypes.AQUARIUM, PlaceTypes.AMUSEMENT_PARK,

                // Transportation
                PlaceTypes.TRAIN_STATION, PlaceTypes.SUBWAY_STATION
        );

        // Filter the types based on user selected categories
        List<String> typesToSearch = new ArrayList<>();
        for (String category : selectedCategories) {
            switch (category.toLowerCase()) {
                case "food":
                    typesToSearch.add(PlaceTypes.RESTAURANT);
                    typesToSearch.add(PlaceTypes.CAFE);
                    typesToSearch.add(PlaceTypes.BAR);
                    break;
                case "attractions":
                    typesToSearch.add(PlaceTypes.TOURIST_ATTRACTION);
                    typesToSearch.add(PlaceTypes.MUSEUM);
                    typesToSearch.add("landmark");
                    typesToSearch.add("historical_landmark");
                    break;
                case "shopping":
                    typesToSearch.add(PlaceTypes.SHOPPING_MALL);
                    break;
                case "outdoors":
                    typesToSearch.add(PlaceTypes.PARK);
                    typesToSearch.add("beach");
                    typesToSearch.add("natural_feature");
                    break;
                case "entertainment":
                    typesToSearch.add(PlaceTypes.MOVIE_THEATER);
                    typesToSearch.add("theater");
                    typesToSearch.add("night_club");
                    typesToSearch.add(PlaceTypes.AMUSEMENT_PARK);
                    break;
                case "culture":
                    typesToSearch.add(PlaceTypes.ART_GALLERY);
                    typesToSearch.add("place_of_worship");
                    typesToSearch.add(PlaceTypes.CHURCH);
                    typesToSearch.add("hindu_temple");
                    typesToSearch.add("mosque");
                    typesToSearch.add("synagogue");
                    break;
                case "family":
                    typesToSearch.add(PlaceTypes.ZOO);
                    typesToSearch.add(PlaceTypes.AQUARIUM);
                    typesToSearch.add(PlaceTypes.AMUSEMENT_PARK);
                    break;
                case "transit":
                    typesToSearch.add(PlaceTypes.TRAIN_STATION);
                    typesToSearch.add(PlaceTypes.SUBWAY_STATION);
                    break;
            }
        }


        Set<String> uniqueTypes = new HashSet<>(typesToSearch);

        for (String placeType : uniqueTypes) {
            fetchPlacesOfType(placeType);
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            checkForEmptyResults();

            // Save all places data for toggling
            runOnUiThread(() -> {
                if (!placesData.isEmpty() && allPlacesData.isEmpty()) {
                    allPlacesData.addAll(placesData);
                }
            });
        }, 5, TimeUnit.SECONDS);
    }

    // Add this method for formatting place types
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
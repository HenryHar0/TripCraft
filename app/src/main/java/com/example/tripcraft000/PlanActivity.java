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
import androidx.privacysandbox.tools.core.model.Method;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;



import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;



import com.android.volley.toolbox.Volley;
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
                long days = differenceInMillis / (1000 * 60 * 60 * 24) + 1;
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

    private void getBoundariesFromNominatim(String cityName, final Runnable onComplete) {
        Log.d("Geocoding", "Initializing Nominatim API request for city: " + cityName);

        // Create a URL for the Nominatim API request
        String encodedCityName;
        try {
            encodedCityName = URLEncoder.encode(cityName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("Geocoding", "Error encoding city name: " + e.getMessage());
            return;
        }

        // Build the URL with parameters
        String url = "https://nominatim.openstreetmap.org/search" +
                "?format=json" +
                "&q=" + encodedCityName +
                "&limit=1" +
                "&addressdetails=1" +
                "&polygon_geojson=0" +
                "&bounded=1" +
                "&featuretype=city";

        Log.d("Geocoding", "Making request to Nominatim API: " + url);

        // Create OkHttp client
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // Build the request with proper headers
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "YourAppName/1.0 (your@email.com)") // Required by Nominatim's usage policy
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Geocoding", "Nominatim API request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("Geocoding", "Unexpected code: " + response);
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);

                    // Switch to main thread for UI updates
                    runOnUiThread(() -> {
                        try {
                            Log.d("Geocoding", "Response received from Nominatim API");

                            if (jsonArray.length() > 0) {
                                JSONObject place = jsonArray.getJSONObject(0);

                                // Extract the bounding box coordinates
                                JSONArray boundingBox = place.getJSONArray("boundingbox");
                                double south = Double.parseDouble(boundingBox.getString(0));
                                double north = Double.parseDouble(boundingBox.getString(1));
                                double west = Double.parseDouble(boundingBox.getString(2));
                                double east = Double.parseDouble(boundingBox.getString(3));

                                Log.d("Geocoding", "Bounding box for " + cityName + ": S=" + south +
                                        ", N=" + north + ", W=" + west + ", E=" + east);

                                // Create LatLngBounds similar to Google Maps viewport
                                LatLng southwest = new LatLng(south, west);
                                LatLng northeast = new LatLng(north, east);
                                cityBounds = new LatLngBounds(southwest, northeast);

                                rectangularBounds = RectangularBounds.newInstance(cityBounds.southwest, cityBounds.northeast);

                                Log.d("Geocoding", "rectangularbounds: " + rectangularBounds);

                                Log.d("Geocoding", "City bounds and rectangular bounds created successfully");

                                // Call the callback function (fetchPlacesOfType) once the bounds are set
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            } else {
                                Log.e("Geocoding", "No results found for " + cityName);
                            }
                        } catch (JSONException e) {
                            Log.e("Geocoding", "Error parsing Nominatim response: " + e.getMessage());
                        }
                    });
                } catch (JSONException e) {
                    Log.e("Geocoding", "Error parsing JSON: " + e.getMessage());
                }
            }
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
        List<String> typesToSearch = new ArrayList<>();

        for (String category : selectedCategories) {
            Log.d("SelectedCategory", category.toLowerCase());
            switch (category) {
                case "Museum":
                    typesToSearch.add("museum");
                    break;
                case "Tourist Attraction":
                    typesToSearch.add("tourist_attraction");
                    break;
                case "Restaurant":
                    typesToSearch.add("restaurant");
                    break;
                case "Cafe":
                    typesToSearch.add("cafe");
                    typesToSearch.add("bakery");
                    break;
                case "Bar":
                    typesToSearch.add("bar");
                    break;
                case "Shopping Mall":
                    typesToSearch.add("shopping_mall");
                    break;
                case "Theater":
                    typesToSearch.add("concert_hall");
                    typesToSearch.add("performing_arts_theater");
                    break;
                case "Cinema":
                    typesToSearch.add("movie_theater");
                    break;
                case "Night Club":
                    typesToSearch.add("night_club");
                    break;
                case "Park":
                    typesToSearch.add("park");
                    break;
                case "Beach":
                    typesToSearch.add("beach");
                    break;
                case "Art Gallery":
                    typesToSearch.add("art_gallery");
                    break;
                case "Place of Worship":
                    typesToSearch.add("church");
                    typesToSearch.add("hindu_temple");
                    typesToSearch.add("mosque");
                    typesToSearch.add("synagogue");
                    break;
                case "Zoo":
                    typesToSearch.add("zoo");
                    break;
                case "Aquarium":
                    typesToSearch.add("aquarium");
                    break;
                case "Amusement Park":
                    typesToSearch.add("amusement_park");
                    typesToSearch.add("bowling_alley");
                    break;
            }
        }

        for (String placeType : typesToSearch) {
            fetchPlacesOfType(placeType);
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            checkForEmptyResults();
            runOnUiThread(() -> {
                if (!placesData.isEmpty() && allPlacesData.isEmpty()) {
                    allPlacesData.addAll(placesData);
                }
            });
        }, 5, TimeUnit.SECONDS);
    }


    private String getFormattedPlaceType(String type) {
        switch (type) {
            // Cultural & tourist attractions
            case "museum":
                return "Museum";
            case "tourist_attraction":
                return "Tourist Attraction";
            case "art_gallery":
                return "Art Gallery";
            case "church":
            case "hindu_temple":
            case "mosque":
            case "synagogue":
                return "Place of Worship";

            // Food and drink
            case "restaurant":
                return "Restaurant";
            case "cafe":
            case "bakery":
                return "Cafe";
            case "bar":
                return "Bar";

            // Entertainment & leisure
            case "shopping_mall":
                return "Shopping Mall";
            case "concert_hall":
            case "performing_arts_theater":
                return "Theater";
            case "movie_theater":
                return "Cinema";
            case "night_club":
                return "Night Club";
            case "amusement_park":
            case "bowling_alley":
                return "Amusement Park";

            // Nature & outdoor
            case "park":
                return "Park";
            case "beach":
                return "Beach";

            // Family-friendly
            case "zoo":
                return "Zoo";
            case "aquarium":
                return "Aquarium";

            // Default for unknown or unsupported types
            default:
                return null;
        }
    }



    private static final String TAG1 = "PlaceFetchDebug";
    private static final int MAX_RESULTS_PER_TYPE = 20;

    private List<PlaceData> placeDataList = new ArrayList<>();
    private PlaceAdapter1 placeAdapter1;

    private void fetchPlacesOfType(String placeType) {
        Log.d(TAG1, "Fetching places for type: " + placeType);

        // 1. Format the place type (e.g. "restaurant" → "restaurant")
        String formattedType = getFormattedPlaceType(placeType);
        if (formattedType == null) {
            Log.w(TAG1, "Skipping unformatted place type: " + placeType);
            return;
        }

        // 2. First, get city boundaries via Nominatim (unchanged)
        getBoundariesFromNominatim(city, () -> {
            LatLng centerLatLng = getCenterFromBounds(rectangularBounds);
            double lat = centerLatLng.latitude;
            double lng = centerLatLng.longitude;

            // 3. Compute radius in meters based on bounding box
            int radius = calculateRadiusFromBounds(rectangularBounds);
            // Cap the radius to Google's maximum allowed (50,000 meters)
            radius = Math.min(radius, 50000);
            Log.d(TAG1, "Calculated radius: " + radius + " meters");

            // 4. Build the POST URL (with API key as a query param)
            String apiKey = getString(R.string.google_api_key);
            String url = "https://places.googleapis.com/v1/places:searchNearby"
                    + "?key=" + apiKey
                    + "&fields=places.id,places.displayName.text,places.formattedAddress,places.rating,places.location,places.photos";



            // 5. Build the JSON request body
            JSONObject bodyJson = new JSONObject();
            try {
                // locationRestriction.circle
                JSONObject center = new JSONObject()
                        .put("latitude", lat)
                        .put("longitude", lng);
                JSONObject circle = new JSONObject()
                        .put("center", center)
                        .put("radius", (double) radius);
                JSONObject locationRestriction = new JSONObject()
                        .put("circle", circle);

                bodyJson.put("locationRestriction", locationRestriction);
                bodyJson.put("includedTypes", new JSONArray().put(formattedType.toLowerCase())); // Google uses lowercase types
                bodyJson.put("maxResultCount", MAX_RESULTS_PER_TYPE);

            } catch (JSONException e) {
                Log.e(TAG1, "Failed to build JSON body", e);
                return;
            }


            Log.d(TAG1, "SearchNearby POST body: " + bodyJson.toString());

            // 6. Send the POST request with OkHttp
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(
                    bodyJson.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG1, "Places:searchNearby API call failed", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG1, "Unexpected response code: " + response.code());
                        // Print the response body to debug
                        if (response.body() != null) {
                            String errorBody = response.body().string();
                            Log.e(TAG1, "Error response: " + errorBody);
                        }
                        return;
                    }

                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray places = json.optJSONArray("places");

                        if (places == null || places.length() == 0) {
                            Log.w(TAG1, "No places found for type: " + formattedType);
                            return;
                        }

                        List<PlaceData> newPlaces = new ArrayList<>();
                        int count = 0;

                        for (int i = 0; i < places.length() && count < MAX_RESULTS_PER_TYPE; i++) {
                            JSONObject place = places.getJSONObject(i);

                            String placeId = place.getString("id");  // ✅ NEW field
                            String name = place.getJSONObject("displayName").getString("text");  // ✅ NEW structure
                            String address = place.optString("formattedAddress", "No address available");
                            float rating = (float) place.optDouble("rating", 0.0);

                            JSONObject loc = place.getJSONObject("location");
                            LatLng placeLatLng = new LatLng(
                                    loc.getDouble("latitude"),
                                    loc.getDouble("longitude")
                            );


                            // build PlaceData
                            PlaceData pd = new PlaceData(placeId, name, address, rating, placeLatLng, formattedType);

                            if (place.has("photos")) {
                                JSONArray photos = place.getJSONArray("photos");
                                for (int j = 0; j < photos.length(); j++) {
                                    String ref = photos.getJSONObject(j).getString("name");
                                    String photoUrl = "https://places.googleapis.com/v1/" + ref + "/media?key=" + R.string.google_api_key;
                                    pd.addPhotoReference(photoUrl);
                                    Log.d(TAG1, "Photo URL for " + name + ": " + photoUrl);  // Debug the URL
                                }
                            } else {
                                pd.addPhotoReference("default_placeholder");
                            }


                            newPlaces.add(pd);
                            Log.d(TAG1, "Place found: " + formattedType + ": " + name);
                            count++;
                        }

                        if (newPlaces.isEmpty()) {
                            Log.w(TAG1, "No places added for type: " + formattedType);
                        } else {
                            // merge into main list
                            synchronized (placeDataList) {
                                placeDataList.addAll(newPlaces);
                            }
                        }

                        // 7. Update UI on main thread
                        runOnUiThread(() -> {
                            if (placeAdapter1 == null) {
                                placeAdapter1 = new PlaceAdapter1(placeDataList, apiKey);
                                activitiesList.setAdapter(placeAdapter1);
                                activitiesList.setLayoutManager(
                                        new LinearLayoutManager(getApplicationContext())
                                );
                            } else {
                                placeAdapter1.updatePlaces(placeDataList);
                            }
                        });

                    } catch (JSONException e) {
                        Log.e(TAG1, "JSON parse error", e);
                    }
                }
            });
        });
    }


    /**
     * Calculates the radius based on rectangular bounds to ensure coverage of the entire area.
     * The radius is calculated as the distance from the center to the farthest corner of the bounds.
     *
     * @param bounds The rectangular bounds of the area
     * @return The calculated radius in meters
     */
    private int calculateRadiusFromBounds(RectangularBounds bounds) {
        LatLng center = getCenterFromBounds(bounds);
        LatLng northeast = bounds.getNortheast();
        LatLng southwest = bounds.getSouthwest();

        // Calculate distance to all four corners and find the maximum
        double[] distances = new double[4];

        // Distance to northeast corner
        distances[0] = calculateDistance(center.latitude, center.longitude,
                northeast.latitude, northeast.longitude);

        // Distance to northwest corner
        distances[1] = calculateDistance(center.latitude, center.longitude,
                northeast.latitude, southwest.longitude);

        // Distance to southeast corner
        distances[2] = calculateDistance(center.latitude, center.longitude,
                southwest.latitude, northeast.longitude);

        // Distance to southwest corner
        distances[3] = calculateDistance(center.latitude, center.longitude,
                southwest.latitude, southwest.longitude);

        // Find the maximum distance
        double maxDistance = distances[0];
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] > maxDistance) {
                maxDistance = distances[i];
            }
        }

        // Round up to the nearest meter and ensure minimum radius
        int radius = (int) Math.ceil(maxDistance);

        // Ensure radius doesn't exceed Google Places API limit of 50,000 meters
        return Math.min(radius, 50000);
    }

    /**
     * Calculates the distance between two points using the Haversine formula.
     *
     * @param lat1 Latitude of point 1
     * @param lng1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lng2 Longitude of point 2
     * @return Distance between the points in meters
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371000; // Earth's radius in meters

        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    private LatLng getCenterFromBounds(RectangularBounds bounds) {
        double lat = (bounds.getSouthwest().latitude + bounds.getNortheast().latitude) / 2.0;
        double lng = (bounds.getSouthwest().longitude + bounds.getNortheast().longitude) / 2.0;
        return new LatLng(lat, lng);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
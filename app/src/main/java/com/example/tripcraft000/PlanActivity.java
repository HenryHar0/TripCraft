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
import java.util.Collections;
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
import com.google.android.libraries.places.api.model.PhotoMetadata;
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
import java.util.concurrent.atomic.AtomicInteger;


public class PlanActivity extends AppCompatActivity {

    private TextView planTitle, weatherInfo;
    private TextView destinationValue, durationValue;
    private RecyclerView activitiesList, chosenActivitiesRecycler;
    private Button savePlanButton, editPlanButton, backToMainButton;

    private static final String TAG = "PlanActivity";

    private String startDate, endDate, city;
    private double latitude, longitude;
    private ArrayList<String> activitiesListData;
    private ExecutorService executorService;
    private OkHttpClient httpClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private PlacesClient placesClient;
    private LatLngBounds cityBounds;
    private RectangularBounds rectangularBounds;
    private ArrayList<String> selectedPlaceIds;
    private ArrayList<Integer> hoursList = new ArrayList<>();

    // Add notification manager
    private TripNotificationManager notificationManager;
    private TripPlanStorageManager tripPlanStorageManager;
    private final AtomicInteger callcount = new AtomicInteger(0);
    private ArrayList<String> selectedCategories;

    private int days;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        // Initialize services
        executorService = Executors.newSingleThreadExecutor();
        httpClient = new OkHttpClient();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_api_key));
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

        // Setup notification permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // Use notification manager to handle permission result
                    notificationManager.handlePermissionResult(isGranted, city, startDate);
                }
        );

        // Initialize notification manager
        notificationManager = new TripNotificationManager(this, requestPermissionLauncher);

        // Initialize trip plan storage manager
        tripPlanStorageManager = new TripPlanStorageManager(this, notificationManager);

        // Retrieve intent extras
        Intent intent = getIntent();
        startDate = intent.getStringExtra("start_date");
        endDate = intent.getStringExtra("end_date");
        city = intent.getStringExtra("city");
        latitude = intent.getDoubleExtra("city_lat", 0.0);
        longitude = intent.getDoubleExtra("city_lng", 0.0);
        selectedCategories = intent.getStringArrayListExtra("selected_categories");
        selectedPlaceIds = intent.getStringArrayListExtra("selected_place_ids");
        hoursList = intent.getIntegerArrayListExtra("hours_per_day");

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

        // Check if first time user using notification manager
        notificationManager.checkFirstTimeUser(city, startDate);

    }

    private void fetchPlaceById(String placeId) {
        // Define the fields to return
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.TYPES,
                Place.Field.RATING,
                Place.Field.LAT_LNG,
                Place.Field.USER_RATINGS_TOTAL,
                Place.Field.PHOTO_METADATAS
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
                                placeType = getFormattedPlaceType(type).toString();
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
                    Log.d("PlaceDetails", "Successfully fetched: " + placeInfo);

                    // Create a PlaceData object for the selected place
                    // Assume a default time spent of 2 hours (adjust as needed)
                    int defaultTimeSpent = 2;
                    PlaceData selectedPlace = new PlaceData(
                            place.getId(),
                            place.getName(),
                            place.getAddress() != null ? place.getAddress() : "No address available",
                            place.getRating() != null ? place.getRating().floatValue() : 0.0f,
                            place.getLatLng(),
                            placeType,
                            place.getUserRatingsTotal() != null ? place.getUserRatingsTotal() : 0,
                            defaultTimeSpent
                    );

                    // Add photos if available
                    if (place.getPhotoMetadatas() != null) {
                        String apiKey = getString(R.string.google_api_key);
                        for (PhotoMetadata photoMetadata : place.getPhotoMetadatas()) {
                            // For Places SDK, use photo reference
                            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                                    + "?maxwidth=400"
                                    + "&photo_reference=" + photoMetadata.getAttributions()
                                    + "&key=" + apiKey;
                            selectedPlace.addPhotoReference(photoUrl);
                        }
                    }

                    // Mark this place as user-selected
                    selectedPlace.setUserSelected(true);

                    // Calculate score for consistency with other places
                    float normalizedRating = selectedPlace.getRating() / 5.0f * 100.0f;
                    float reviewScore = 0;
                    if (selectedPlace.getUserRatingsTotal() > 0) {
                        reviewScore = (float) (Math.log10(selectedPlace.getUserRatingsTotal()) / 4.0 * 100.0);
                    }
                    float score = (normalizedRating + reviewScore) / 2.0f;
                    selectedPlace.setScore(score);

                    // Update the UI with the selected place
                    runOnUiThread(() -> {
                        synchronized (placeDataList) {
                            // Check if the place is already in the list and remove it to avoid duplicates
                            List<PlaceData> toRemove = new ArrayList<>();
                            for (PlaceData existingPlace : placeDataList) {
                                if (existingPlace.getPlaceId().equals(placeId)) {
                                    toRemove.add(existingPlace);
                                }
                            }
                            placeDataList.removeAll(toRemove);

                            // Add the selected place at the beginning of the list
                            placeDataList.add(0, selectedPlace);
                        }

                        // Update the adapter
                        if (placeAdapter1 == null) {
                            placeAdapter1 = new PlaceAdapter1(placeDataList, getString(R.string.google_api_key));
                            activitiesList.setAdapter(placeAdapter1);
                            activitiesList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        } else {
                            placeAdapter1.updatePlaces(placeDataList);
                        }
                    });
                })
                .addOnFailureListener((exception) -> {
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        Log.e("PlaceDetails", "Place not found: " + apiException.getStatusCode());
                    }
                });
    }


    private static final String TAG1 = "PlaceFetchDebug";
    private static final int MAX_RESULTS_PER_TYPE = 20;

    private List<PlaceData> placeDataList = new ArrayList<>();
    private PlaceAdapter1 placeAdapter1;

    private void fetchPlacesOfType(String placeType, int timeSpent) {
        Log.d(TAG1, "Fetching places for type: " + placeType);
        int currentCall = callcount.incrementAndGet();

        getBoundariesFromNominatim(city, () -> {
            LatLng centerLatLng = getCenterFromBounds(rectangularBounds);
            double lat = centerLatLng.latitude;
            double lng = centerLatLng.longitude;

            int radius = calculateRadiusFromBounds(rectangularBounds);
            radius = Math.min(radius, 50000);
            Log.d(TAG1, "Calculated radius: " + radius + " meters");

            String apiKey = getString(R.string.google_api_key);
            String url = "https://places.googleapis.com/v1/places:searchNearby"
                    + "?key=" + apiKey
                    + "&fields=places.id,places.displayName.text,places.formattedAddress,places.rating,places.location,places.photos,places.userRatingCount";

            JSONObject bodyJson = new JSONObject();
            try {
                JSONObject center = new JSONObject()
                        .put("latitude", lat)
                        .put("longitude", lng);
                JSONObject circle = new JSONObject()
                        .put("center", center)
                        .put("radius", (double) radius);
                JSONObject locationRestriction = new JSONObject()
                        .put("circle", circle);

                bodyJson.put("locationRestriction", locationRestriction);
                bodyJson.put("includedTypes", new JSONArray().put(placeType.toLowerCase()));
                bodyJson.put("maxResultCount", MAX_RESULTS_PER_TYPE);
            } catch (JSONException e) {
                Log.e(TAG1, "Failed to build JSON body", e);
                return;
            }

            Log.d(TAG1, "SearchNearby POST body: " + bodyJson.toString());

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
                            Log.w(TAG1, "No places found for type: " + placeType);
                            return;
                        }

                        List<PlaceData> newPlaces = new ArrayList<>();
                        int count = 0;

                        for (int i = 0; i < places.length() && count < MAX_RESULTS_PER_TYPE; i++) {
                            JSONObject place = places.getJSONObject(i);

                            String placeId = place.getString("id");
                            String name = place.getJSONObject("displayName").getString("text");
                            String address = place.optString("formattedAddress", "No address available");
                            float rating = (float) place.optDouble("rating", 0.0f);
                            int userRatingsTotal = place.optInt("userRatingCount", 0);

                            JSONObject loc = place.getJSONObject("location");
                            LatLng placeLatLng = new LatLng(loc.getDouble("latitude"), loc.getDouble("longitude"));

                            PlaceData pd = new PlaceData(placeId, name, address, rating, placeLatLng, placeType, userRatingsTotal, timeSpent);

                            if (place.has("photos")) {
                                JSONArray photos = place.getJSONArray("photos");
                                for (int j = 0; j < photos.length(); j++) {
                                    String ref = photos.getJSONObject(j).getString("name");
                                    String photoUrl = "https://places.googleapis.com/v1/" + ref + "/media?maxWidthPx=400&key=" + apiKey;
                                    pd.addPhotoReference(photoUrl);
                                }
                            } else {
                                pd.addPhotoReference("default_placeholder");
                            }

                            newPlaces.add(pd);
                            count++;
                        }

                        synchronized (placeDataList) {
                            placeDataList.addAll(newPlaces);
                        }

                        if (currentCall == selectedCategories.size()) {
                            int totalTime = TotalTime(days, hoursList);
                            List<PlaceData> filtered = filterPlacesByRatingAndTime(placeDataList, totalTime);

                            runOnUiThread(() -> {
                                if (placeAdapter1 == null) {
                                    placeAdapter1 = new PlaceAdapter1(filtered, apiKey);
                                    activitiesList.setAdapter(placeAdapter1);
                                    activitiesList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                                } else {
                                    placeAdapter1.updatePlaces(filtered);
                                }
                            });
                        }

                    } catch (JSONException e) {
                        Log.e(TAG1, "JSON parse error", e);
                    }
                }
            });
        });
    }

    private int TotalTime(int totalDays, ArrayList<Integer> dailyhours) {
        int totalTime = 0;
        for (int i = 0; i < totalDays && i < dailyhours.size(); i++) {
            totalTime += dailyhours.get(i);
        }
        return totalTime;
    }
    private List<PlaceData> filterPlacesByRatingAndTime(List<PlaceData> allPlacesData, int totalAvailableTime) {
        Log.d(TAG1, "Filtering places. Total places: " + allPlacesData.size() +
                ", Available time: " + totalAvailableTime + " hours");

        if (allPlacesData.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 1: Calculate a balanced score for each place
        // The score is based on both rating and number of ratings
        for (PlaceData place : allPlacesData) {
            // Calculate a normalized score (0-100) that balances rating and user ratings count
            // Places with both high ratings and many reviews get higher scores
            float normalizedRating = place.getRating() / 5.0f * 100.0f; // Convert rating to 0-100 scale

            // Logarithmic scale for number of ratings to prevent places with extremely high
            // number of ratings from completely dominating the score
            float reviewScore = 0;
            if (place.getUserRatingsTotal() > 0) {
                reviewScore = (float) (Math.log10(place.getUserRatingsTotal()) / 4.0 * 100.0);
                // Math.log10(10000) ≈ 4, so this scales logarithmically from 0-100
            }

            // Calculate final score (equal weight to rating and user ratings count)
            float score = (normalizedRating + reviewScore) / 2.0f;
            place.setScore(score); // Assuming you add a score field to PlaceData
        }

        // Step 2: Sort places by their score in descending order
        Collections.sort(allPlacesData, (p1, p2) -> Float.compare(p2.getScore(), p1.getScore()));

        // Step 3: Keep adding places until we exceed the time limit
        List<PlaceData> filteredPlaces = new ArrayList<>();
        int totalTime = 0;

        for (PlaceData place : allPlacesData) {
            if (totalTime + place.getTimeSpent() <= totalAvailableTime) {
                filteredPlaces.add(place);
                totalTime += place.getTimeSpent();
            } else {
                // Optional: If we have remaining time and this place would exceed it only slightly,
                // consider adding it anyway if it has a high score
                int remainingTime = totalAvailableTime - totalTime;
                if (remainingTime > 0 &&
                        place.getTimeSpent() - remainingTime <= 1 && // Only exceed by at most 1 hour
                        place.getScore() > 80) {                     // Only if it's a high-scoring place
                    filteredPlaces.add(place);
                    totalTime += place.getTimeSpent();
                }
            }
        }

        Log.d(TAG1, "Places filtered: " + filteredPlaces.size() +
                " places selected, Total time: " + totalTime + " hours");

        return filteredPlaces;
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


        // Fetch places for the given categories
        fetchPlacesForCategories(categories);
    }


    private void calculateDuration() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            if (start != null && end != null) {
                long differenceInMillis = end.getTime() - start.getTime();
                days = (int) (differenceInMillis / (1000 * 60 * 60 * 24)) + 1;
                durationValue.setText(days + " days");
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Error calculating duration", Toast.LENGTH_SHORT).show();
        }
    }


    private void saveTripPlan() {
        final String destination = destinationValue.getText().toString();
        final String duration = durationValue.getText().toString();
        final String weather = weatherInfo.getText().toString();

        // Use the TripPlanStorageManager to show save dialog and handle saving
        tripPlanStorageManager.showSaveTripPlanDialog(
                destination,
                duration,
                activitiesListData,
                weather,
                city,
                startDate
        );
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



    private void fetchPlacesForCategories(ArrayList<String> selectedCategories) {
        Map<String, Integer> typesToSearch = new HashMap<>();

        for (String category : selectedCategories) {
            Log.d("SelectedCategory", category.toLowerCase());
            switch (category) {
                case "Museum":
                    typesToSearch.put("museum", 2);
                    break;
                case "Tourist Attraction":
                    typesToSearch.put("tourist_attraction", 2);
                    break;
                case "Restaurant":
                    typesToSearch.put("restaurant", 1);
                    break;
                case "Cafe":
                    typesToSearch.put("cafe", 1);
                    typesToSearch.put("bakery", 1);
                    break;
                case "Bar":
                    typesToSearch.put("bar", 2);
                    break;
                case "Shopping Mall":
                    typesToSearch.put("shopping_mall", 3);
                    break;
                case "Theater":
                    typesToSearch.put("concert_hall", 2);
                    typesToSearch.put("performing_arts_theater", 2);
                    break;
                case "Cinema":
                    typesToSearch.put("movie_theater", 2);
                    break;
                case "Night Club":
                    typesToSearch.put("night_club", 3);
                    break;
                case "Park":
                    typesToSearch.put("park", 2);
                    break;
                case "Beach":
                    typesToSearch.put("beach", 3);
                    break;
                case "Art Gallery":
                    typesToSearch.put("art_gallery", 2);
                    break;
                case "Place of Worship":
                    typesToSearch.put("church", 1);
                    typesToSearch.put("hindu_temple", 1);
                    typesToSearch.put("mosque", 1);
                    typesToSearch.put("synagogue", 1);
                    break;
                case "Zoo":
                    typesToSearch.put("zoo", 3);
                    break;
                case "Aquarium":
                    typesToSearch.put("aquarium", 2);
                    break;
                case "Amusement Park":
                    typesToSearch.put("amusement_park", 4);
                    typesToSearch.put("bowling_alley", 2);
                    break;
            }
        }

        for (Map.Entry<String, Integer> entry : typesToSearch.entrySet()) {
            String placeType = entry.getKey();
            int timeSpent = entry.getValue();
            fetchPlacesOfType(placeType, timeSpent);
        }
    }


    private PlaceTypeInfo getFormattedPlaceType(String type) {
        switch (type) {
            // Cultural & tourist attractions
            case "museum":
                return new PlaceTypeInfo("Museum", R.drawable.ic_museum);
            case "tourist_attraction":
                return new PlaceTypeInfo("Tourist Attraction", R.drawable.ic_tourist_attraction);
            case "art_gallery":
                return new PlaceTypeInfo("Art Gallery", R.drawable.ic_art_gallery);
            case "church":
            case "hindu_temple":
            case "mosque":
            case "synagogue":
                return new PlaceTypeInfo("Place of Worship", R.drawable.ic_place_of_worship);

            // Food and drink
            case "restaurant":
                return new PlaceTypeInfo("Restaurant", R.drawable.ic_restaurant);
            case "cafe":
            case "bakery":
                return new PlaceTypeInfo("Cafe", R.drawable.ic_cafe);
            case "bar":
                return new PlaceTypeInfo("Bar", R.drawable.ic_bar);

            // Entertainment & leisure
            case "shopping_mall":
                return new PlaceTypeInfo("Shopping Mall", R.drawable.ic_shopping_mall);
            case "concert_hall":
            case "performing_arts_theater":
                return new PlaceTypeInfo("Theater", R.drawable.ic_theater);
            case "movie_theater":
                return new PlaceTypeInfo("Cinema", R.drawable.ic_cinema);
            case "night_club":
                return new PlaceTypeInfo("Night Club", R.drawable.ic_night_club);
            case "amusement_park":
            case "bowling_alley":
                return new PlaceTypeInfo("Amusement Park", R.drawable.ic_amusement_park);

            // Nature & outdoor
            case "park":
                return new PlaceTypeInfo("Park", R.drawable.ic_park);
            case "beach":
                return new PlaceTypeInfo("Beach", R.drawable.ic_beach);

            // Family-friendly
            case "zoo":
                return new PlaceTypeInfo("Zoo", R.drawable.ic_zoo);
            case "aquarium":
                return new PlaceTypeInfo("Aquarium", R.drawable.ic_aquarium);

            // Default for unknown types
            default:
                return null;
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
    public class PlaceTypeInfo {
        public final String name;
        public final int drawableRes;

        public PlaceTypeInfo(String name, int drawableRes) {
            this.name = name;
            this.drawableRes = drawableRes;
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
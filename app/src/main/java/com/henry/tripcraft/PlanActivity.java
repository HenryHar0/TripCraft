package com.henry.tripcraft;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
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


import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class PlanActivity extends AppCompatActivity {

    private TextView planTitle, weatherInfo;
    private TextView destinationValue, durationValue;
    private RecyclerView activitiesList,filteredList, chosenActivitiesRecycler;
    private Button savePlanButton, editPlanButton, backToMainButton;

    private static final String TAG = "PlanActivity";

    private String startDate, endDate, city;
    private double latitude, longitude;
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
    private List<PlaceData> filtered;

    private PlaceAdapter1 placeAdapter1, filteredAdapter;

    private DayByDayAdapter dayByDayAdapter;

    private List<List<PlaceData>> schedule;

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
        filteredList = findViewById(R.id.filteredPlacesList);
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

    private void DayByDayPlan() {
        runOnUiThread(() -> {
            // 1) Distribute places across days
            schedule = distributePlaces(filtered, days, hoursList);

            // 2) Prepare your RecyclerView
            RecyclerView recycler = findViewById(R.id.dayByDayPlanList);
            recycler.setLayoutManager(new LinearLayoutManager(this));

            // 3) Get your API key
            String apiKey = getString(R.string.google_api_key1);

            // 4) Set or update the DayByDayAdapter
            if (dayByDayAdapter == null) {
                // Pass 'this' (PlanActivity instance) to the adapter
                dayByDayAdapter = new DayByDayAdapter(schedule, apiKey, this);
                recycler.setAdapter(dayByDayAdapter);
            } else {
                dayByDayAdapter.updateSchedule(schedule);
            }
        });
    }

    public void showFragment(Fragment fragment) {

        // Show the fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // Add to back stack so user can navigate back
                .commit();

        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
    }



    private void fetchPlaceById(String placeId, int timeSpent) {
        String apiKey = getString(R.string.google_api_key1);

        String url = "https://places.googleapis.com/v1/places/" + placeId +
                "?fields=displayName,formattedAddress,types,rating,location,userRatingCount,photos,regularOpeningHours,websiteUri,primaryTypeDisplayName" +
                "&key=" + apiKey;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SelectedPlacesDebug", "Places API call failed for placeId: " + placeId, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("SelectedPlacesDebug", "Unexpected response code: " + response.code());
                    if (response.body() != null) {
                        String errorBody = response.body().string();
                        Log.e("SelectedPlacesDebug", "Error response: " + errorBody);
                    }
                    return;
                }

                String responseBody = response.body().string();

                try {
                    JSONObject json = new JSONObject(responseBody);

                    // Extract displayName.text
                    JSONObject displayName = json.optJSONObject("displayName");
                    String name = (displayName != null) ? displayName.optString("text", "No name available") : "No name available";

                    String address = json.optString("formattedAddress", "No address available");

                    JSONObject primaryTypeDisplayName = json.optJSONObject("primaryTypeDisplayName");
                    String placeType = "Place"; // Default fallback
                    if (primaryTypeDisplayName != null) {
                        placeType = primaryTypeDisplayName.optString("text", "Place");
                    }

                    float rating = (float) json.optDouble("rating", 0.0);
                    int userRatingsTotal = json.optInt("userRatingCount", 0);

                    // Extract website
                    String website = json.optString("websiteUri", null);
                    if (website != null && website.trim().isEmpty()) {
                        website = null;
                    }

                    // Extract opening hours
                    String openingHours = "N/A";
                    JSONObject regularOpeningHours = json.optJSONObject("regularOpeningHours");
                    if (regularOpeningHours != null) {
                        JSONArray weekdayDescriptions = regularOpeningHours.optJSONArray("weekdayDescriptions");
                        if (weekdayDescriptions != null && weekdayDescriptions.length() > 0) {
                            StringBuilder hoursBuilder = new StringBuilder();
                            for (int i = 0; i < weekdayDescriptions.length(); i++) {
                                if (i > 0) hoursBuilder.append("\n");
                                hoursBuilder.append(weekdayDescriptions.getString(i));
                            }
                            openingHours = hoursBuilder.toString();
                        }
                    }

                    JSONObject location = json.optJSONObject("location");
                    LatLng latLng = null;
                    if (location != null) {
                        double lat = location.optDouble("latitude", 0.0);
                        double lng = location.optDouble("longitude", 0.0);
                        latLng = new LatLng(lat, lng);
                    }

                    PlaceData selectedPlace = new PlaceData(
                            placeId,
                            name,
                            address,
                            rating,
                            latLng,
                            placeType,
                            userRatingsTotal,
                            timeSpent
                    );

                    // Set price level, opening hours, and website
                    selectedPlace.setOpeningHours(openingHours);
                    selectedPlace.setWebsite(website);

                    JSONArray photos = json.optJSONArray("photos");
                    if (photos != null) {
                        for (int i = 0; i < photos.length(); i++) {
                            JSONObject photo = photos.getJSONObject(i);
                            String photoName = photo.optString("name");
                            if (photoName != null && !photoName.isEmpty()) {
                                String photoUrl = "https://places.googleapis.com/v1/" + photoName +
                                        "/media?maxWidthPx=400&key=" + apiKey;
                                selectedPlace.addPhotoReference(photoUrl);
                            }
                        }
                    } else {
                        selectedPlace.addPhotoReference("default_placeholder");
                    }

                    Log.d("SelectedPlacesDebug", "Setting userSelected to TRUE for place: " + name);
                    selectedPlace.setUserSelected(true);
                    Log.d("SelectedPlacesDebug", "userSelected is now: " + selectedPlace.isUserSelected());

                    synchronized (placeDataList) {
                        // Find and remove existing place with same ID if it exists
                        PlaceData existingPlace = null;
                        for (int i = 0; i < placeDataList.size(); i++) {
                            PlaceData p = placeDataList.get(i);
                            if (p.getPlaceId().equals(placeId)) {
                                existingPlace = p;
                                placeDataList.remove(i);
                                Log.d("SelectedPlacesDebug", "REMOVED existing place: " + p.getName() + " (Selected: " + p.isUserSelected() + ")");
                                break;
                            }
                        }

                        // Add the new selected place at the beginning
                        placeDataList.add(0, selectedPlace);

                        // Log selected places after update
                        Log.d("SelectedPlacesDebug", "=== SELECTED PLACES AFTER UPDATE ===");
                        for (int i = 0; i < placeDataList.size(); i++) {
                            PlaceData p = placeDataList.get(i);
                            if (p.isUserSelected()) {
                                Log.d("SelectedPlacesDebug", "Selected place [" + i + "]: " + p.getName() +
                                        " (ID: " + p.getPlaceId() +
                                        ", TimeSpent: " + p.getTimeSpent() +
                                        ", Type: " + p.getPlaceType() + ")");
                            }
                        }
                        Log.d("SelectedPlacesDebug", "=== END SELECTED PLACES ===");
                    }

                    runOnUiThread(() -> {
                        if (placeAdapter1 != null) {
                            placeAdapter1.updatePlaces(placeDataList);
                        }
                    });

                } catch (JSONException e) {
                    Log.e("SelectedPlacesDebug", "JSON parsing error for placeId: " + placeId, e);
                }
            }
        });
    }

    private final AtomicInteger completedCalls = new AtomicInteger(0);
    private final Object placeDataLock = new Object();
    private static final int MAX_RESULTS_PER_TYPE = 20;

    private List<PlaceData> placeDataList = new ArrayList<>();

    private void fetchPlacesOfType(String placeType, int timeSpent) {
        getBoundariesFromNominatim(city, () -> {
            LatLng centerLatLng = getCenterFromBounds(rectangularBounds);
            double lat = centerLatLng.latitude;
            double lng = centerLatLng.longitude;

            int radius = calculateRadiusFromBounds(rectangularBounds);
            radius = Math.min(radius, 50000);

            String apiKey = getString(R.string.google_api_key1);
            String url = "https://places.googleapis.com/v1/places:searchNearby"
                    + "?key=" + apiKey
                    + "&fields=places.id,places.displayName.text,places.formattedAddress,places.rating,places.location,places.photos,places.userRatingCount,places.regularOpeningHours,places.websiteUri";

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
                return;
            }

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
                    Log.e("API", "Failed to fetch places for type: " + placeType, e);
                    checkIfAllCallsCompleted(); // Still need to check completion even on failure
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e("API", "Unsuccessful response for type: " + placeType + ", code: " + response.code());
                        checkIfAllCallsCompleted();
                        return;
                    }

                    String responseBody = response.body().string();

                    try {
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray places = json.optJSONArray("places");

                        if (places == null || places.length() == 0) {
                            Log.w("API", "No places found for type: " + placeType);
                            checkIfAllCallsCompleted();
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

                            // Extract website
                            String website = place.optString("websiteUri", null);
                            if (website != null && website.trim().isEmpty()) {
                                website = null;
                            }

                            // Extract opening hours
                            String openingHours = "N/A";
                            JSONObject regularOpeningHours = place.optJSONObject("regularOpeningHours");
                            if (regularOpeningHours != null) {
                                JSONArray weekdayDescriptions = regularOpeningHours.optJSONArray("weekdayDescriptions");
                                if (weekdayDescriptions != null && weekdayDescriptions.length() > 0) {
                                    StringBuilder hoursBuilder = new StringBuilder();
                                    for (int j = 0; j < weekdayDescriptions.length(); j++) {
                                        if (j > 0) hoursBuilder.append("\n");
                                        hoursBuilder.append(weekdayDescriptions.getString(j));
                                    }
                                    openingHours = hoursBuilder.toString();
                                }
                            }

                            JSONObject loc = place.getJSONObject("location");
                            LatLng placeLatLng = new LatLng(loc.getDouble("latitude"), loc.getDouble("longitude"));

                            PlaceData pd = new PlaceData(placeId, name, address, rating, placeLatLng, placeType, userRatingsTotal, timeSpent);

                            pd.setOpeningHours(openingHours);
                            pd.setWebsite(website);
                            pd.setUserSelected(false);

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

                        Log.d("API", "Fetched " + newPlaces.size() + " places for type: " + placeType);

                        // Thread-safe addition to the main list
                        synchronized (placeDataLock) {
                            placeDataList.addAll(newPlaces);

                            // Log current state
                            Map<String, Integer> typeCounts = new HashMap<>();
                            for (PlaceData pd : placeDataList) {
                                typeCounts.merge(pd.getPlaceType(), 1, Integer::sum);
                            }
                            Log.d("API", "Current placeDataList size: " + placeDataList.size());
                            Log.d("API", "Current type distribution: " + typeCounts);
                        }

                        runOnUiThread(() -> {
                            if (placeAdapter1 == null) {
                                placeAdapter1 = new PlaceAdapter1(placeDataList, apiKey);
                                activitiesList.setAdapter(placeAdapter1);
                                activitiesList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                            } else {
                                placeAdapter1.updatePlaces(placeDataList);
                            }
                        });

                        checkIfAllCallsCompleted();

                    } catch (JSONException e) {
                        Log.e("API", "JSON parsing error for type: " + placeType, e);
                        checkIfAllCallsCompleted();
                    }
                }
            });
        });
    }

    private void checkIfAllCallsCompleted() {
        int completed = completedCalls.incrementAndGet();
        Log.d("API", "Completed calls: " + completed + "/" + selectedCategories.size());

        if (completed == selectedCategories.size()) {
            Log.d("API", "All API calls completed, starting deduplication and filtering");

            synchronized (placeDataLock) {
                // Deduplicate - preserve user-selected places
                Map<String, PlaceData> uniqueMap = new LinkedHashMap<>();
                for (PlaceData pd : placeDataList) {
                    String key = pd.getPlaceId();
                    PlaceData existing = uniqueMap.get(key);

                    if (existing == null) {
                        uniqueMap.put(key, pd);
                    } else {
                        if (pd.isUserSelected()) {
                            uniqueMap.put(key, pd); // Replace with user-selected version
                        } else if (!existing.isUserSelected()) {
                            uniqueMap.put(key, pd); // Both are not user-selected, keep the last one
                        }
                    }
                }

                placeDataList.clear();
                placeDataList.addAll(uniqueMap.values());

                // Log final statistics before filtering
                Map<String, Integer> finalTypeCounts = new HashMap<>();
                for (PlaceData pd : placeDataList) {
                    finalTypeCounts.merge(pd.getPlaceType(), 1, Integer::sum);
                }
                Log.d("API", "=== FINAL DATA BEFORE FILTERING ===");
                Log.d("API", "Total places: " + placeDataList.size());
                Log.d("API", "Type distribution: " + finalTypeCounts);

                // Now filter the complete dataset
                int totalTime = TotalTime(days, hoursList);
                filtered = filterPlacesByRatingAndTime(placeDataList, totalTime);

                runOnUiThread(() -> {
                    if (filteredAdapter == null) {
                        String apiKey = getString(R.string.google_api_key1);
                        filteredAdapter = new PlaceAdapter1(filtered, apiKey);
                        filteredList.setAdapter(filteredAdapter);
                        filteredList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                    } else {
                        filteredAdapter.updatePlaces(filtered);
                    }
                });

                if (days > 0) {
                    DayByDayPlan();
                }
            }
        }
    }

    private void startFetchingAllPlaces() {
        // Reset counters
        completedCalls.set(0);
        placeDataList.clear();

        // Map to hold place types and their corresponding time spent
        Map<String, Integer> typesToSearch = new HashMap<>();

        // Populate the map based on selected categories
        for (String category : selectedCategories) {
            Log.d("SelectedCategory", category.toLowerCase());
            switch (category) {
                case "Museum":
                    typesToSearch.put("museum", 2);
                    break;
                case "Tourist Attraction":
                    typesToSearch.put("tourist_attraction", 2);
                    break;
                case "Theater":
                    typesToSearch.put("concert_hall", 2);
                    typesToSearch.put("performing_arts_theater", 2);
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

        // Fetch places by type and time
        for (Map.Entry<String, Integer> entry : typesToSearch.entrySet()) {
            String placeType = entry.getKey();
            int timeSpent = entry.getValue();

            if (selectedPlaceIds != null && !selectedPlaceIds.isEmpty()) {
                for (String id : selectedPlaceIds) {
                    fetchPlaceById(id, timeSpent);
                }
            }

            fetchPlacesOfType(placeType, timeSpent);
        }
    }




    private int TotalTime(int totalDays, ArrayList<Integer> dailyhours) {
        int totalTime = 0;
        for (int i = 0; i < totalDays && i < dailyhours.size(); i++) {
            totalTime += dailyhours.get(i);
        }
        return totalTime;
    }

    private static final float MAX_RATING = 5.0f;
    private static final float MAX_SCORE = 100.0f;
    private static final float LOG_NORMALIZATION_FACTOR = 4.0f;
    private static final float USER_SELECTION_SCORE_BOOST = 10000.0f;


    public static List<PlaceData> filterPlacesByRatingAndTime(List<PlaceData> allPlacesData, int totalAvailableTime) {
        System.out.println("=== STARTING PLACE FILTERING ===");
        System.out.println("Total available time: " + totalAvailableTime + " minutes");

        if (allPlacesData == null || allPlacesData.isEmpty()) {
            System.out.println("No places data provided - returning empty list");
            return new ArrayList<>();
        }

        System.out.println("Input places count: " + allPlacesData.size());

        computePlaceScores(allPlacesData);

        List<PlaceData> selectedPlaces = new ArrayList<>();
        List<PlaceData> nonSelectedPlaces = new ArrayList<>();
        for (PlaceData place : allPlacesData) {
            if (place.isUserSelected()) selectedPlaces.add(place);
            else nonSelectedPlaces.add(place);
        }

        System.out.println("User selected places: " + selectedPlaces.size());
        System.out.println("Non-selected places: " + nonSelectedPlaces.size());

        // Log user selected places
        if (!selectedPlaces.isEmpty()) {
            System.out.println("--- USER SELECTED PLACES ---");
            for (PlaceData p : selectedPlaces) {
                System.out.println("  " + p.getName() + " (Type: " + p.getPlaceType() + ", Time: " + p.getTimeSpent() + "min, Score: " + String.format("%.2f", p.getScore()) + ")");
            }
        }

        Map<String, List<PlaceData>> placesByType = groupPlacesByType(nonSelectedPlaces);
        sortTypeGroupsByScore(placesByType);

        // Log place types and counts
        System.out.println("--- PLACE TYPES ANALYSIS ---");
        System.out.println("Total place types found: " + placesByType.size());
        for (Map.Entry<String, List<PlaceData>> entry : placesByType.entrySet()) {
            String type = entry.getKey();
            List<PlaceData> places = entry.getValue();
            System.out.println("Type '" + type + "': " + places.size() + " places");

            // Show top 3 places per type
            int showCount = Math.min(3, places.size());
            for (int i = 0; i < showCount; i++) {
                PlaceData p = places.get(i);
                System.out.println("  #" + (i+1) + ": " + p.getName() + " (Time: " + p.getTimeSpent() + "min, Score: " + String.format("%.2f", p.getScore()) + ")");
            }
        }

        List<PlaceData> finalSelection = new ArrayList<>();
        int currentTime = 0;

        // Add user selected places first
        for (PlaceData p : selectedPlaces) {
            finalSelection.add(p);
            currentTime += p.getTimeSpent();
            System.out.println("Added user selected: " + p.getName() + " (Type: " + p.getPlaceType() + ") - Current time: " + currentTime + "min");
        }

        System.out.println("--- STARTING QUOTA BALANCED FILL (TOP 10 TYPES) ---");
        currentTime = fillByTypeQuotaBalanced(
                placesByType, finalSelection, currentTime, totalAvailableTime
        );

        System.out.println("--- STARTING LEFTOVERS FILL ---");
        fillWithLeftovers(placesByType, finalSelection, currentTime, totalAvailableTime);

        // Final analysis
        System.out.println("=== FINAL RESULTS ===");
        System.out.println("Total selected places: " + finalSelection.size());

        Map<String, Integer> finalTypeCounts = new HashMap<>();
        int totalFinalTime = 0;
        for (PlaceData p : finalSelection) {
            finalTypeCounts.merge(p.getPlaceType(), 1, Integer::sum);
            totalFinalTime += p.getTimeSpent();
        }

        System.out.println("Final time used: " + totalFinalTime + "/" + totalAvailableTime + " minutes");
        System.out.println("Final type distribution:");
        for (Map.Entry<String, Integer> entry : finalTypeCounts.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " places");
        }

        return finalSelection;
    }

    /**
     * Computes and assigns a score to each PlaceData item based on:
     * - Normalized rating (0–5 scaled to 0–100)
     * - Log-scaled count of user reviews
     * - Optional bonus for user-selected places
     */
    public static void computePlaceScores(List<PlaceData> places) {
        System.out.println("--- COMPUTING SCORES ---");
        float minScore = Float.MAX_VALUE;
        float maxScore = Float.MIN_VALUE;

        for (PlaceData place : places) {
            float rating = place.getRating();
            int reviewCount = place.getUserRatingsTotal();

            // Normalize rating: scale 0–5 to 0–100
            float normalizedRating = clamp((rating / MAX_RATING) * MAX_SCORE, 0, MAX_SCORE);

            // Normalize review count: log10 scale to compress large values
            float normalizedReviews = reviewCount > 0
                    ? clamp((float) (Math.log10(reviewCount) / LOG_NORMALIZATION_FACTOR) * MAX_SCORE, 0, MAX_SCORE)
                    : 0f;

            // Weighted average of rating and review score
            float weightRating = 0.7f;
            float weightReviews = 0.3f;
            float score = (normalizedRating * weightRating) + (normalizedReviews * weightReviews);

            // Bonus for manually selected places
            if (place.isUserSelected()) {
                score += USER_SELECTION_SCORE_BOOST;
            }

            place.setScore(score);

            minScore = Math.min(minScore, score);
            maxScore = Math.max(maxScore, score);
        }

        System.out.println("Score range: " + String.format("%.2f", minScore) + " - " + String.format("%.2f", maxScore));
    }

    public static Map<String, List<PlaceData>> groupPlacesByType(List<PlaceData> places) {
        System.out.println("--- GROUPING PLACES BY TYPE ---");
        Map<String, List<PlaceData>> map = new LinkedHashMap<>();
        for (PlaceData p : places) {
            map.computeIfAbsent(p.getPlaceType(), k -> new ArrayList<>()).add(p);
        }

        System.out.println("Created " + map.size() + " type groups");
        return map;
    }

    public static void sortTypeGroupsByScore(Map<String, List<PlaceData>> groupedPlaces) {
        System.out.println("--- SORTING PLACES BY SCORE WITHIN TYPES ---");
        for (Map.Entry<String, List<PlaceData>> entry : groupedPlaces.entrySet()) {
            List<PlaceData> list = entry.getValue();
            list.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

            if (!list.isEmpty()) {
                System.out.println("Type '" + entry.getKey() + "' - Top score: " + String.format("%.2f", list.get(0).getScore()) +
                        ", Bottom score: " + String.format("%.2f", list.get(list.size()-1).getScore()));
            }
        }
    }

    public static int fillByTypeQuotaBalanced(
            Map<String, List<PlaceData>> groupedPlaces,
            List<PlaceData> finalSelection,
            int currentTime,
            int totalAvailableTime
    ) {
        System.out.println("--- FILL BY TYPE QUOTA BALANCED (TOP 10 TYPES) ---");
        Set<PlaceData> selectedSet = new HashSet<>(finalSelection);
        int typeCount = groupedPlaces.size();
        int minTypes = Math.min(10, typeCount);
        int minTimePerType = totalAvailableTime / minTypes;

        System.out.println("Type count: " + typeCount + ", Min types to satisfy: " + minTypes);
        System.out.println("Min time per type: " + minTimePerType + " minutes");
        System.out.println("Starting time: " + currentTime + " minutes");

        // Phase A: Enforce minimum per type (top 10 types)
        System.out.println("=== PHASE A: MINIMUM PER TYPE (TOP 10) ===");
        List<String> typesToSatisfy = new ArrayList<>(groupedPlaces.keySet()).subList(0, minTypes);
        System.out.println("Types to satisfy: " + typesToSatisfy);

        for (String type : typesToSatisfy) {
            System.out.println("Processing type: " + type);
            int timeAllocated = 0;
            int placesAdded = 0;

            for (PlaceData place : groupedPlaces.get(type)) {
                if (selectedSet.contains(place)) {
                    System.out.println("  Skipping " + place.getName() + " - already selected");
                    continue;
                }

                int t = place.getTimeSpent();
                if (currentTime + t > totalAvailableTime) {
                    System.out.println("  Skipping " + place.getName() + " - would exceed time limit (" + (currentTime + t) + " > " + totalAvailableTime + ")");
                    break;
                }

                finalSelection.add(place);
                selectedSet.add(place);
                currentTime += t;
                timeAllocated += t;
                placesAdded++;

                System.out.println("  Added: " + place.getName() + " (Time: " + t + "min, Score: " + String.format("%.2f", place.getScore()) + ") - Current time: " + currentTime + "min");

                if (timeAllocated >= minTimePerType) {
                    System.out.println("  Minimum time satisfied for type " + type + " (" + timeAllocated + " >= " + minTimePerType + ")");
                    break;
                }
            }

            System.out.println("Type " + type + " - Added " + placesAdded + " places, " + timeAllocated + " minutes");
        }

        System.out.println("Phase A completed - Current time: " + currentTime + "min");
        return currentTime;
    }

    public static void fillWithLeftovers(
            Map<String, List<PlaceData>> groupedPlaces,
            List<PlaceData> finalSelection,
            int currentTime,
            int totalAvailableTime
    ) {
        System.out.println("--- FILL WITH LEFTOVERS ---");
        System.out.println("Starting time: " + currentTime + "min");

        Set<PlaceData> selectedSet = new HashSet<>(finalSelection);
        List<PlaceData> leftovers = new ArrayList<>();

        // Collect all unselected places
        for (Map.Entry<String, List<PlaceData>> entry : groupedPlaces.entrySet()) {
            String type = entry.getKey();
            int leftoverCount = 0;

            for (PlaceData p : entry.getValue()) {
                if (!selectedSet.contains(p)) {
                    leftovers.add(p);
                    leftoverCount++;
                }
            }

            System.out.println("Type " + type + " has " + leftoverCount + " leftover places");
        }

        System.out.println("Total leftovers: " + leftovers.size());

        // Sort by score
        leftovers.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        if (!leftovers.isEmpty()) {
            System.out.println("Top 5 leftover places by score:");
            for (int i = 0; i < Math.min(5, leftovers.size()); i++) {
                PlaceData p = leftovers.get(i);
                System.out.println("  #" + (i+1) + ": " + p.getName() + " (Type: " + p.getPlaceType() +
                        ", Time: " + p.getTimeSpent() + "min, Score: " + String.format("%.2f", p.getScore()) + ")");
            }
        }

        // Add leftovers that fit
        int addedCount = 0;
        for (PlaceData place : leftovers) {
            int t = place.getTimeSpent();
            if (currentTime + t > totalAvailableTime) {
                System.out.println("Skipping " + place.getName() + " - would exceed time limit (" + (currentTime + t) + " > " + totalAvailableTime + ")");
                continue;
            }

            finalSelection.add(place);
            currentTime += t;
            addedCount++;

            System.out.println("Added leftover: " + place.getName() + " (Type: " + place.getPlaceType() +
                    ", Time: " + t + "min) - Current time: " + currentTime + "min");
        }

        System.out.println("Added " + addedCount + " leftover places");
        System.out.println("Final time: " + currentTime + "min");
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }







    /**
     * Distributes a list of PlaceData objects into daily itineraries, respecting spatial clustering,
     * place-type diversity, and daily time constraints.
     *
     * @param places    List of all places to schedule
     * @param days      Number of days to plan
     * @param hoursList Available hours per day (size must equal "days")
     * @return          A List of days, each day itself a List<PlaceData>
     */
    public static List<List<PlaceData>> distributePlaces(
            List<PlaceData> places,
            int days,
            List<Integer> hoursList) {
        if (days <= 0 || places == null || hoursList == null || hoursList.size() != days) {
            throw new IllegalArgumentException("Invalid input parameters");
        }

        int n = places.size();
        // 1. Prepare geographic points
        double[][] points = new double[n][2];
        for (int i = 0; i < n; i++) {
            LatLng latLng = places.get(i).getLatLng();
            points[i][0] = latLng.latitude;
            points[i][1] = latLng.longitude;
        }


        // 2. Cluster by location into 'days' clusters
        int[] assignment = kMeans(points, days, 100);

        // 3. Build initial day lists
        List<List<PlaceData>> schedule = new ArrayList<>();
        for (int d = 0; d < days; d++) schedule.add(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            schedule.get(assignment[i]).add(places.get(i));
        }

        // 4. Enforce no duplicate place types per day
        enforceTypeDiversity(schedule);

        // 5. Fit each day into available hours
        balanceDailyTime(schedule, hoursList);

        return schedule;
    }

// ---- Helper functions below ----

    // KMeans on 2D points
    private static int[] kMeans(double[][] pts, int k, int maxIter) {
        int n = pts.length;
        int[] labels = new int[n];
        double[][] centroids = new double[k][2];
        Random rnd = new Random();
        for (int c = 0; c < k; c++) centroids[c] = pts[rnd.nextInt(n)].clone();

        for (int iter = 0; iter < maxIter; iter++) {
            boolean changed = false;
            // assignment step
            for (int i = 0; i < n; i++) {
                int best = 0;
                double bestDist = dist(pts[i], centroids[0]);
                for (int c = 1; c < k; c++) {
                    double d = dist(pts[i], centroids[c]);
                    if (d < bestDist) { bestDist = d; best = c; }
                }
                if (labels[i] != best) { labels[i] = best; changed = true; }
            }
            if (!changed) break;
            // update centroids
            double[][] sum = new double[k][2];
            int[] count = new int[k];
            for (int i = 0; i < n; i++) {
                int c = labels[i];
                sum[c][0] += pts[i][0];
                sum[c][1] += pts[i][1];
                count[c]++;
            }
            for (int c = 0; c < k; c++) {
                if (count[c] > 0) {
                    centroids[c][0] = sum[c][0] / count[c];
                    centroids[c][1] = sum[c][1] / count[c];
                }
            }
        }
        return labels;
    }

    // squared Euclidean distance
    private static double dist(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1];
        return dx*dx + dy*dy;
    }

    // Move duplicates so each day has unique place types
    private static void enforceTypeDiversity(List<List<PlaceData>> days) {
        int dCount = days.size();
        for (int d = 0; d < dCount; d++) {
            Set<String> seen = new HashSet<>();
            Iterator<PlaceData> iter = days.get(d).iterator();
            while (iter.hasNext()) {
                PlaceData p = iter.next();
                String type = p.getPlaceType();
                if (seen.contains(type)) {
                    // find another day lacking this type
                    for (int od = 0; od < dCount; od++) {
                        if (od == d) continue;
                        boolean has = days.get(od).stream()
                                .anyMatch(x -> x.getPlaceType().equals(type));
                        if (!has) {
                            days.get(od).add(p);
                            iter.remove();
                            break;
                        }
                    }
                } else {
                    seen.add(type);
                }
            }
        }
    }

    // Adjust days so total visit time <= available hours
    private static void balanceDailyTime(
            List<List<PlaceData>> days,
            List<Integer> hoursList) {
        boolean moved;
        do {
            moved = false;
            for (int d = 0; d < days.size(); d++) {
                double total = days.get(d).stream()
                        .mapToDouble(PlaceData::getTimeSpent).sum();
                double limit = hoursList.get(d);
                if (total > limit) {
                    // pick smallest-time place to move
                    PlaceData candidate = Collections.min(
                            days.get(d), Comparator.comparingDouble(PlaceData::getTimeSpent));
                    // find best target day
                    int bestDay = -1;
                    double bestSpace = -1;
                    for (int od = 0; od < days.size(); od++) {
                        if (od == d) continue;
                        double used = days.get(od).stream()
                                .mapToDouble(PlaceData::getTimeSpent).sum();
                        double space = hoursList.get(od) - used;
                        if (space >= candidate.getTimeSpent()
                                && days.get(od).stream()
                                .noneMatch(x -> x.getPlaceType().equals(candidate.getPlaceType()))
                                && space > bestSpace) {
                            bestSpace = space;
                            bestDay = od;
                        }
                    }
                    if (bestDay >= 0) {
                        days.get(d).remove(candidate);
                        days.get(bestDay).add(candidate);
                        moved = true;
                    }
                }
            }
        } while (moved);
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
                schedule,  // This should be your activitiesListData (List<List<PlaceData>>)
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
                case "Theater":
                    typesToSearch.put("concert_hall", 2);
                    typesToSearch.put("performing_arts_theater", 2);
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
            if (selectedPlaceIds != null && !selectedPlaceIds.isEmpty()) {
                for (String id : selectedPlaceIds) {
                    fetchPlaceById(id,timeSpent);
                }
            }
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



            case "concert_hall":
            case "performing_arts_theater":
                return new PlaceTypeInfo("Theater", R.drawable.ic_theater);
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
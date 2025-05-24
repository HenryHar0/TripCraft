package com.henry.tripcraft;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestaurantActivity extends AppCompatActivity {

    private static final String TAG = "RestaurantFinder";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI Components
    private MaterialButton btnCurrentLocation;
    private MaterialButton btnSearchRestaurants;
    private MaterialButton btnSortDistance;
    private MaterialButton btnSortRating;
    private MaterialButton btnSortPopularity;
    private MaterialButton btnTryAgain;
    private MaterialCardView radiusCard;
    private MaterialCardView sortCard;
    private ChipGroup chipGroupRadius;
    private RecyclerView recyclerViewRestaurants;
    private TextView tvResultsCount;
    private FrameLayout loadingLayout;
    private LinearLayout noResultsLayout;

    // Data
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentSearchLocation;
    private List<PlaceData> restaurantsList;
    private PlaceAdapter1 restaurantAdapter;
    private String apiKey;
    private int selectedRadius = 1000; // Default 1km
    private OkHttpClient httpClient;

    // Sort states
    private enum SortType {
        DISTANCE, RATING, POPULARITY
    }
    private SortType currentSortType = SortType.DISTANCE;

    // Restaurant types to filter for
    private static final Set<String> VALID_RESTAURANT_TYPES = new HashSet<>(Arrays.asList(
            "acai_shop",
            "afghani_restaurant",
            "african_restaurant",
            "american_restaurant",
            "asian_restaurant",
            "bagel_shop",
            "bakery",
            "bar",
            "bar_and_grill",
            "barbecue_restaurant",
            "brazilian_restaurant",
            "breakfast_restaurant",
            "brunch_restaurant",
            "buffet_restaurant",
            "cafe",
            "cafeteria",
            "candy_store",
            "cat_cafe",
            "chinese_restaurant",
            "chocolate_factory",
            "chocolate_shop",
            "coffee_shop",
            "confectionery",
            "deli",
            "dessert_restaurant",
            "dessert_shop",
            "diner",
            "dog_cafe",
            "donut_shop",
            "fast_food_restaurant",
            "fine_dining_restaurant",
            "food_court",
            "french_restaurant",
            "greek_restaurant",
            "hamburger_restaurant",
            "ice_cream_shop",
            "indian_restaurant",
            "indonesian_restaurant",
            "italian_restaurant",
            "japanese_restaurant",
            "juice_shop",
            "korean_restaurant",
            "lebanese_restaurant",
            "meal_delivery",
            "meal_takeaway",
            "mediterranean_restaurant",
            "mexican_restaurant",
            "middle_eastern_restaurant",
            "pizza_restaurant",
            "pub",
            "ramen_restaurant",
            "restaurant",
            "sandwich_shop",
            "seafood_restaurant",
            "spanish_restaurant",
            "steak_house",
            "sushi_restaurant",
            "tea_house",
            "thai_restaurant",
            "turkish_restaurant",
            "vegan_restaurant",
            "vegetarian_restaurant",
            "vietnamese_restaurant",
            "wine_bar"
    ));


    // Types to exclude (places that might serve food but aren't primarily restaurants)
    private static final Set<String> EXCLUDED_TYPES = new HashSet<>(Arrays.asList(
            "lodging", "hotel", "motel", "resort_hotel", "extended_stay_hotel",
            "golf_course", "country_club", "bowling_alley", "movie_theater",
            "amusement_park", "zoo", "aquarium", "museum", "casino",
            "gas_station", "convenience_store", "supermarket", "grocery_store",
            "department_store", "shopping_mall", "hospital", "school", "university"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);

        initializeViews();
        setupToolbar();
        setupHttpClient();
        setupLocationServices();
        setupClickListeners();
        setupRecyclerView();

        // Get API key from resources
        apiKey = getString(R.string.google_api_key1);
    }

    private void initializeViews() {
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        btnSearchRestaurants = findViewById(R.id.btnSearchRestaurants);
        btnSortDistance = findViewById(R.id.btnSortDistance);
        btnSortRating = findViewById(R.id.btnSortRating);
        btnSortPopularity = findViewById(R.id.btnSortPopularity);
        btnTryAgain = findViewById(R.id.btnTryAgain);
        radiusCard = findViewById(R.id.radiusCard);
        sortCard = findViewById(R.id.sortCard);
        chipGroupRadius = findViewById(R.id.chipGroupRadius);
        recyclerViewRestaurants = findViewById(R.id.recyclerViewRestaurants);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        loadingLayout = findViewById(R.id.loadingLayout);
        noResultsLayout = findViewById(R.id.noResultsLayout);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupClickListeners() {
        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnSearchRestaurants.setOnClickListener(v -> searchRestaurants());
        btnSortDistance.setOnClickListener(v -> sortResults(SortType.DISTANCE));
        btnSortRating.setOnClickListener(v -> sortResults(SortType.RATING));
        btnSortPopularity.setOnClickListener(v -> sortResults(SortType.POPULARITY));
        btnTryAgain.setOnClickListener(v -> resetSearch());

        // Radius chip selection
        chipGroupRadius.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip500m) {
                selectedRadius = 500;
            } else if (checkedId == R.id.chip1km) {
                selectedRadius = 1000;
            } else if (checkedId == R.id.chip2km) {
                selectedRadius = 2000;
            } else if (checkedId == R.id.chip5km) {
                selectedRadius = 5000;
            }
        });

        // Select default radius (1km)
        ((Chip) findViewById(R.id.chip1km)).setChecked(true);
    }

    private void setupRecyclerView() {
        restaurantsList = new ArrayList<>();
        restaurantAdapter = new PlaceAdapter1(restaurantsList, apiKey);
        recyclerViewRestaurants.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRestaurants.setAdapter(restaurantAdapter);
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        showLoading(true);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    showLoading(false);
                    if (location != null) {
                        currentSearchLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        showRadiusCard();
                        Toast.makeText(this, "Current location selected", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Unable to get current location. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void searchRestaurants() {
        if (currentSearchLocation == null) {
            Toast.makeText(this, "Please get your current location first", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        hideNoResults();

        String url = "https://places.googleapis.com/v1/places:searchNearby";
        Log.d(TAG, "Searching restaurants with URL: " + url);

        // Create request body for new Places API
        JSONObject requestBody = buildSearchNearbyRequestBody(currentSearchLocation, selectedRadius);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Goog-FieldMask", "places.id,places.displayName,places.rating,places.userRatingCount,places.location,places.types,places.regularOpeningHours,places.priceLevel,places.photos,places.websiteUri")
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json")))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(RestaurantActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "API Response: " + responseBody);

                runOnUiThread(() -> {
                    showLoading(false);
                    parseRestaurantsResponse(responseBody);
                });
            }
        });
    }

    private JSONObject buildSearchNearbyRequestBody(LatLng location, int radius) {
        try {
            JSONObject requestBody = new JSONObject();

            // Set included types for restaurants - be more specific
            JSONArray includedTypes = new JSONArray();
            includedTypes.put("restaurant");
            includedTypes.put("meal_takeaway");
            includedTypes.put("meal_delivery");
            includedTypes.put("bakery");
            includedTypes.put("cafe");
            requestBody.put("includedTypes", includedTypes);

            // Set max result count
            requestBody.put("maxResultCount", 20);

            // Set location restriction
            JSONObject locationRestriction = new JSONObject();
            JSONObject circle = new JSONObject();
            JSONObject center = new JSONObject();
            center.put("latitude", location.latitude);
            center.put("longitude", location.longitude);
            circle.put("center", center);
            circle.put("radius", radius);
            locationRestriction.put("circle", circle);
            requestBody.put("locationRestriction", locationRestriction);

            // Set ranking preference
            requestBody.put("rankPreference", "DISTANCE");

            return requestBody;
        } catch (JSONException e) {
            Log.e(TAG, "Error building request body", e);
            return new JSONObject();
        }
    }

    private void parseRestaurantsResponse(String responseBody) {
        Log.d(TAG, "Full API Response: " + responseBody);

        try {
            JSONObject jsonResponse = new JSONObject(responseBody);

            // Log the entire response structure
            Log.d(TAG, "Response keys: " + jsonResponse.keys().toString());

            if (!jsonResponse.has("places")) {
                Log.e(TAG, "No 'places' key found in response");
                if (jsonResponse.has("error")) {
                    JSONObject error = jsonResponse.getJSONObject("error");
                    Log.e(TAG, "API Error details: " + error.toString());
                }
                // Log all available keys to understand the response structure
                Log.d(TAG, "Available response keys: " + jsonResponse.keys().toString());
                showNoResults();
                return;
            }

            JSONArray results = jsonResponse.getJSONArray("places");
            Log.d(TAG, "Number of places returned: " + results.length());

            restaurantsList.clear();
            int validRestaurants = 0;
            int totalPlaces = results.length();

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);
                Log.d(TAG, "Processing place " + i + ": " + place.toString());

                // Log place name and types for debugging
                String placeName = "Unknown";
                if (place.has("displayName")) {
                    JSONObject displayName = place.getJSONObject("displayName");
                    placeName = displayName.optString("text", "Unknown");
                }
                Log.d(TAG, "Place name: " + placeName);

                if (place.has("types")) {
                    JSONArray types = place.getJSONArray("types");
                    Log.d(TAG, "Place types: " + types.toString());
                }

                // Check if it's a valid restaurant
                boolean isValid = isValidRestaurant(place);
                Log.d(TAG, "Is valid restaurant: " + isValid);

                if (isValid) {
                    validRestaurants++;
                    PlaceData restaurant = parseRestaurantFromJson(place);
                    if (restaurant != null) {
                        restaurantsList.add(restaurant);
                        Log.d(TAG, "Added restaurant: " + restaurant.getName());
                    } else {
                        Log.e(TAG, "Failed to parse restaurant data");
                    }
                }
            }

            Log.d(TAG, "Total places processed: " + totalPlaces);
            Log.d(TAG, "Valid restaurants found: " + validRestaurants);
            Log.d(TAG, "Final restaurant list size: " + restaurantsList.size());

            if (restaurantsList.isEmpty()) {
                showNoResults();
            } else {
                sortResults(SortType.DISTANCE);
                showResults();
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing restaurants response", e);
            Toast.makeText(this, "Error parsing restaurant data: " + e.getMessage(), Toast.LENGTH_LONG).show();
            showNoResults();
        }
    }

    private boolean isValidRestaurant(JSONObject place) {
        try {
            // Get the types array
            if (!place.has("types")) {
                return false;
            }

            JSONArray types = place.getJSONArray("types");
            List<String> placeTypes = new ArrayList<>();

            for (int i = 0; i < types.length(); i++) {
                placeTypes.add(types.getString(i));
            }

            Log.d(TAG, "Place types: " + placeTypes.toString());

            // Check if any of the place types are in our excluded list
            for (String type : placeTypes) {
                if (EXCLUDED_TYPES.contains(type)) {
                    Log.d(TAG, "Excluding place due to type: " + type);
                    return false;
                }
            }

            // Check if at least one type is a valid restaurant type
            for (String type : placeTypes) {
                if (VALID_RESTAURANT_TYPES.contains(type)) {
                    Log.d(TAG, "Valid restaurant type found: " + type);
                    return true;
                }
            }

            // If no valid restaurant type found, check if it contains "restaurant" in the name
            // as a fallback for edge cases
            if (place.has("displayName")) {
                JSONObject displayName = place.getJSONObject("displayName");
                String name = displayName.optString("text", "").toLowerCase();
                if (name.contains("restaurant") || name.contains("cafe") || name.contains("diner")
                        || name.contains("bistro") || name.contains("eatery") || name.contains("grill")) {
                    Log.d(TAG, "Valid restaurant found by name: " + name);
                    return true;
                }
            }

            Log.d(TAG, "Not a valid restaurant");
            return false;

        } catch (JSONException e) {
            Log.e(TAG, "Error checking if place is valid restaurant", e);
            return false;
        }
    }

    private PlaceData parseRestaurantFromJson(JSONObject place) {
        try {
            // Get place ID
            String placeId = place.optString("id", "");

            // Get display name (new API structure)
            String name = "Unknown Restaurant";
            if (place.has("displayName")) {
                JSONObject displayName = place.getJSONObject("displayName");
                name = displayName.optString("text", "Unknown Restaurant");
            }

            // Get rating
            float rating = (float) place.optDouble("rating", 0.0);

            // Get user rating count (renamed in new API)
            int userRatingsTotal = place.optInt("userRatingCount", 0);

            // Get location (new structure)
            JSONObject location = place.getJSONObject("location");
            double lat = location.getDouble("latitude");
            double lng = location.getDouble("longitude");
            LatLng latLng = new LatLng(lat, lng);

            // Get place type (first type from types array)
            String placeType = "restaurant";
            if (place.has("types")) {
                JSONArray types = place.getJSONArray("types");
                if (types.length() > 0) {
                    placeType = types.getString(0);
                }
            }

            // Get website URL
            String website = place.optString("websiteUri", "");

            // Parse opening hours for current day
            String simplifiedHours = parseSimplifiedOpeningHours(place);

            // Create PlaceData object
            PlaceData restaurant = new PlaceData(placeId, name, "", rating,
                    latLng, placeType, userRatingsTotal, 60); // Default 60 min time spent

            // Set opening hours (simplified)
            restaurant.setOpeningHours(simplifiedHours);

            // Set website
            restaurant.setWebsite(website);

            // Set price level if available
            if (place.has("priceLevel")) {
                String priceLevel = place.getString("priceLevel");
                // Convert string price level to int (new API uses strings like "PRICE_LEVEL_MODERATE")
                int priceLevelInt = convertPriceLevelToInt(priceLevel);
                restaurant.setPriceLevel(priceLevelInt);
            }

            // Add photo references if available (new structure)
            JSONArray photos = place.optJSONArray("photos");
            if (photos != null) {
                for (int j = 0; j < Math.min(photos.length(), 3); j++) {
                    JSONObject photo = photos.getJSONObject(j);
                    String photoName = photo.optString("name");
                    if (photoName != null && !photoName.isEmpty()) {
                        String photoUrl = "https://places.googleapis.com/v1/" + photoName +
                                "/media?maxWidthPx=400&key=" + apiKey;
                        restaurant.addPhotoReference(photoUrl);
                    }
                }
            } else {
                restaurant.addPhotoReference("default_placeholder");
            }

            // Calculate distance from search location
            if (currentSearchLocation != null) {
                float[] results = new float[1];
                Location.distanceBetween(
                        currentSearchLocation.latitude, currentSearchLocation.longitude,
                        lat, lng, results);
                restaurant.setScore(results[0]); // Use score field to store distance
            }

            return restaurant;

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing individual restaurant", e);
            return null;
        }
    }

    private String parseSimplifiedOpeningHours(JSONObject place) {
        try {
            if (!place.has("regularOpeningHours")) {
                return "Hours not available";
            }

            JSONObject openingHoursObj = place.getJSONObject("regularOpeningHours");

            // Get current day of week (0 = Sunday, 1 = Monday, etc.)
            Calendar calendar = Calendar.getInstance();
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // Convert to 0-based

            // Get current time
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
            int currentTimeInMinutes = currentHour * 60 + currentMinute;

            if (openingHoursObj.has("weekdayDescriptions")) {
                JSONArray weekdayDescriptions = openingHoursObj.getJSONArray("weekdayDescriptions");

                if (currentDayOfWeek < weekdayDescriptions.length()) {
                    String todayHours = weekdayDescriptions.getString(currentDayOfWeek);
                    return processOpeningHoursForToday(todayHours, currentTimeInMinutes);
                }
            }

            // Alternative: Try to parse from periods if available
            if (openingHoursObj.has("periods")) {
                JSONArray periods = openingHoursObj.getJSONArray("periods");
                return processPeriods(periods, currentDayOfWeek, currentTimeInMinutes);
            }

            return "Hours not available";

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing opening hours", e);
            return "Hours not available";
        }
    }

    private String processOpeningHoursForToday(String todayHours, int currentTimeInMinutes) {
        try {
            if (todayHours.toLowerCase().contains("closed")) {
                return "Closed today";
            }

            String[] parts = todayHours.split(":");
            if (parts.length < 2) {
                return todayHours;
            }

            String timePart = parts[1].trim();

            if (timePart.toLowerCase().contains("24 hours") || timePart.toLowerCase().contains("open 24 hours")) {
                return "Open 24 hours";
            }

            // Try different separators for time ranges
            String[] timeRange = null;
            if (timePart.contains("–")) {
                timeRange = timePart.split("–");
            } else if (timePart.contains("-")) {
                timeRange = timePart.split("-");
            } else if (timePart.contains(" to ")) {
                timeRange = timePart.split(" to ");
            }

            if (timeRange != null && timeRange.length == 2) {
                String openTime = timeRange[0].trim();
                String closeTime = timeRange[1].trim();

                int openMinutes = parseTimeToMinutes(openTime);
                int closeMinutes = parseTimeToMinutes(closeTime);

                if (openMinutes != -1 && closeMinutes != -1) {
                    boolean isOpen = isCurrentlyOpen(currentTimeInMinutes, openMinutes, closeMinutes);

                    if (isOpen) {
                        int minutesUntilClose = calculateMinutesUntilClose(currentTimeInMinutes, closeMinutes);

                        if (minutesUntilClose > 60) {
                            int hoursUntilClose = minutesUntilClose / 60;
                            return "Open: closes in " + hoursUntilClose + " hour" + (hoursUntilClose == 1 ? "" : "s");
                        } else if (minutesUntilClose > 0) {
                            return "Open: closes in " + minutesUntilClose + " min";
                        } else {
                            return "Open: closes soon";
                        }
                    } else {
                        String openTimeFormatted = formatTime(openMinutes);
                        String closeTimeFormatted = formatTime(closeMinutes);
                        return "Closed: opens " + openTimeFormatted + "-" + closeTimeFormatted;
                    }
                }
            } else {
                // Handle single time value or malformed data
                // Check if it's a single number that could be an hour
                if (timePart.matches("\\d{1,2}")) {
                    int hour = Integer.parseInt(timePart);
                    if (hour >= 6 && hour <= 23) { // Reasonable restaurant hours
                        String formattedTime = formatTime(hour * 60);
                        return "Opens at " + formattedTime;
                    }
                }

                // Check if it looks like incomplete time data
                if (timePart.length() < 3 || !timePart.matches(".*\\d.*")) {
                    return "Hours not available";
                }
            }

            return timePart;

        } catch (Exception e) {
            return "Hours not available";
        }
    }


    private String processPeriods(JSONArray periods, int currentDayOfWeek, int currentTimeInMinutes) {
        try {
            for (int i = 0; i < periods.length(); i++) {
                JSONObject period = periods.getJSONObject(i);

                if (period.has("open") && period.has("close")) {
                    JSONObject open = period.getJSONObject("open");
                    JSONObject close = period.getJSONObject("close");

                    int openDay = open.getInt("day");
                    int openHour = open.getInt("hour");
                    int openMinute = open.optInt("minute", 0);

                    int closeDay = close.getInt("day");
                    int closeHour = close.getInt("hour");
                    int closeMinute = close.optInt("minute", 0);

                    if (openDay == currentDayOfWeek) {
                        int openTimeInMinutes = openHour * 60 + openMinute;
                        int closeTimeInMinutes = closeHour * 60 + closeMinute;

                        // Handle midnight crossover
                        if (closeDay != openDay) {
                            closeTimeInMinutes += 24 * 60; // Add 24 hours if closes next day
                        }

                        boolean isOpen = isCurrentlyOpen(currentTimeInMinutes, openTimeInMinutes, closeTimeInMinutes);

                        if (isOpen) {
                            int minutesUntilClose = calculateMinutesUntilClose(currentTimeInMinutes, closeTimeInMinutes);
                            if (minutesUntilClose > 60) {
                                int hoursUntilClose = minutesUntilClose / 60;
                                return "Open: closes in " + hoursUntilClose + " hour" + (hoursUntilClose == 1 ? "" : "s");
                            } else if (minutesUntilClose > 0) {
                                return "Open: closes in " + minutesUntilClose + " min";
                            } else {
                                return "Open: closes soon";
                            }
                        } else {
                            String openTime = formatTime(openTimeInMinutes);
                            String closeTime = formatTime(closeTimeInMinutes % (24 * 60)); // Handle day overflow
                            return "Closed: opens " + openTime + "-" + closeTime;
                        }
                    }
                }
            }

            return "Closed today";

        } catch (JSONException e) {
            Log.e(TAG, "Error processing periods", e);
            return "Hours not available";
        }
    }

    private int parseTimeToMinutes(String timeStr) {
        try {
            timeStr = timeStr.trim().toUpperCase();
            boolean isPM = timeStr.contains("PM");
            boolean isAM = timeStr.contains("AM");

            // Remove AM/PM
            timeStr = timeStr.replace("AM", "").replace("PM", "").trim();

            String[] timeParts = timeStr.split(":");
            if (timeParts.length >= 2) {
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                // Convert to 24-hour format
                if (isPM && hour != 12) {
                    hour += 12;
                } else if (isAM && hour == 12) {
                    hour = 0;
                }

                return hour * 60 + minute;
            }

            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean isCurrentlyOpen(int currentTime, int openTime, int closeTime) {
        if (closeTime > openTime) {
            // Same day (e.g., 9 AM to 5 PM)
            return currentTime >= openTime && currentTime < closeTime;
        } else {
            // Crosses midnight (e.g., 10 PM to 2 AM)
            return currentTime >= openTime || currentTime < closeTime;
        }
    }

    private int calculateMinutesUntilClose(int currentTime, int closeTime) {
        int minutesUntilClose;

        if (closeTime > currentTime) {
            minutesUntilClose = closeTime - currentTime;
        } else {
            // Closes tomorrow
            minutesUntilClose = (24 * 60) - currentTime + closeTime;
        }

        return minutesUntilClose;
    }

    private String formatTime(int timeInMinutes) {
        int hour = (timeInMinutes / 60) % 24;
        int minute = timeInMinutes % 60;

        String period = (hour < 12) ? "AM" : "PM";
        int displayHour = (hour == 0) ? 12 : (hour > 12) ? hour - 12 : hour;

        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, period);
    }

    private int convertPriceLevelToInt(String priceLevel) {
        switch (priceLevel) {
            case "PRICE_LEVEL_FREE":
                return 0;
            case "PRICE_LEVEL_INEXPENSIVE":
                return 1;
            case "PRICE_LEVEL_MODERATE":
                return 2;
            case "PRICE_LEVEL_EXPENSIVE":
                return 3;
            case "PRICE_LEVEL_VERY_EXPENSIVE":
                return 4;
            default:
                return -1; // Unknown
        }
    }

    private void sortResults(SortType sortType) {
        if (restaurantsList == null || restaurantsList.isEmpty()) {
            return;
        }

        currentSortType = sortType;
        updateSortButtonStates();

        switch (sortType) {
            case DISTANCE:
                Collections.sort(restaurantsList, (r1, r2) ->
                        Float.compare(r1.getScore(), r2.getScore()));
                break;
            case RATING:
                Collections.sort(restaurantsList, (r1, r2) ->
                        Float.compare(r2.getRating(), r1.getRating()));
                break;
            case POPULARITY:
                Collections.sort(restaurantsList, (r1, r2) ->
                        Integer.compare(r2.getUserRatingsTotal(), r1.getUserRatingsTotal()));
                break;
        }

        restaurantAdapter.updatePlaces(restaurantsList);
        updateResultsCount();
    }

    private void updateSortButtonStates() {
        // Reset all buttons
        btnSortDistance.setBackgroundTintList(null);
        btnSortRating.setBackgroundTintList(null);
        btnSortPopularity.setBackgroundTintList(null);

        // Highlight selected button
        int selectedColor = ContextCompat.getColor(this, R.color.dusk_purple);
        int defaultColor = ContextCompat.getColor(this, android.R.color.white);

        switch (currentSortType) {
            case DISTANCE:
                btnSortDistance.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, R.color.dusk_purple));
                break;
            case RATING:
                btnSortRating.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, R.color.dusk_purple));
                break;
            case POPULARITY:
                btnSortPopularity.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, R.color.dusk_purple));
                break;
        }
    }

    private void showRadiusCard() {
        radiusCard.setVisibility(View.VISIBLE);
        radiusCard.animate().alpha(1.0f).setDuration(300);
    }

    private void showLoading(boolean show) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showResults() {
        sortCard.setVisibility(View.VISIBLE);
        recyclerViewRestaurants.setVisibility(View.VISIBLE);
        tvResultsCount.setVisibility(View.VISIBLE);
        noResultsLayout.setVisibility(View.GONE);
        updateResultsCount();
    }

    private void showNoResults() {
        sortCard.setVisibility(View.GONE);
        recyclerViewRestaurants.setVisibility(View.GONE);
        tvResultsCount.setVisibility(View.GONE);
        noResultsLayout.setVisibility(View.VISIBLE);
    }

    private void hideNoResults() {
        noResultsLayout.setVisibility(View.GONE);
    }

    private void updateResultsCount() {
        if (restaurantsList != null) {
            String countText = "Found " + restaurantsList.size() + " restaurants";
            tvResultsCount.setText(countText);
        }
    }

    private void resetSearch() {
        currentSearchLocation = null;
        radiusCard.setVisibility(View.GONE);
        sortCard.setVisibility(View.GONE);
        recyclerViewRestaurants.setVisibility(View.GONE);
        tvResultsCount.setVisibility(View.GONE);
        noResultsLayout.setVisibility(View.GONE);

        if (restaurantsList != null) {
            restaurantsList.clear();
            restaurantAdapter.updatePlaces(restaurantsList);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission is required to use current location",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (noResultsLayout.getVisibility() == View.VISIBLE ||
                recyclerViewRestaurants.getVisibility() == View.VISIBLE) {
            resetSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}
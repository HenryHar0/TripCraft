package com.example.tripcraft000;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class InterestsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private LinearLayout interestsLayout;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button applyFiltersButton;
    private LinearLayout categoriesLayout;
    private String selectedCityName;
    private LatLng selectedCityCoordinates;
    private final String API_KEY = "AIzaSyCYnYiiqrHO0uwKoxNQLA_mKEIuX1aRyL4";
    private final ConcurrentHashMap<String, Integer> typeCountMap = new ConcurrentHashMap<>();
    private final AtomicInteger pendingRequests = new AtomicInteger(0);
    private Set<String> selectedCategories = new HashSet<>();
    private List<Map.Entry<String, Integer>> allCategories = new ArrayList<>();
    private Button nextButton;
    private final List<String> pendingPageTokens = new ArrayList<>();
    private static final int PAGINATION_DELAY = 2000;
    private final Set<String> processedPlaceIds = new HashSet<>();
    private LatLngBounds cityBounds;
    private int cityRadius = 5000;  // Default radius

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);

        // Initialize UI components
        interestsLayout = findViewById(R.id.interestsLayout);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        applyFiltersButton = findViewById(R.id.applyFiltersButton);
        categoriesLayout = findViewById(R.id.categoriesLayout);
        nextButton = findViewById(R.id.nextButton);

        // Get intent extras
        selectedCityName = getIntent().getStringExtra("city");
        int geonameId = getIntent().getIntExtra("geonameId", -1);
        String startDate = getIntent().getStringExtra("start_date");
        String endDate = getIntent().getStringExtra("end_date");
        int durationDays = getIntent().getIntExtra("duration_days", 0);

        // Get coordinates from intent or use default coordinates based on city name
        double lat = getIntent().getDoubleExtra("city_lat", 0);
        double lng = getIntent().getDoubleExtra("city_lng", 0);

        // If coordinates weren't passed, use some defaults for common cities
        if (lat == 0 && lng == 0) {
            if (selectedCityName.equalsIgnoreCase("Paris")) {
                lat = 48.8566;
                lng = 2.3522;
            } else if (selectedCityName.equalsIgnoreCase("London")) {
                lat = 51.5074;
                lng = -0.1278;
            } else if (selectedCityName.equalsIgnoreCase("New York")) {
                lat = 40.7128;
                lng = -74.0060;
            } else if (selectedCityName.equalsIgnoreCase("Tokyo")) {
                lat = 35.6762;
                lng = 139.6503;
            } else {
                // Default coordinates if city is unknown
                lat = 0;
                lng = 0;
                Toast.makeText(this, "No coordinates available for " + selectedCityName, Toast.LENGTH_SHORT).show();
            }
        }

        selectedCityCoordinates = new LatLng(lat, lng);

        TextView titleTextView = findViewById(R.id.titleText);
        if (titleTextView != null) {
            titleTextView.setText("Explore " + selectedCityName);
        }

        // Setup button click listeners
        if (applyFiltersButton != null) {
            applyFiltersButton.setOnClickListener(v -> applyFilters());
        }

        if (nextButton != null) {
            nextButton.setOnClickListener(v -> navigateToNextScreen());
        }

        // Fetch city boundaries first
        fetchCityBoundaries(selectedCityName, bounds -> {
            if (bounds != null) {
                cityBounds = bounds;
                // Calculate radius based on city boundaries
                cityRadius = calculateCityRadius(bounds);
                Log.d("InterestsActivity", "City radius calculated: " + cityRadius + " meters");
            }

            // Check for location permission and start fetching places
            if (hasLocationPermission()) {
                startFetchingPlaces();
            } else {
                requestLocationPermission();
            }
        });
    }

    private void fetchCityBoundaries(String cityName, final BoundariesCallback callback) {
        String encodedCityName;
        try {
            encodedCityName = URLEncoder.encode(cityName, "UTF-8");
        } catch (Exception e) {
            Log.e("InterestsActivity", "Error encoding city name", e);
            callback.onBoundariesFetched(null);
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/geocode/json" +
                "?address=" + encodedCityName +
                "&key=" + API_KEY;

        new Thread(() -> {
            try {
                URL requestUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setRequestMethod("GET");

                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(reader);
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                }

                bufferedReader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                LatLngBounds bounds = parseBoundaries(jsonResponse);

                runOnUiThread(() -> callback.onBoundariesFetched(bounds));

            } catch (Exception e) {
                Log.e("InterestsActivity", "Error fetching city boundaries", e);
                runOnUiThread(() -> callback.onBoundariesFetched(null));
            }
        }).start();
    }

    private LatLngBounds parseBoundaries(JSONObject jsonResponse) throws JSONException {
        if (jsonResponse.getString("status").equals("OK")) {
            JSONArray results = jsonResponse.getJSONArray("results");
            if (results.length() > 0) {
                JSONObject result = results.getJSONObject(0);
                JSONObject geometry = result.getJSONObject("geometry");
                if (geometry.has("viewport")) {
                    JSONObject viewport = geometry.getJSONObject("viewport");

                    JSONObject northeast = viewport.getJSONObject("northeast");
                    double neLat = northeast.getDouble("lat");
                    double neLng = northeast.getDouble("lng");

                    JSONObject southwest = viewport.getJSONObject("southwest");
                    double swLat = southwest.getDouble("lat");
                    double swLng = southwest.getDouble("lng");

                    return new LatLngBounds(
                            new LatLng(swLat, swLng),
                            new LatLng(neLat, neLng)
                    );
                }
            }
        }
        return null;
    }

    private int calculateCityRadius(LatLngBounds bounds) {
        // Calculate the diagonal of the bounding box to determine an appropriate radius
        double distance = calculateDistance(
                bounds.southwest.latitude, bounds.southwest.longitude,
                bounds.northeast.latitude, bounds.northeast.longitude
        );

        // Take roughly 70% of the diagonal as radius to cover most of the city area
        return (int) (distance * 0.7 * 1000); // Convert to meters
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        int radiusEarth = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radiusEarth * c;
    }

    interface BoundariesCallback {
        void onBoundariesFetched(LatLngBounds bounds);
    }

    private void startFetchingPlaces() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Loading places for " + selectedCityName + "...");

        typeCountMap.clear();
        processedPlaceIds.clear();
        pendingPageTokens.clear();
        interestsLayout.removeAllViews();

        fetchNearbyPlaces();
        fetchPlacesByTypes();
        performTextSearch();
    }

    private void fetchNearbyPlaces() {
        Log.d("InterestsActivity", "Fetching nearby places for: " + selectedCityName + " with radius: " + cityRadius);

        String location = selectedCityCoordinates.latitude + "," + selectedCityCoordinates.longitude;

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + location +
                "&radius=" + cityRadius +  // Use calculated city radius
                "&key=" + API_KEY;

        pendingRequests.incrementAndGet();
        fetchPlacesPage(url, null, "nearby");
    }

    private void fetchPlacesByTypes() {
        Log.d("InterestsActivity", "Fetching multiple place types for: " + selectedCityName);

        String[] popularTypes = {
                "tourist_attraction", "museum", "restaurant", "cafe", "park",
                "shopping_mall", "art_gallery", "bar", "zoo", "amusement_park",
                "aquarium", "stadium", "church", "night_club", "movie_theater",
                "bakery", "library", "spa", "casino"
        };

        for (final String type : popularTypes) {
            pendingRequests.incrementAndGet();
            fetchPlacesByType(type);
        }
    }

    private void fetchPlacesByType(final String type) {
        final String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + selectedCityCoordinates.latitude + "," + selectedCityCoordinates.longitude +
                "&radius=" + cityRadius +  // Use calculated city radius
                "&type=" + type +
                "&key=" + API_KEY;

        new Thread(() -> {
            try {
                URL requestUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setRequestMethod("GET");

                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(reader);
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                }

                bufferedReader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                parsePlaceResults(jsonResponse);

                int remaining = pendingRequests.decrementAndGet();
                if (remaining == 0) {
                    updateUI();
                }

            } catch (Exception e) {
                Log.e("PlacesAPI", "Error fetching places by type: " + type, e);
                int remaining = pendingRequests.decrementAndGet();
                if (remaining == 0) {
                    updateUI();
                }
            }
        }).start();
    }

    private void fetchPlacesPage(final String baseUrl, final String pageToken, final String searchType) {
        final String finalUrl = pageToken == null ? baseUrl : baseUrl + "&pagetoken=" + pageToken;

        new Thread(() -> {
            try {
                URL requestUrl = new URL(finalUrl);
                HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(reader);
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                }

                bufferedReader.close();

                final JSONObject jsonResponse = new JSONObject(response.toString());
                String status = jsonResponse.getString("status");

                if ("OK".equals(status)) {
                    parsePlaceResults(jsonResponse);

                    if (jsonResponse.has("next_page_token")) {
                        final String nextPageToken = jsonResponse.getString("next_page_token");
                        synchronized (pendingPageTokens) {
                            pendingPageTokens.add(nextPageToken + ":" + searchType);
                        }

                        // Process next page after delay (required by Places API)
                        new Thread(() -> {
                            try {
                                Thread.sleep(PAGINATION_DELAY);
                                String tokenInfo;
                                synchronized (pendingPageTokens) {
                                    if (pendingPageTokens.isEmpty()) return;
                                    tokenInfo = pendingPageTokens.remove(0);
                                }

                                String[] parts = tokenInfo.split(":");
                                String token = parts[0];
                                String type = parts[1];

                                if ("nearby".equals(type)) {
                                    fetchPlacesPage(baseUrl, token, type);
                                } else if ("text".equals(type)) {
                                    fetchPlacesPage(baseUrl, token, type);
                                }
                            } catch (Exception e) {
                                Log.e("PlacesAPI", "Error processing page token", e);
                            }
                        }).start();
                    }
                } else {
                    Log.e("PlacesAPI", "Error status: " + status);
                    if ("nearby".equals(searchType) && pageToken == null) {
                        fallbackToTextSearch();
                    }
                }

                int remaining = pendingRequests.decrementAndGet();
                if (remaining == 0) {
                    updateUI();
                }

            } catch (Exception e) {
                Log.e("PlacesAPI", "Error fetching places", e);

                if ("nearby".equals(searchType) && pageToken == null) {
                    fallbackToTextSearch();
                }

                int remaining = pendingRequests.decrementAndGet();
                if (remaining == 0) {
                    updateUI();
                }
            }
        }).start();
    }

    private void fallbackToTextSearch() {
        Log.d("InterestsActivity", "Falling back to text search for categories");

        try {
            for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
                String category = entry.getKey();

                // Remove emoji if present
                String queryText = category;
                if (queryText.contains(" ")) {
                    queryText = queryText.substring(queryText.indexOf(" ") + 1);
                }

                String query = URLEncoder.encode(queryText + " in " + selectedCityName, "UTF-8");
                String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                        "?query=" + query +
                        "&key=" + API_KEY;

                pendingRequests.incrementAndGet();
                fetchPlacesPage(url, null, "text");
            }
        } catch (Exception e) {
            Log.e("PlacesAPI", "Error encoding text search URL", e);
        }
    }

    private void performTextSearch() {
        Log.d("InterestsActivity", "Performing text search for: " + selectedCityName);

        try {
            String[] queries = {
                    "top attractions in " + selectedCityName,
                    "must visit in " + selectedCityName,
                    "things to do in " + selectedCityName,
                    "best restaurants in " + selectedCityName,
                    "entertainment in " + selectedCityName,
                    "cultural sites in " + selectedCityName,
                    "historical sites in " + selectedCityName,
                    "popular in " + selectedCityName,
                    "landmarks in " + selectedCityName
            };

            for (final String query : queries) {
                pendingRequests.incrementAndGet();
                final String encodedQuery = URLEncoder.encode(query, "UTF-8");
                final String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                        "?query=" + encodedQuery +
                        "&key=" + API_KEY;

                fetchPlacesPage(url, null, "text");
            }
        } catch (Exception e) {
            Log.e("PlacesAPI", "Error encoding query", e);
        }
    }

    private void parsePlaceResults(JSONObject jsonResponse) {
        try {
            if (!jsonResponse.has("results")) {
                Log.d("PlacesAPI", "No results found in response");
                return;
            }

            JSONArray results = jsonResponse.getJSONArray("results");
            Log.d("PlacesAPI", "Found " + results.length() + " places");

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);
                String placeId = place.getString("place_id");

                // Only count a place once by using placeId as a unique identifier
                if (processedPlaceIds.add(placeId) && place.has("types")) {
                    JSONArray types = place.getJSONArray("types");
                    boolean categoryAdded = false;

                    for (int j = 0; j < types.length() && !categoryAdded; j++) {
                        String type = types.getString(j);
                        String formattedType = getFormattedPlaceType(type);

                        if (formattedType != null) {
                            // Only count the first valid category for each place
                            typeCountMap.put(formattedType, typeCountMap.getOrDefault(formattedType, 0) + 1);
                            categoryAdded = true;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("PlacesAPI", "Error parsing place results", e);
        }
    }

    private synchronized void updateUI() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            statusText.setText("Found " + typeCountMap.size() + " categories in " + selectedCityName);

            categoriesLayout.setVisibility(View.VISIBLE);
            categoriesLayout.removeAllViews();

            allCategories = new ArrayList<>(typeCountMap.entrySet());
            Collections.sort(allCategories, (a, b) -> b.getValue().compareTo(a.getValue()));

            int columnCount = 2;
            LinearLayout currentRow = null;

            CheckBox selectAllCheckbox = new CheckBox(this);
            selectAllCheckbox.setText("Select All");
            selectAllCheckbox.setChecked(false);
            selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
                    View child = categoriesLayout.getChildAt(i);
                    if (child instanceof LinearLayout) {
                        LinearLayout row = (LinearLayout) child;
                        for (int j = 0; j < row.getChildCount(); j++) {
                            View checkbox = row.getChildAt(j);
                            if (checkbox instanceof CheckBox) {
                                ((CheckBox) checkbox).setChecked(isChecked);

                                // Update selected categories
                                if (checkbox.getTag() != null) {
                                    String category = (String) checkbox.getTag();
                                    if (isChecked) {
                                        selectedCategories.add(category);
                                    } else {
                                        selectedCategories.remove(category);
                                    }
                                }
                            }
                        }
                    }
                }
            });

            LinearLayout selectAllRow = new LinearLayout(this);
            selectAllRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            selectAllRow.addView(selectAllCheckbox);
            categoriesLayout.addView(selectAllRow);

            for (int i = 0; i < allCategories.size(); i++) {
                if (i % columnCount == 0) {
                    currentRow = new LinearLayout(this);
                    currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                    categoriesLayout.addView(currentRow);
                }

                Map.Entry<String, Integer> entry = allCategories.get(i);
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(entry.getKey());  // Removed count display from checkbox text
                checkBox.setTag(entry.getKey());

                // Check if this category was previously selected
                checkBox.setChecked(selectedCategories.contains(entry.getKey()));

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    String category = (String) buttonView.getTag();
                    if (isChecked) {
                        selectedCategories.add(category);
                    } else {
                        selectedCategories.remove(category);
                    }
                });

                LinearLayout.LayoutParams checkboxParams = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f);
                checkBox.setLayoutParams(checkboxParams);

                if (currentRow != null) {
                    currentRow.addView(checkBox);
                }
            }

            displaySelectedCategories();

            if (allCategories.isEmpty()) {
                statusText.setText("No places found for " + selectedCityName + ". Please try again.");
            }

            // Enable the Next button after loading completes
            if (nextButton != null) {
                nextButton.setEnabled(true);
            }
        });
    }

    private void applyFilters() {
        displaySelectedCategories();
    }

    private void displaySelectedCategories() {
        interestsLayout.removeAllViews();

        CardView titleCard = createTitleCard("Your Interests in " + selectedCityName);
        interestsLayout.addView(titleCard);

        int count = 0;

        for (Map.Entry<String, Integer> entry : allCategories) {
            if (selectedCategories.contains(entry.getKey())) {
                createCategoryCard(entry.getKey(), entry.getValue());
                count++;
            }
        }

        statusText.setText("Showing " + count + " categories in " + selectedCityName);
    }

    private CardView createTitleCard(String title) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setCardElevation(4);
        card.setRadius(8);

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(18);
        titleText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        titleText.setPadding(16, 16, 16, 16);
        titleText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        card.addView(titleText);
        return card;
    }

    private void createCategoryCard(final String category, final int count) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setCardElevation(4);
        card.setRadius(8);

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(16, 16, 16, 16);

        TextView categoryText = new TextView(this);
        categoryText.setText(category);
        categoryText.setTextSize(16);
        categoryText.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        // Removed countText TextView from category card

        Button viewButton = new Button(this);
        viewButton.setText("View Places");
        viewButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        viewButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, 16, 0, 0);
        viewButton.setLayoutParams(buttonParams);

        viewButton.setOnClickListener(v -> {
            // Launch the MapPlacesActivity when the button is clicked
            Intent intent = new Intent(InterestsActivity.this, MapPlacesActivity.class);
            intent.putExtra("city_name", selectedCityName);
            intent.putExtra("city_lat", selectedCityCoordinates.latitude);
            intent.putExtra("city_lng", selectedCityCoordinates.longitude);
            intent.putExtra("category_name", category);
            startActivity(intent);
        });

        cardLayout.addView(categoryText);
        // Removed cardLayout.addView(countText);  // Removed count display
        cardLayout.addView(viewButton);

        card.addView(cardLayout);
        interestsLayout.addView(card);
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

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void navigateToNextScreen() {
        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, "Please select at least one category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create intent for PlanActivity
        Intent intent = new Intent(InterestsActivity.this, PlanActivity.class);

        // Pass along all the information received from the previous activity
        intent.putExtra("city", selectedCityName);
        intent.putExtra("geonameId", getIntent().getIntExtra("geonameId", -1));
        intent.putExtra("start_date", getIntent().getStringExtra("start_date"));
        intent.putExtra("end_date", getIntent().getStringExtra("end_date"));
        intent.putExtra("duration_days", getIntent().getIntExtra("duration_days", 0));

        // Pass city coordinates
        intent.putExtra("city_lat", selectedCityCoordinates.latitude);
        intent.putExtra("city_lng", selectedCityCoordinates.longitude);

        // Pass selected categories
        intent.putStringArrayListExtra("selected_categories", new ArrayList<>(selectedCategories));

        // Start PlanActivity
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startFetchingPlaces();
            } else {
                Toast.makeText(this, "Location permission is required to fetch nearby places", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

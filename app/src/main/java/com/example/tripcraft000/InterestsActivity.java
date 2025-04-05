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

        // Check for location permission and start fetching places
        if (hasLocationPermission()) {
            startFetchingPlaces();
        } else {
            requestLocationPermission();
        }
    }

    private void navigateToNextScreen() {
        ArrayList<String> selectedCategoriesList = new ArrayList<>(selectedCategories);

        Intent intent = new Intent(InterestsActivity.this, PlanActivity.class);

        intent.putExtra("selected_city", selectedCityName);
        intent.putExtra("selected_city_coordinates", selectedCityCoordinates);

        intent.putExtra("start_date", getIntent().getStringExtra("start_date"));
        intent.putExtra("end_date", getIntent().getStringExtra("end_date"));
        intent.putExtra("duration_days", getIntent().getIntExtra("duration_days", 0));

        intent.putExtra("geonameId", getIntent().getIntExtra("geonameId", -1));

        intent.putStringArrayListExtra("selected_categories", selectedCategoriesList);

        startActivity(intent);
    }

    private void startFetchingPlaces() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Loading places for " + selectedCityName + "...");

        typeCountMap.clear();
        processedPlaceIds.clear(); // Clear processed place IDs
        pendingPageTokens.clear(); // Clear pending page tokens
        interestsLayout.removeAllViews();

        fetchNearbyPlaces();
        fetchPlacesByTypes();
        fetchMultipleAreas();
        performTextSearch();
        fetchSpecificCategories(); // Add this new method call
    }

    private void fetchNearbyPlaces() {
        Log.d("InterestsActivity", "Fetching nearby places with pagination for: " + selectedCityName);

        String location = selectedCityCoordinates.latitude + "," + selectedCityCoordinates.longitude;
        int radius = 30000;

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + location +
                "&radius=" + radius +
                "&key=" + API_KEY;

        pendingRequests.incrementAndGet();
        fetchPlacesPage(url, null, "nearby");
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
                        // Save the token for later processing
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
                    // If nearby search fails, try text search
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

                // Implement fallback strategy
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
                "&radius=5000" +
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

    private void fetchMultipleAreas() {
        Log.d("InterestsActivity", "Fetching from multiple areas for: " + selectedCityName);

        List<LatLng> areaCoordinates = new ArrayList<>();

        areaCoordinates.add(selectedCityCoordinates);

        double latOffset = 0.018;
        double lngOffset = 0.018;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;

                LatLng offset = new LatLng(
                        selectedCityCoordinates.latitude + (i * latOffset),
                        selectedCityCoordinates.longitude + (j * lngOffset)
                );
                areaCoordinates.add(offset);
            }
        }

        if (selectedCityName.equalsIgnoreCase("Paris")) {
            areaCoordinates.add(new LatLng(48.8584, 2.2945));
            areaCoordinates.add(new LatLng(48.8606, 2.3376));
            areaCoordinates.add(new LatLng(48.8738, 2.2950));
            areaCoordinates.add(new LatLng(48.8530, 2.3499));
        } else if (selectedCityName.equalsIgnoreCase("London")) {
            areaCoordinates.add(new LatLng(51.5007, -0.1246));
            areaCoordinates.add(new LatLng(51.5074, -0.1278));
            areaCoordinates.add(new LatLng(51.5067, -0.0762));
        }

        for (final LatLng area : areaCoordinates) {
            pendingRequests.incrementAndGet();
            fetchPlacesForArea(area);
        }
    }

    private void fetchPlacesForArea(final LatLng area) {
        final String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + area.latitude + "," + area.longitude +
                "&radius=3000" +
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
                Log.e("PlacesAPI", "Error fetching places for area: " + area, e);
                int remaining = pendingRequests.decrementAndGet();
                if (remaining == 0) {
                    updateUI();
                }
            }
        }).start();
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
                    // Add more queries for better coverage
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

    private void executeTextSearch(final String url) {
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
                Log.e("PlacesAPI", "Error performing text search", e);
                int remaining = pendingRequests.decrementAndGet();
                if (remaining == 0) {
                    updateUI();
                }
            }
        }).start();
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

                // Store place IDs to avoid duplicates
                if (processedPlaceIds.add(placeId) && place.has("types")) {
                    JSONArray types = place.getJSONArray("types");

                    for (int j = 0; j < types.length(); j++) {
                        String type = types.getString(j);
                        String formattedType = getFormattedPlaceType(type);

                        if (formattedType != null) {
                            typeCountMap.put(formattedType, typeCountMap.getOrDefault(formattedType, 0) + 1);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("PlacesAPI", "Error parsing place results", e);
        }
    }

    private void fetchSpecificCategories() {
        Log.d("InterestsActivity", "Fetching specific categories for: " + selectedCityName);

        // Important categories that users are likely interested in
        String[] importantCategories = {
                "museum", "art_gallery", "tourist_attraction", "restaurant", "park",
                "shopping_mall", "historic", "landmark", "attraction"
        };

        for (String category : importantCategories) {
            pendingRequests.incrementAndGet();

            try {
                String query = URLEncoder.encode(category + " in " + selectedCityName, "UTF-8");
                String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                        "?query=" + query +
                        "&key=" + API_KEY;

                fetchPlacesPage(url, null, "text");
            } catch (Exception e) {
                Log.e("PlacesAPI", "Error encoding specific category", e);
                pendingRequests.decrementAndGet();
            }
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
                checkBox.setText(entry.getKey());
                checkBox.setTag(entry.getKey());

                // MODIFIED: Set all checkboxes to unchecked initially
                checkBox.setChecked(false);

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

            // Clear selected categories since all checkboxes are unchecked
            selectedCategories.clear();

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

        TextView countText = new TextView(this);
        countText.setText(count + " places");
        countText.setTextSize(14);
        countText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

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
        cardLayout.addView(countText);
        cardLayout.addView(viewButton);

        card.addView(cardLayout);
        interestsLayout.addView(card);
    }

    private String getFormattedPlaceType(String type) {
        switch (type) {
            // Cultural and Educational
            case "museum": return "🏛 Museum";
            case "art_gallery": return "🖼 Art Gallery";
            case "tourist_attraction": return "📸 Tourist Attraction";
            case "landmark": return "🏛 Landmark";
            case "historical_landmark": return "🏺 Historical Landmark";
            case "historical_site": return "🏰 Historical Site";
            case "library": return "📚 Library";

            // Entertainment and Recreation
            case "park": return "🌳 Park";
            case "zoo": return "🦁 Zoo";
            case "amusement_park": return "🎢 Amusement Park";
            case "aquarium": return "🐠 Aquarium";
            case "stadium": return "🏟 Stadium";
            case "movie_theater": return "🎬 Movie Theater";
            case "theater": return "🎭 Theater";
            case "casino": return "🎰 Casino";
            case "night_club": return "🎶 Night Club";
            case "beach": return "🏖 Beach";

            // Religious and Cultural Sites
            case "church": return "⛪ Church";
            case "hindu_temple": return "🛕 Temple";
            case "mosque": return "🕌 Mosque";
            case "synagogue": return "🕍 Synagogue";
            case "place_of_worship": return "🙏 Place of Worship";

            // Food and Dining
            case "restaurant": return "🍽 Restaurant";
            case "cafe": return "☕ Cafe";
            case "bar": return "🍹 Bar";
            case "bakery": return "🥐 Bakery";
            case "food": return "🍲 Food";

            // Shopping
            case "shopping_mall": return "🛍 Shopping Mall";
            case "store": return "🏪 Store";
            case "book_store": return "📚 Book Store";
            case "clothing_store": return "👚 Clothing Store";
            case "department_store": return "🏬 Department Store";

            // Accommodation
            case "lodging": return "🏨 Hotel";

            // Transportation (relevant for tourists)
            case "train_station": return "🚂 Train Station";
            case "subway_station": return "🚇 Metro Station";
            case "bus_station": return "🚌 Bus Station";
            case "airport": return "✈️ Airport";

            // Wellness and Relaxation
            case "spa": return "💆 Spa";

            // Nature and Outdoor
            case "natural_feature": return "🏞 Natural Feature";
            case "campground": return "🏕 Campground";

            // Common Services for Tourists
            case "atm": return "💰 ATM";
            case "pharmacy": return "💊 Pharmacy";
            case "travel_agency": return "🧳 Travel Agency";
            case "post_office": return "📮 Post Office";

            case "point_of_interest": return "📍 Point of Interest";

            default: return null;
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
package com.example.tripcraft000;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.RectangularBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapPlacesActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        PlacesAdapter.OnPlaceClickListener,
        PlacesAdapter.OnPlaceSelectionChangedListener {


    private static final String PREF_SELECTED_PLACES = "selected_places";
    private static final int MAX_PLACES = 20;
    private static final int MAX_PHOTOS = 5;

    // Cache keys
    private static final String CACHE_KEY_PREFIX = "places_ids_cache_";
    // Static cache to persist selected place IDs across activity instances within the same app session
    // We only cache place IDs to comply with Google Places API terms
    private static Map<String, Set<String>> selectedPlacesCache = new HashMap<>();
    private static Map<String, Set<String>> placeIdsCache = new HashMap<>();

    private GoogleMap googleMap;
    private RecyclerView recyclerView;
    private PlacesAdapter placesAdapter;
    private TextView selectedCountText;
    private Button doneButton;
    private String startDate, endDate;
    private SearchView placeSearchView;

    private String selectedCityName;
    private String selectedCategoryName;
    private List<PlaceMarker> allPlaces = new ArrayList<>();
    private Map<String, Marker> placeMarkers = new HashMap<>();
    private LatLngBounds cityBounds;
    private String cacheKey;

    private RectangularBounds rectangularBounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_places);

        // Get city information from intent
        selectedCityName = getIntent().getStringExtra("city_name");
        double cityLat = getIntent().getDoubleExtra("city_lat", 0);
        double cityLng = getIntent().getDoubleExtra("city_lng", 0);
        startDate = getIntent().getStringExtra("start_date");
        endDate = getIntent().getStringExtra("end_date");
        selectedCategoryName = getIntent().getStringExtra("category_name");

        // Generate cache key based on city and category
        cacheKey = CACHE_KEY_PREFIX + selectedCityName + "_" + selectedCategoryName;

        placeSearchView = findViewById(R.id.placeSearchView);
        placeSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // no-op; we filter as-you-type
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (placesAdapter != null) {
                    placesAdapter.getFilter().filter(newText);
                }
                return true;
            }
        });

        // Set up the back button
        findViewById(R.id.backButton).setOnClickListener(v -> onBackPressed());

        // Set up the list toggle button
        findViewById(R.id.listFab).setOnClickListener(v -> toggleListView());

        // Set up the list container
        findViewById(R.id.listContainer).setVisibility(View.GONE);

        // Initialize UI components
        recyclerView = findViewById(R.id.placesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        selectedCountText = findViewById(R.id.selectedCountText);
        doneButton = findViewById(R.id.doneButton);
        doneButton.setOnClickListener(v -> finishWithSelectedPlaces());

        // Set title based on category
        TextView titleText = findViewById(R.id.titleText);
        if (titleText != null && selectedCategoryName != null) {
            titleText.setText(selectedCategoryName + " in " + selectedCityName);
        }

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Fetch city boundaries first
        fetchCityBoundaries(selectedCityName, bounds -> {
            if (bounds != null) {
                cityBounds = bounds;

                // Focus map on the city boundaries if available
                if (googleMap != null) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(cityBounds, 50));
                }
            }

            // Start fetching places from API - we don't cache actual place data
            // per Google Places API terms of service
            fetchPlacesForCategoryByName(selectedCityName, selectedCategoryName);
        });
    }

    private void fetchPlacesForCategoryByName(String selectedCityName, String selectedCategoryName) {
        List<String> typesToSearch = new ArrayList<>();

        switch (selectedCategoryName) {
            case "Museum":
                typesToSearch.add("museum");
                break;
            case "Tourist Attraction":
                typesToSearch.add("tourist_attraction");
                break;
            case "Theater":
                typesToSearch.add("concert_hall");
                typesToSearch.add("performing_arts_theater");
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

        for (String type : typesToSearch) {
            fetchPlacesForCategory(type);
        }
    }




    @Override
    protected void onPause() {
        super.onPause();
        // Save selected place IDs to the cache
        saveSelectionState();
    }

    private void saveSelectionState() {
        if (placesAdapter != null) {
            List<PlaceMarker> selectedPlaces = placesAdapter.getSelectedPlaces();
            Set<String> selectedIds = new HashSet<>();

            for (PlaceMarker place : selectedPlaces) {
                selectedIds.add(place.getPlaceId());
            }

            // Update selection cache
            selectedPlacesCache.put(cacheKey, selectedIds);

            Log.d("MapPlacesActivity", "Saved " + selectedIds.size() + " selected places to cache");
        }
    }

    private void toggleListView() {
        androidx.constraintlayout.widget.ConstraintLayout listContainer = findViewById(R.id.listContainer);
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton listFab = findViewById(R.id.listFab);

        // Toggle visibility
        boolean isListVisible = listContainer.getVisibility() == View.VISIBLE;

        // Update visibility
        listContainer.setVisibility(isListVisible ? View.GONE : View.VISIBLE);

        // Update FAB text
        listFab.setText(isListVisible ? "Show List" : "Hide List");
        listFab.setIcon(getDrawable(isListVisible ?
                R.drawable.ic_list :
                R.drawable.ic_map));
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Set up map click listener
        googleMap.setOnInfoWindowClickListener(marker -> {
            String placeId = (String) marker.getTag();
            if (placeId != null) {
                for (PlaceMarker place : allPlaces) {
                    if (place.getPlaceId().equals(placeId)) {
                        // Toggle selection
                        place.setSelected(!place.isSelected());
                        updateMarkerIcon(marker, place.isSelected());

                        // Update the adapter
                        placesAdapter.notifyDataSetChanged();
                        updateSelectedCount();

                        // Update the selection cache
                        saveSelectionState();
                        break;
                    }
                }
            }
        });


        // If we already have places (from cache), update markers
        if (!allPlaces.isEmpty() && cityBounds != null) {
            List<PlaceMarker> cityPlaces = allPlaces;
            updateMapMarkers(cityPlaces);
        }
    }

    private int calculateDuration() {
        // Validate that we have both dates
        if (startDate == null || endDate == null) {
            return 1; // Default to 1 day if dates aren't provided
        }

        try {
            // Parse the dates using a simple date format
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            if (start == null || end == null) {
                return 1; // Default to 1 day if parsing fails
            }

            // Calculate difference in milliseconds
            long diffInMillis = end.getTime() - start.getTime();

            // Convert to days and add 1 (to include both start and end date)
            int daysDiff = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;

            // Ensure we return at least 1 day
            return Math.max(1, daysDiff);
        } catch (ParseException e) {
            Log.e("MapPlacesActivity", "Error parsing dates", e);
            return 1; // Default to 1 day if parsing throws exception
        }
    }

    private void fetchCityBoundaries(String cityName, final BoundariesCallback callback) {
        Log.d("MapPlacesActivity", "Initializing Nominatim API request for city: " + cityName);

        // Create a URL for the Nominatim API request
        String encodedCityName;
        try {
            encodedCityName = URLEncoder.encode(cityName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("MapPlacesActivity", "Error encoding city name: " + e.getMessage());
            callback.onBoundariesFetched(null);
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

        Log.d("MapPlacesActivity", "Making request to Nominatim API: " + url);

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
                Log.e("MapPlacesActivity", "Nominatim API request failed: " + e.getMessage());
                runOnUiThread(() -> callback.onBoundariesFetched(null));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("MapPlacesActivity", "Unexpected code: " + response);
                    runOnUiThread(() -> callback.onBoundariesFetched(null));
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);

                    // Switch to main thread for UI updates
                    runOnUiThread(() -> {
                        try {
                            Log.d("MapPlacesActivity", "Response received from Nominatim API");

                            if (jsonArray.length() > 0) {
                                JSONObject place = jsonArray.getJSONObject(0);

                                // Extract the bounding box coordinates
                                JSONArray boundingBox = place.getJSONArray("boundingbox");
                                double south = Double.parseDouble(boundingBox.getString(0));
                                double north = Double.parseDouble(boundingBox.getString(1));
                                double west = Double.parseDouble(boundingBox.getString(2));
                                double east = Double.parseDouble(boundingBox.getString(3));

                                Log.d("MapPlacesActivity", "Bounding box for " + cityName + ": S=" + south +
                                        ", N=" + north + ", W=" + west + ", E=" + east);

                                // Create LatLngBounds similar to Google Maps viewport
                                LatLng southwest = new LatLng(south, west);
                                LatLng northeast = new LatLng(north, east);
                                rectangularBounds = RectangularBounds.newInstance(southwest, northeast);



                                Log.d("MapPlacesActivity", "City bounds created successfully");

                                // Pass the boundaries back through the callback
                                callback.onBoundariesFetched(cityBounds);
                            } else {
                                Log.e("MapPlacesActivity", "No results found for " + cityName);
                                callback.onBoundariesFetched(null);
                            }
                        } catch (JSONException e) {
                            Log.e("MapPlacesActivity", "Error parsing Nominatim response: " + e.getMessage());
                            callback.onBoundariesFetched(null);
                        }
                    });
                } catch (JSONException e) {
                    Log.e("MapPlacesActivity", "Error parsing JSON: " + e.getMessage());
                    runOnUiThread(() -> callback.onBoundariesFetched(null));
                }
            }
        });
    }

    // Callback interface for asynchronous boundaries fetching
    interface BoundariesCallback {
        void onBoundariesFetched(LatLngBounds bounds);
    }


    // Haversine distance in meters
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000; // Radius of Earth in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

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

    private LatLng getCenterFromBounds(RectangularBounds bounds) {
        double lat = (bounds.getSouthwest().latitude + bounds.getNortheast().latitude) / 2.0;
        double lng = (bounds.getSouthwest().longitude + bounds.getNortheast().longitude) / 2.0;
        return new LatLng(lat, lng);
    }


    private void fetchPlacesForCategory(String categoryName) {
        if (rectangularBounds == null) {
            Toast.makeText(this, "Bounds not available yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        String apiKey = getString(R.string.google_api_key);
        String baseUrl = "https://places.googleapis.com/v1/places:searchNearby";

        // Format category name
        String category = categoryName.trim().toLowerCase().replace(" ", "_");

        LatLng center = getCenterFromBounds(rectangularBounds);
        double centerLat = center.latitude;
        double centerLng = center.longitude;
        double radius = calculateRadiusFromBounds(rectangularBounds);

        // Build JSON body
        JSONObject requestBody = new JSONObject();
        try {
            JSONObject centerObj = new JSONObject();
            centerObj.put("latitude", centerLat);
            centerObj.put("longitude", centerLng);

            JSONObject circle = new JSONObject();
            circle.put("center", centerObj);
            circle.put("radius", radius);

            JSONObject locationRestriction = new JSONObject();
            locationRestriction.put("circle", circle);

            JSONArray includedTypes = new JSONArray();
            includedTypes.put(category);

            requestBody.put("locationRestriction", locationRestriction);
            requestBody.put("includedTypes", includedTypes);
            requestBody.put("maxResultCount", MAX_PLACES);
        } catch (JSONException e) {
            Log.e("MapPlacesActivity", "JSON build error", e);
            return;
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(baseUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Goog-FieldMask",
                        "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount,places.photos,places.types")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MapPlacesActivity", "Network request failed", e);
                runOnUiThread(() -> Toast.makeText(MapPlacesActivity.this,
                        "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e("MapPlacesActivity", "API error: " + errorBody);
                    runOnUiThread(() -> Toast.makeText(MapPlacesActivity.this,
                            "Error fetching places: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    runOnUiThread(() -> handlePlacesApiResponse(jsonResponse));
                } catch (JSONException e) {
                    Log.e("MapPlacesActivity", "JSON parsing error", e);
                    runOnUiThread(() -> Toast.makeText(MapPlacesActivity.this,
                            "Failed to parse response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }




    private void handlePlacesApiResponse(JSONObject response) {
        try {
            JSONArray results = response.has("places") ? response.getJSONArray("places") : new JSONArray();
            List<PlaceMarker> page = new ArrayList<>();
            Set<String> placeIds = new HashSet<>();

            for (int i = 0; i < results.length() && i < MAX_PLACES; i++) {
                JSONObject placeJson = results.getJSONObject(i);
                PlaceMarker marker = convertJsonToPlaceMarkerV1(placeJson);

                // Track place IDs for future reference
                placeIds.add(marker.getPlaceId());

                // Check if this place should be selected from our cached selection
                if (selectedPlacesCache.containsKey(cacheKey) &&
                        selectedPlacesCache.get(cacheKey).contains(marker.getPlaceId())) {
                    marker.setSelected(true);
                }

                page.add(marker);
            }

            allPlaces.addAll(page);

            // Cache just the place IDs, not the data (complies with Google Terms)
            placeIdsCache.put(cacheKey, placeIds);
            Log.d("MapPlacesActivity", "Saved " + placeIds.size() + " place IDs to cache for " + cacheKey);

            List<PlaceMarker> cityPlaces = allPlaces;
            updatePlacesDisplay(cityPlaces);

            // Update title with count
            TextView titleText = findViewById(R.id.titleText);
            if (titleText != null && selectedCategoryName != null) {
                titleText.setText(String.format(
                        "%s in %s (%d places)",
                        selectedCategoryName,
                        selectedCityName,
                        cityPlaces.size()
                ));
            }

        } catch (JSONException e) {
            Log.e("MapPlacesActivity", "Error parsing places response", e);
        }
    }

    // New method to convert JSON from Places API v1 to PlaceMarker
    private PlaceMarker convertJsonToPlaceMarkerV1(JSONObject placeJson) throws JSONException {
        String placeId = placeJson.getString("id");

        // Extract display name
        String name = placeJson.has("displayName") ?
                placeJson.getJSONObject("displayName").optString("text", "") :
                "Unnamed Place";

        // Extract address/vicinity
        String vicinity = placeJson.has("formattedAddress") ?
                placeJson.getString("formattedAddress") :
                "";

        // Extract location
        double lat = 0, lng = 0;
        if (placeJson.has("location")) {
            JSONObject location = placeJson.getJSONObject("location");
            lat = location.getDouble("latitude");
            lng = location.getDouble("longitude");
        }

        // Extract rating
        double rating = placeJson.optDouble("rating", 0);

        // Extract user ratings total
        int userRatingsTotal = placeJson.optInt("userRatingCount", 0); // or "user_ratings_total" if field name differs

        // Extract photos
        List<String> photoUrls = new ArrayList<>();
        if (placeJson.has("photos")) {
            JSONArray photosArray = placeJson.getJSONArray("photos");
            for (int i = 0; i < photosArray.length(); i++) {
                JSONObject photo = photosArray.getJSONObject(i);
                if (photo.has("name")) {
                    String photoReference = photo.getString("name");
                    String apiKey = getString(R.string.google_api_key);
                    String photoUrl = "https://places.googleapis.com/v1/" + photoReference
                            + "/media?key=" + apiKey + "&maxHeightPx=400&maxWidthPx=400";
                    photoUrls.add(photoUrl);
                }
            }
        }

        return new PlaceMarker(placeId, name, vicinity, lat, lng, rating, userRatingsTotal, photoUrls);

    }




    private void updatePlacesDisplay(List<PlaceMarker> places) {
    // Update RecyclerView
    placesAdapter = new PlacesAdapter(places, this, this);
    recyclerView.setAdapter(placesAdapter);

    // Update map markers
    updateMapMarkers(places);

    // Update selected count
    updateSelectedCount();
}

private void updateMapMarkers(List<PlaceMarker> places) {
    if (googleMap == null) return;

    // Clear existing markers
    googleMap.clear();
    placeMarkers.clear();

    // If there are no places, return
    if (places.isEmpty()) {
        Toast.makeText(this, "No places found", Toast.LENGTH_SHORT).show();
        return;
    }

    // Add markers for all places and build bounds
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

    for (PlaceMarker place : places) {
        LatLng position = new LatLng(place.getLatitude(), place.getLongitude());
        boundsBuilder.include(position);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(place.getName())
                .snippet(place.getVicinity());

        // Set marker color based on selection
        updateMarkerOptions(markerOptions, place.isSelected());

        Marker marker = googleMap.addMarker(markerOptions);
        if (marker != null) {
            marker.setTag(place.getPlaceId());
            placeMarkers.put(place.getPlaceId(), marker);
        }
    }

    // Zoom to show all markers
    try {
        LatLngBounds bounds = boundsBuilder.build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    } catch (IllegalStateException e) {
        // Handle the case where no points were added to the builder
        Log.e("MapPlacesActivity", "No valid locations to build bounds", e);
    }
}

private void updateMarkerOptions(MarkerOptions options, boolean isSelected) {
    if (isSelected) {
        // Selected places: green
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
    } else {
        // Normal places: red (default)
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
    }
}

private void updateMarkerIcon(Marker marker, boolean isSelected) {
    if (marker != null) {
        float hue = isSelected ?
                BitmapDescriptorFactory.HUE_GREEN :
                BitmapDescriptorFactory.HUE_RED;

        marker.setIcon(BitmapDescriptorFactory.defaultMarker(hue));
    }
}

private void updateSelectedCount() {
    if (placesAdapter == null) return;

    List<PlaceMarker> selectedPlaces = placesAdapter.getSelectedPlaces();
    int duration = calculateDuration();
    int maxAllowed = duration * 2; // 2 places per day

    // Count format: Selected/Max
    String countText = String.format("%d / %d places selected",
            selectedPlaces.size(), maxAllowed);
    selectedCountText.setText(countText);

    // Disable Done button if too many places selected
    doneButton.setEnabled(selectedPlaces.size() <= maxAllowed);

    // Show warning if too many places are selected
    if (selectedPlaces.size() > maxAllowed) {
        Toast.makeText(this,
                "You can select maximum " + maxAllowed + " places for a " +
                        duration + "-day trip", Toast.LENGTH_SHORT).show();
    }
}

private void finishWithSelectedPlaces() {
    List<PlaceMarker> selectedPlaces = placesAdapter.getSelectedPlaces();
    int duration = calculateDuration();
    int maxAllowed = duration * 2;

    if (selectedPlaces.isEmpty()) {
        Toast.makeText(this, "Please select at least one place", Toast.LENGTH_SHORT).show();
        return;
    }

    if (selectedPlaces.size() > maxAllowed) {
        Toast.makeText(this,
                "You can select maximum " + maxAllowed + " places for a " +
                        duration + "-day trip", Toast.LENGTH_SHORT).show();
        return;
    }

    Intent resultIntent = new Intent();
    ArrayList<String> selectedPlaceIds = new ArrayList<>();
    ArrayList<String> selectedPlaceNames = new ArrayList<>();

    for (PlaceMarker place : selectedPlaces) {
        selectedPlaceIds.add(place.getPlaceId());
        selectedPlaceNames.add(place.getName());
    }
    resultIntent.putStringArrayListExtra("selected_place_ids", selectedPlaceIds);

    resultIntent.putStringArrayListExtra("selected_place_names", selectedPlaceNames);

    setResult(RESULT_OK, resultIntent);
    finish();
}

@Override
public void onPlaceClick(PlaceMarker place) {
    // When a place is clicked in the list, focus the map on it
    if (googleMap != null) {
        LatLng position = new LatLng(place.getLatitude(), place.getLongitude());
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));

        // Show info window for the marker
        Marker marker = placeMarkers.get(place.getPlaceId());
        if (marker != null) {
            marker.showInfoWindow();
        }
    }
}

@Override
public void onPlaceSelectionChanged(PlaceMarker place, boolean isSelected) {
    // Update marker icon on the map
    Marker marker = placeMarkers.get(place.getPlaceId());
    updateMarkerIcon(marker, place.isSelected());

    // Update the selected count
    updateSelectedCount();

    // Save selection state to cache
    saveSelectionState();
}

// Clear all static caches when application terminates
public static void clearCache() {
    selectedPlacesCache.clear();
}

    public static class PlaceMarker {
        private final String placeId;
        private final String name;
        private final String vicinity;
        private final double latitude;
        private final double longitude;
        private final double rating;
        private final int userRatingsTotal;
        private final List<String> photoUrls;
        private boolean selected;

        public PlaceMarker(String placeId, String name, String vicinity, double latitude, double longitude,
                           double rating, int userRatingsTotal, List<String> photoUrls) {
            this.placeId = placeId;
            this.name = name;
            this.vicinity = vicinity;
            this.latitude = latitude;
            this.longitude = longitude;
            this.rating = rating;
            this.userRatingsTotal = userRatingsTotal;
            this.photoUrls = photoUrls != null ? photoUrls : new ArrayList<>();
            this.selected = false;
        }

        public String getPlaceId() { return placeId; }

        public String getName() { return name; }

        public String getVicinity() { return vicinity; }

        public double getLatitude() { return latitude; }

        public double getLongitude() { return longitude; }

        public float getRating() { return (float) rating; }

        public int getUserRatingsTotal() { return userRatingsTotal; }

        public List<String> getPhotoUrls() { return photoUrls; }


        public boolean isSelected() { return selected; }

        public void setSelected(boolean selected) { this.selected = selected; }
    }

}
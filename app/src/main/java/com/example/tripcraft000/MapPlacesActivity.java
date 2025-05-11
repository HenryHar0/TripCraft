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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
    private LatLng selectedCityCoordinates;
    private String selectedCategoryName;
    private List<PlaceMarker> allPlaces = new ArrayList<>();
    private Map<String, Marker> placeMarkers = new HashMap<>();
    private LatLngBounds cityBounds;
    private String cacheKey;

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
            selectedCityCoordinates = new LatLng(cityLat, cityLng);
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
                fetchPlacesForCategory(selectedCityName, selectedCategoryName);
            });
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

    // Set initial camera position to the city
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedCityCoordinates, 12));

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

    // If we already have city bounds, zoom to them
    if (cityBounds != null) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(cityBounds, 50));
    }

    // If we already have places (from cache), update markers
    if (!allPlaces.isEmpty() && cityBounds != null) {
        List<PlaceMarker> cityPlaces = filterPlacesToCityOnly(allPlaces, cityBounds);
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
                            LatLngBounds cityBounds = new LatLngBounds(southwest, northeast);

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

// Method to check if a place is within city boundaries
private boolean isWithinCityBoundaries(double placeLat, double placeLng, LatLngBounds cityBounds) {
    // If we have boundaries from the API, use them
    if (cityBounds != null) {
        return cityBounds.contains(new LatLng(placeLat, placeLng));
    }

    // Fallback to radius-based approach if we couldn't get boundaries
    LatLng cityCenter = selectedCityCoordinates;

    double distance = calculateDistance(
            cityCenter.latitude, cityCenter.longitude,
            placeLat, placeLng
    );

    // Use a default radius of 7km if boundaries couldn't be fetched
    return distance <= 7;
}

// Calculate distance between two points using Haversine formula
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

// Update filtering method to use boundaries
private List<PlaceMarker> filterPlacesToCityOnly(List<PlaceMarker> allPlaces, LatLngBounds cityBounds) {
    List<PlaceMarker> cityPlaces = new ArrayList<>();

    for (PlaceMarker place : allPlaces) {
        if (isWithinCityBoundaries(place.getLatitude(), place.getLongitude(), cityBounds)) {
            cityPlaces.add(place);
        }
    }

    Log.d("MapPlacesActivity", "Filtered " + allPlaces.size() +
            " places down to " + cityPlaces.size() + " in city");

    return cityPlaces;
}

private void fetchPlacesForCategory(String cityName, String categoryName) {
    // Extract actual category name (remove emoji if present)
    String category = categoryName;
    if (category.contains(" ")) {
        category = category.substring(category.indexOf(" ") + 1);
    }

    // Create search query
    String query;
    try {
        query = URLEncoder.encode(category + " in " + cityName, "UTF-8");
    } catch (UnsupportedEncodingException e) {
        Log.e("MapPlacesActivity", "Error encoding query", e);
        Toast.makeText(this, "Error creating search query", Toast.LENGTH_SHORT).show();
        return;
    }

    // Build Text Search URL for a single request
    String apiKey = getString(R.string.google_api_key);
    String url = "https://maps.googleapis.com/maps/api/place/textsearch/json"
            + "?query=" + query
            + "&key=" + apiKey;

    allPlaces.clear();
    fetchPlacesFromUrl(url);
}

private void fetchPlacesFromUrl(final String url) {
    new Thread(() -> {
        try {
            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            bufferedReader.close();

            final JSONObject jsonResponse = new JSONObject(response.toString());
            runOnUiThread(() -> handlePlacesApiResponse(jsonResponse));

        } catch (Exception e) {
            Log.e("MapPlacesActivity", "Error fetching places", e);
            runOnUiThread(() -> Toast.makeText(MapPlacesActivity.this,
                    "Error fetching places", Toast.LENGTH_SHORT).show());
        }
    }).start();
}

private void handlePlacesApiResponse(JSONObject response) {
    try {
        JSONArray results = response.getJSONArray("results");
        List<PlaceMarker> page = new ArrayList<>();
        Set<String> placeIds = new HashSet<>();

        for (int i = 0; i < results.length() && i < MAX_PLACES; i++) {
            JSONObject placeJson = results.getJSONObject(i);
            PlaceMarker marker = convertJsonToPlaceMarker(placeJson);

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

        List<PlaceMarker> cityPlaces = filterPlacesToCityOnly(allPlaces, cityBounds);
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

/**
 * Convert a Place JSON object into a PlaceMarker instance.
 */
private PlaceMarker convertJsonToPlaceMarker(JSONObject placeJson) throws JSONException {
    String placeId = placeJson.optString("place_id");
    String name = placeJson.optString("name");
    String vicinity = placeJson.optString("vicinity", placeJson.optString("formatted_address", ""));

    JSONObject location = placeJson.getJSONObject("geometry").getJSONObject("location");
    double lat = location.getDouble("lat");
    double lng = location.getDouble("lng");

    double rating = placeJson.optDouble("rating", 0.0);

    // Build photo URLs if available
    List<String> photoUrls = new ArrayList<>();
    if (placeJson.has("photos")) {
        JSONArray photos = placeJson.getJSONArray("photos");
        String apiKey = getString(R.string.google_api_key);
        for (int j = 0; j < Math.min(photos.length(), MAX_PHOTOS); j++) {
            JSONObject photo = photos.getJSONObject(j);
            String ref = photo.optString("photo_reference");
            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                    + "?maxwidth=400"
                    + "&photoreference=" + ref
                    + "&key=" + apiKey;
            photoUrls.add(photoUrl);
        }
    }

    PlaceMarker marker = new PlaceMarker(placeId, name, vicinity, lat, lng, rating, photoUrls);
    return marker;
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
    private final List<String> photoUrls;
    private boolean selected;

    public PlaceMarker(String placeId, String name, String vicinity, double latitude, double longitude,
                       double rating, List<String> photoUrls) {
        this.placeId = placeId;
        this.name = name;
        this.vicinity = vicinity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rating = rating;
        this.photoUrls = photoUrls;
        this.selected = false;
    }

    public String getPlaceId() { return placeId; }
    public String getName() { return name; }
    public String getVicinity() { return vicinity; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getRating() { return (float) rating; }
    public List<String> getPhotoUrls() { return photoUrls; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
}
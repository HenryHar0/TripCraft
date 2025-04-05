package com.example.tripcraft000;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapPlacesActivity extends AppCompatActivity implements OnMapReadyCallback,
        PlacesAdapter.OnPlaceSelectionChangedListener {

    private static final String TAG = "MapPlacesActivity";
    private GoogleMap mMap;
    private LatLng cityCoordinates;
    private String cityName;
    private String categoryName;
    private String rawCategoryName;
    private final String API_KEY = "AIzaSyCYnYiiqrHO0uwKoxNQLA_mKEIuX1aRyL4";
    private TextView mapTitleText;
    private TextView statusText;
    private CardView statusCard;
    private static final Map<String, String> CATEGORY_TO_TYPE_MAP = new HashMap<>();

    private View mapContainer;
    private RecyclerView placesRecyclerView;
    private PlacesAdapter placesAdapter;
    private List<PlaceMarker> placesList = new ArrayList<>();
    private ExtendedFloatingActionButton listFab;
    private boolean isShowingMap = true;

    // For pagination handling
    private List<PlaceMarker> allPlacesList = new ArrayList<>();
    private boolean isLoadingMorePlaces = false;
    private static final int PAGINATION_DELAY = 2000; // 2 seconds delay between pagination requests

    private Map<String, Marker> markersMap = new HashMap<>();
    private ExecutorService executor;

    static {
        CATEGORY_TO_TYPE_MAP.put("🏛 Museum", "museum");
        CATEGORY_TO_TYPE_MAP.put("🖼 Art Gallery", "art_gallery");
        CATEGORY_TO_TYPE_MAP.put("📸 Tourist Attraction", "tourist_attraction");
        CATEGORY_TO_TYPE_MAP.put("🏛 Landmark", "tourist_attraction");
        CATEGORY_TO_TYPE_MAP.put("🏺 Historical Landmark", "tourist_attraction");
        CATEGORY_TO_TYPE_MAP.put("🏰 Historical Site", "tourist_attraction");
        CATEGORY_TO_TYPE_MAP.put("📚 Library", "library");
        CATEGORY_TO_TYPE_MAP.put("🌳 Park", "park");
        CATEGORY_TO_TYPE_MAP.put("🦁 Zoo", "zoo");
        CATEGORY_TO_TYPE_MAP.put("🎢 Amusement Park", "amusement_park");
        CATEGORY_TO_TYPE_MAP.put("🐠 Aquarium", "aquarium");
        CATEGORY_TO_TYPE_MAP.put("🏟 Stadium", "stadium");
        CATEGORY_TO_TYPE_MAP.put("🎬 Movie Theater", "movie_theater");
        CATEGORY_TO_TYPE_MAP.put("🎭 Theater", "movie_theater");
        CATEGORY_TO_TYPE_MAP.put("🎰 Casino", "casino");
        CATEGORY_TO_TYPE_MAP.put("🎶 Night Club", "night_club");
        CATEGORY_TO_TYPE_MAP.put("🏖 Beach", "natural_feature");
        CATEGORY_TO_TYPE_MAP.put("⛪ Church", "church");
        CATEGORY_TO_TYPE_MAP.put("🛕 Temple", "hindu_temple");
        CATEGORY_TO_TYPE_MAP.put("🕌 Mosque", "mosque");
        CATEGORY_TO_TYPE_MAP.put("🕍 Synagogue", "synagogue");
        CATEGORY_TO_TYPE_MAP.put("🙏 Place of Worship", "place_of_worship");
        CATEGORY_TO_TYPE_MAP.put("🍽 Restaurant", "restaurant");
        CATEGORY_TO_TYPE_MAP.put("☕ Cafe", "cafe");
        CATEGORY_TO_TYPE_MAP.put("🍹 Bar", "bar");
        CATEGORY_TO_TYPE_MAP.put("🥐 Bakery", "bakery");
        CATEGORY_TO_TYPE_MAP.put("🍲 Food", "restaurant");
        CATEGORY_TO_TYPE_MAP.put("🛍 Shopping Mall", "shopping_mall");
        CATEGORY_TO_TYPE_MAP.put("🏪 Store", "store");
        CATEGORY_TO_TYPE_MAP.put("📚 Book Store", "book_store");
        CATEGORY_TO_TYPE_MAP.put("👚 Clothing Store", "clothing_store");
        CATEGORY_TO_TYPE_MAP.put("🏬 Department Store", "department_store");
        CATEGORY_TO_TYPE_MAP.put("🏨 Hotel", "lodging");
        CATEGORY_TO_TYPE_MAP.put("🚂 Train Station", "train_station");
        CATEGORY_TO_TYPE_MAP.put("🚇 Metro Station", "subway_station");
        CATEGORY_TO_TYPE_MAP.put("🚌 Bus Station", "bus_station");
        CATEGORY_TO_TYPE_MAP.put("✈️ Airport", "airport");
        CATEGORY_TO_TYPE_MAP.put("💆 Spa", "spa");
        CATEGORY_TO_TYPE_MAP.put("🏞 Natural Feature", "natural_feature");
        CATEGORY_TO_TYPE_MAP.put("🏕 Campground", "campground");
        CATEGORY_TO_TYPE_MAP.put("💰 ATM", "atm");
        CATEGORY_TO_TYPE_MAP.put("💊 Pharmacy", "pharmacy");
        CATEGORY_TO_TYPE_MAP.put("🧳 Travel Agency", "travel_agency");
        CATEGORY_TO_TYPE_MAP.put("📮 Post Office", "post_office");
        CATEGORY_TO_TYPE_MAP.put("📍 Point of Interest", "point_of_interest");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_places);

        executor = Executors.newSingleThreadExecutor();

        cityName = getIntent().getStringExtra("city_name");
        categoryName = getIntent().getStringExtra("category_name");
        rawCategoryName = categoryName;
        double lat = getIntent().getDoubleExtra("city_lat", 0);
        double lng = getIntent().getDoubleExtra("city_lng", 0);
        cityCoordinates = new LatLng(lat, lng);

        Log.d(TAG, "City: " + cityName + ", Category: " + categoryName + ", Coords: " + lat + "," + lng);

        setupUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupRecyclerView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void setupUI() {
        Toolbar toolbar = findViewById(R.id.mapToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mapTitleText = findViewById(R.id.mapTitleText);
        statusText = findViewById(R.id.statusText);
        statusCard = findViewById(R.id.statusCard);
        ImageButton backButton = findViewById(R.id.backButton);
        listFab = findViewById(R.id.listFab);
        mapContainer = findViewById(R.id.mapContainer);
        placesRecyclerView = findViewById(R.id.placesRecyclerView);

        mapTitleText.setText(categoryName + " in " + cityName);
        statusText.setText("Loading " + categoryName + "...");
        statusCard.setVisibility(View.VISIBLE);

        placesRecyclerView.setVisibility(View.GONE);
        mapContainer.setVisibility(View.VISIBLE);
        listFab.setText("Show List");
        listFab.setIconResource(R.drawable.ic_list);
        listFab.setEnabled(false);

        backButton.setOnClickListener(v -> finish());
        listFab.setOnClickListener(v -> toggleView());
    }

    private void setupRecyclerView() {
        placesAdapter = new PlacesAdapter(placesList, this::onPlaceItemClick, this);
        placesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        placesRecyclerView.setAdapter(placesAdapter);
    }

    private void toggleView() {
        isShowingMap = !isShowingMap;

        if (isShowingMap) {
            mapContainer.setVisibility(View.VISIBLE);
            placesRecyclerView.setVisibility(View.GONE);
            listFab.setText("Show List");
            listFab.setIconResource(R.drawable.ic_list);
        } else {
            mapContainer.setVisibility(View.GONE);
            placesRecyclerView.setVisibility(View.VISIBLE);
            listFab.setText("Show Map");
            listFab.setIconResource(R.drawable.ic_map);
        }
    }

    public void onPlaceSelectionChanged(PlaceMarker place, boolean isSelected, boolean isMandatory) {
        // Handle selection change here
        // For now, we can just log it or do nothing
        Log.d(TAG, "Place selection changed: " + place.getName() +
                " - Selected: " + isSelected +
                " - Mandatory: " + isMandatory);
    }

    private void onPlaceItemClick(PlaceMarker place) {
        if (!isShowingMap) {
            toggleView();
        }

        if (mMap != null) {
            LatLng location = new LatLng(place.getLatitude(), place.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16));

            String markerId = place.getPlaceId();
            Marker marker = markersMap.get(markerId);
            if (marker != null) {
                marker.showInfoWindow();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cityCoordinates, 13));

        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Clear lists before fetching
        allPlacesList.clear();
        placesList.clear();

        fetchPlacesByCategory();
    }

    private void fetchPlacesByCategory() {
        Log.d(TAG, "Fetching places for category: " + categoryName);
        statusText.setText("Searching for " + categoryName + " in " + cityName + "...");

        // First try with nearby search
        String placeType = getPlaceTypeFromCategory();
        Log.d(TAG, "Converted to place type: " + placeType);

        if (placeType != null) {
            String nearbyUrl = buildNearbySearchUrl(placeType);
            Log.d(TAG, "Making nearby search request: " + nearbyUrl);
            fetchPlacesFromUrl(nearbyUrl, true, null);
        } else {
            // If we can't determine the place type, go straight to text search
            Log.d(TAG, "Cannot determine place type, using text search");
            fallbackToTextSearch();
        }
    }

    private String getPlaceTypeFromCategory() {
        // Try the direct mapping first
        String placeType = CATEGORY_TO_TYPE_MAP.get(categoryName);

        // If not found and category has emoji prefix, try removing emoji
        if (placeType == null && categoryName.contains(" ")) {
            String categoryWithoutEmoji = categoryName.substring(categoryName.indexOf(" ") + 1);
            placeType = CATEGORY_TO_TYPE_MAP.get(categoryWithoutEmoji);

            // If still not found, use the category name without emoji as the type
            if (placeType == null) {
                placeType = categoryWithoutEmoji.toLowerCase().replace(" ", "_");
            }
        }

        return placeType;
    }

    private String buildNearbySearchUrl(String placeType) {
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + cityCoordinates.latitude + "," + cityCoordinates.longitude +
                "&radius=10000" + // Increased radius from 5000 to 10000
                "&type=" + placeType +
                "&key=" + API_KEY;
    }

    private String buildNearbySearchUrlWithToken(String nextPageToken) {
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?pagetoken=" + nextPageToken +
                "&key=" + API_KEY;
    }

    private void fallbackToTextSearch() {
        Log.d(TAG, "Falling back to text search for: " + categoryName);
        statusText.setText("Trying alternative search method...");

        try {
            String queryText = categoryName;
            // Remove emoji if present
            if (queryText.contains(" ")) {
                queryText = queryText.substring(queryText.indexOf(" ") + 1);
            }

            String query = URLEncoder.encode(queryText + " in " + cityName, "UTF-8");
            String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                    "?query=" + query +
                    "&key=" + API_KEY;

            Log.d(TAG, "Making text search request: " + url);
            fetchPlacesFromUrl(url, false, null);
        } catch (Exception e) {
            Log.e(TAG, "Error encoding URL", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Error searching for places", Toast.LENGTH_SHORT).show();
                statusText.setText("Error searching for places");
            });
        }
    }

    private String buildTextSearchUrlWithToken(String nextPageToken) {
        return "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                "?pagetoken=" + nextPageToken +
                "&key=" + API_KEY;
    }

    private void fetchPlacesFromUrl(final String url, final boolean canFallback, final String searchType) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            StringBuilder response = new StringBuilder();

            try {
                // Setup connection
                URL requestUrl = new URL(url);
                connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                // Check if the connection was successful
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "API Response code: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP Error: " + responseCode);
                    throw new IOException("HTTP error code: " + responseCode);
                }

                // Read the response
                try (InputStream inputStream = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                // Log a small portion of the response for debugging
                String responsePreview = response.length() > 500 ?
                        response.substring(0, 500) + "..." : response.toString();
                Log.d(TAG, "API Response preview: " + responsePreview);

                // Parse the response
                JSONObject jsonResponse = new JSONObject(response.toString());
                String status = jsonResponse.getString("status");
                Log.d(TAG, "Response status: " + status);

                if ("OK".equals(status)) {
                    final List<PlaceMarker> places = parsePlacesResponse(jsonResponse);
                    Log.d(TAG, "Places found in this batch: " + places.size());

                    // Check for next page token
                    final String nextPageToken = jsonResponse.has("next_page_token") ?
                            jsonResponse.getString("next_page_token") : null;
                    Log.d(TAG, "Next page token: " + (nextPageToken != null ? "exists" : "none"));

                    // Add current batch to all places list
                    allPlacesList.addAll(places);

                    if (places.isEmpty() && canFallback && allPlacesList.isEmpty()) {
                        Log.d(TAG, "No places found, falling back to text search");
                        fallbackToTextSearch();
                    } else {
                        runOnUiThread(() -> {
                            // Update UI with all places collected so far
                            updatePlacesList(allPlacesList);
                            addMarkersToMap(allPlacesList);

                            // If there's a next page, fetch it after a delay (API requires delay between pagination requests)
                            if (nextPageToken != null && !nextPageToken.isEmpty()) {
                                statusText.setText("Loading more " + rawCategoryName + "...");

                                // Determine whether we're doing a nearby search or text search
                                final String currentSearchType = searchType != null ? searchType :
                                        (url.contains("nearbysearch") ? "nearby" : "text");

                                // Delay before making the next request (required by Places API)
                                isLoadingMorePlaces = true;
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(PAGINATION_DELAY);

                                        // Build URL for next page
                                        String nextPageUrl;
                                        if ("nearby".equals(currentSearchType)) {
                                            nextPageUrl = buildNearbySearchUrlWithToken(nextPageToken);
                                        } else {
                                            nextPageUrl = buildTextSearchUrlWithToken(nextPageToken);
                                        }

                                        // Fetch next page (not fallback-able)
                                        fetchPlacesFromUrl(nextPageUrl, false, currentSearchType);
                                    } catch (InterruptedException e) {
                                        Log.e(TAG, "Pagination delay interrupted", e);
                                    } finally {
                                        isLoadingMorePlaces = false;
                                    }
                                }).start();
                            } else {
                                // No more pages, show final count
                                runOnUiThread(() -> {
                                    String finalMessage = "Found " + allPlacesList.size() + " " + rawCategoryName;
                                    statusText.setText(finalMessage);

                                    // Hide status card after delay
                                    if (!isLoadingMorePlaces) {
                                        statusCard.postDelayed(() -> {
                                            statusCard.animate()
                                                    .alpha(0f)
                                                    .setDuration(500)
                                                    .withEndAction(() -> statusCard.setVisibility(View.GONE))
                                                    .start();
                                        }, 3000);
                                    }
                                });
                            }
                        });
                    }
                } else if (canFallback && allPlacesList.isEmpty()) {
                    Log.w(TAG, "API returned status: " + status + ", falling back to text search");
                    fallbackToTextSearch();
                } else {
                    Log.e(TAG, "API returned error status: " + status);
                    runOnUiThread(() -> {
                        // If we already have some places, show them despite the error
                        if (!allPlacesList.isEmpty()) {
                            updatePlacesList(allPlacesList);
                            addMarkersToMap(allPlacesList);
                            statusText.setText("Found " + allPlacesList.size() + " " + rawCategoryName + " (could not load more)");
                        } else {
                            Toast.makeText(MapPlacesActivity.this,
                                    "Error: " + status, Toast.LENGTH_SHORT).show();
                            statusText.setText("Error: " + status);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching places: " + e.getMessage(), e);
                if (canFallback && allPlacesList.isEmpty()) {
                    fallbackToTextSearch();
                } else {
                    runOnUiThread(() -> {
                        // If we already have some places, show them despite the error
                        if (!allPlacesList.isEmpty()) {
                            updatePlacesList(allPlacesList);
                            addMarkersToMap(allPlacesList);
                            statusText.setText("Found " + allPlacesList.size() + " " + rawCategoryName + " (could not load more)");
                        } else {
                            Toast.makeText(MapPlacesActivity.this,
                                    "Error loading places: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            statusText.setText("Error loading places");
                        }
                    });
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private List<PlaceMarker> parsePlacesResponse(JSONObject response) {
        List<PlaceMarker> placesList = new ArrayList<>();

        try {
            JSONArray results = response.getJSONArray("results");
            Log.d(TAG, "Parsing " + results.length() + " places");

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);

                String placeId = place.getString("place_id");
                String name = place.getString("name");

                // Handle different response formats (nearby vs text search)
                String vicinity;
                if (place.has("vicinity")) {
                    vicinity = place.getString("vicinity");
                } else if (place.has("formatted_address")) {
                    vicinity = place.getString("formatted_address");
                } else {
                    vicinity = "Address not available";
                }

                JSONObject geometry = place.getJSONObject("geometry");
                JSONObject location = geometry.getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");

                float rating = 0;
                if (place.has("rating")) {
                    rating = (float) place.getDouble("rating");
                }

                List<String> photoUrls = new ArrayList<>();
                if (place.has("photos")) {
                    JSONArray photos = place.getJSONArray("photos");
                    for (int j = 0; j < Math.min(photos.length(), 5); j++) { // Limit to 5 photos
                        JSONObject photo = photos.getJSONObject(j);
                        String photoReference = photo.getString("photo_reference");
                        String photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                                "?maxwidth=400" +
                                "&photoreference=" + photoReference +
                                "&key=" + API_KEY;
                        photoUrls.add(photoUrl);
                    }
                }

                // Skip duplicates (based on placeId)
                boolean isDuplicate = false;
                for (PlaceMarker existingPlace : allPlacesList) {
                    if (existingPlace.getPlaceId().equals(placeId)) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (!isDuplicate) {
                    PlaceMarker placeMarker = new PlaceMarker(
                            placeId,
                            name,
                            vicinity,
                            lat,
                            lng,
                            rating,
                            photoUrls
                    );

                    Log.d(TAG, "Added place: " + name + " at " + lat + "," + lng);
                    placesList.add(placeMarker);
                } else {
                    Log.d(TAG, "Skipped duplicate place: " + name + " (ID: " + placeId + ")");
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing places response", e);
        }

        return placesList;
    }

    private void updatePlacesList(List<PlaceMarker> places) {
        placesList.clear();
        placesList.addAll(places);
        placesAdapter.notifyDataSetChanged();

        listFab.setEnabled(!places.isEmpty());
    }

    private void addMarkersToMap(List<PlaceMarker> places) {
        if (places.isEmpty()) {
            Toast.makeText(this, "No places found for " + rawCategoryName, Toast.LENGTH_SHORT).show();
            statusText.setText("No " + rawCategoryName + " found in this area");
            return;
        }

        mMap.clear();
        markersMap.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (PlaceMarker place : places) {
            String snippet = place.getRating() > 0 ?
                    "Rating: " + place.getRating() + " ⭐\n" + place.getVicinity() :
                    place.getVicinity();

            LatLng placeLocation = new LatLng(place.getLatitude(), place.getLongitude());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(placeLocation)
                    .title(place.getName())
                    .snippet(snippet));

            boundsBuilder.include(placeLocation);
            markersMap.put(place.getPlaceId(), marker);
        }

        statusText.setText("Found " + places.size() + " " + rawCategoryName);
        mapTitleText.setText(rawCategoryName + " in " + cityName);

        try {
            if (places.size() == 1) {
                LatLng location = new LatLng(places.get(0).getLatitude(), places.get(0).getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
            } else {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 100;
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting camera bounds", e);
            // Fallback to city center
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cityCoordinates, 13));
        }

        // Only hide status card when not loading more places
        if (!isLoadingMorePlaces) {
            statusCard.postDelayed(() -> {
                statusCard.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction(() -> statusCard.setVisibility(View.GONE))
                        .start();
            }, 3000);
        }
    }

    public class PlaceMarker implements Serializable {
        private String placeId;
        private String name;
        private String vicinity;
        private double latitude;
        private double longitude;
        private float rating;
        private List<String> photoUrls;
        private boolean isSelected;
        private boolean isMandatory;

        public PlaceMarker(String placeId, String name, String vicinity, double latitude, double longitude, float rating, List<String> photoUrls) {
            this.placeId = placeId;
            this.name = name;
            this.vicinity = vicinity;
            this.latitude = latitude;
            this.longitude = longitude;
            this.rating = rating;
            this.photoUrls = photoUrls;
            this.isSelected = false;
            this.isMandatory = false;
        }

        public String getPlaceId() {
            return placeId;
        }

        public String getName() {
            return name;
        }

        public String getVicinity() {
            return vicinity;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public float getRating() {
            return rating;
        }

        public List<String> getPhotoUrls() {
            return photoUrls;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }

        public boolean isMandatory() {
            return isMandatory;
        }

        public void setMandatory(boolean mandatory) {
            isMandatory = mandatory;
        }
    }
}
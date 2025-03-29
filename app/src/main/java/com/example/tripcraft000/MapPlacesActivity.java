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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapPlacesActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng cityCoordinates;
    private String cityName;
    private String categoryName;
    private final String API_KEY = "AIzaSyCYnYiiqrHO0uwKoxNQLA_mKEIuX1aRyL4";
    private TextView mapTitleText;
    private TextView statusText;
    private CardView statusCard;
    private static final Map<String, String> CATEGORY_TO_TYPE_MAP = new HashMap<>();

    static {
        // Initialize mapping from our category names to Google Places API types
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

        // Get data from intent
        cityName = getIntent().getStringExtra("city_name");
        categoryName = getIntent().getStringExtra("category_name");
        double lat = getIntent().getDoubleExtra("city_lat", 0);
        double lng = getIntent().getDoubleExtra("city_lng", 0);
        cityCoordinates = new LatLng(lat, lng);

        // Initialize UI
        setupUI();

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupUI() {
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.mapToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Initialize views
        mapTitleText = findViewById(R.id.mapTitleText);
        statusText = findViewById(R.id.statusText);
        statusCard = findViewById(R.id.statusCard);
        ImageButton backButton = findViewById(R.id.backButton);
        ExtendedFloatingActionButton listFab = findViewById(R.id.listFab);

        // Set initial texts
        mapTitleText.setText(categoryName + " in " + cityName);
        statusText.setText("Loading " + categoryName + "...");

        // Set up click listeners
        backButton.setOnClickListener(v -> finish());

        listFab.setOnClickListener(v -> {
            // TODO: Implement list view functionality
            Toast.makeText(this, "List view coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Move camera to city location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cityCoordinates, 13));

        // Enable map UI controls
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.e("MapPlaces", "Location permission not granted");
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Apply custom map styling
        // Commented out - you would add this if you have a custom style JSON
        /*try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Log.e("MapPlaces", "Style parsing failed");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapPlaces", "Can't find style", e);
        }*/

        // Start fetching places
        fetchPlacesByCategory();
    }

    private void fetchPlacesByCategory() {
        Log.d("MapPlaces", "Fetching places for category: " + categoryName);
        statusText.setText("Searching for " + categoryName + " in " + cityName + "...");

        // Try to get the Google Place type from our mapping
        String placeType = CATEGORY_TO_TYPE_MAP.get(categoryName);

        // If we don't have a mapping, try to extract the type from the category name
        if (placeType == null) {
            if (categoryName.contains(" ")) {
                // Remove emoji if present
                placeType = categoryName.substring(categoryName.indexOf(" ") + 1).toLowerCase();
            } else {
                placeType = categoryName.toLowerCase();
            }
        }

        Log.d("MapPlaces", "Converted to place type: " + placeType);

        // First try a nearby search with type
        String nearbyUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + cityCoordinates.latitude + "," + cityCoordinates.longitude +
                "&radius=5000" +
                "&type=" + placeType +
                "&key=" + API_KEY;

        Log.d("MapPlaces", "Making nearby search request: " + nearbyUrl);
        fetchPlacesFromUrl(nearbyUrl, true);
    }

    private void fallbackToTextSearch() {
        Log.d("MapPlaces", "Falling back to text search for: " + categoryName);
        statusText.setText("Trying alternative search method...");

        try {
            // Format the search query
            String queryText = categoryName;
            if (queryText.contains(" ")) {
                queryText = queryText.substring(queryText.indexOf(" ") + 1);
            }

            String query = URLEncoder.encode(queryText + " in " + cityName, "UTF-8");
            String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                    "?query=" + query +
                    "&key=" + API_KEY;

            Log.d("MapPlaces", "Making text search request: " + url);
            fetchPlacesFromUrl(url, false);
        } catch (Exception e) {
            Log.e("MapPlaces", "Error encoding URL", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Error searching for places", Toast.LENGTH_SHORT).show();
                statusText.setText("Error searching for places");
            });
        }
    }

    private void fetchPlacesFromUrl(final String url, final boolean canFallback) {
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

                final JSONObject jsonResponse = new JSONObject(response.toString());
                final List<PlaceMarker> places = parsePlaceResults(jsonResponse);

                if (places.isEmpty() && canFallback) {
                    // If no results from nearby search, try text search as fallback
                    fallbackToTextSearch();
                } else {
                    runOnUiThread(() -> addMarkersToMap(places));
                }

            } catch (Exception e) {
                Log.e("MapPlaces", "Error fetching places: " + e.getMessage(), e);
                if (canFallback) {
                    fallbackToTextSearch();
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MapPlacesActivity.this,
                                "Error loading places", Toast.LENGTH_SHORT).show();
                        statusText.setText("Error loading places");
                    });
                }
            }
        }).start();
    }

    private List<PlaceMarker> parsePlaceResults(JSONObject jsonResponse) throws JSONException {
        List<PlaceMarker> places = new ArrayList<>();

        if (!jsonResponse.has("results")) {
            Log.d("MapPlaces", "No results found in response");
            return places;
        }

        JSONArray results = jsonResponse.getJSONArray("results");
        Log.d("MapPlaces", "Found " + results.length() + " places");

        for (int i = 0; i < results.length(); i++) {
            JSONObject place = results.getJSONObject(i);

            String name = place.getString("name");

            JSONObject geometry = place.getJSONObject("geometry");
            JSONObject location = geometry.getJSONObject("location");
            double lat = location.getDouble("lat");
            double lng = location.getDouble("lng");

            float rating = place.has("rating") ? (float) place.getDouble("rating") : 0;

            String vicinity = place.has("vicinity") ?
                    place.getString("vicinity") :
                    (place.has("formatted_address") ? place.getString("formatted_address") : "");

            places.add(new PlaceMarker(name, new LatLng(lat, lng), rating, vicinity));
        }

        return places;
    }

    private void addMarkersToMap(List<PlaceMarker> places) {
        if (places.isEmpty()) {
            Toast.makeText(this, "No places found for " + categoryName, Toast.LENGTH_SHORT).show();
            statusText.setText("No " + categoryName + " found in this area");
            return;
        }

        // Build bounds to encapsulate all markers
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (PlaceMarker place : places) {
            String snippet = place.getRating() > 0 ?
                    "Rating: " + place.getRating() + " ⭐\n" + place.getVicinity() :
                    place.getVicinity();

            mMap.addMarker(new MarkerOptions()
                    .position(place.getLocation())
                    .title(place.getName())
                    .snippet(snippet));

            boundsBuilder.include(place.getLocation());
        }

        // Update status with count
        statusText.setText("Found " + places.size() + " " + categoryName);

        // Update title
        mapTitleText.setText(categoryName + " in " + cityName);

        // If we have at least one place, zoom to show all markers or center on first one
        if (places.size() == 1) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(places.get(0).getLocation(), 15));
        } else if (places.size() > 1) {
            // Zoom to show all markers with padding
            try {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 100; // offset from edges of the map in pixels
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                // Fallback in case of exception (like when builder is empty)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(places.get(0).getLocation(), 13));
            }
        }

        // Hide status card after a delay
        statusCard.postDelayed(() -> {
            statusCard.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction(() -> statusCard.setVisibility(View.GONE))
                    .start();
        }, 3000);
    }

    private static class PlaceMarker {
        private final String name;
        private final LatLng location;
        private final float rating;
        private final String vicinity;

        public PlaceMarker(String name, LatLng location, float rating, String vicinity) {
            this.name = name;
            this.location = location;
            this.rating = rating;
            this.vicinity = vicinity;
        }

        public String getName() {
            return name;
        }

        public LatLng getLocation() {
            return location;
        }

        public float getRating() {
            return rating;
        }

        public String getVicinity() {
            return vicinity;
        }
    }
}
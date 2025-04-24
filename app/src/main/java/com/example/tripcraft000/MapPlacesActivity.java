package com.example.tripcraft000;

import android.Manifest;
import android.content.Intent;
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
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapPlacesActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        PlacesAdapter.OnPlaceClickListener,
        PlacesAdapter.OnPlaceSelectionChangedListener {

    private static final String API_KEY = "AIzaSyCYnYiiqrHO0uwKoxNQLA_mKEIuX1aRyL4";

    private GoogleMap googleMap;
    private RecyclerView recyclerView;
    private PlacesAdapter placesAdapter;
    private TextView selectedCountText;
    private Button doneButton;
    private SearchView placeSearchView;

    private String selectedCityName;
    private LatLng selectedCityCoordinates;
    private String selectedCategoryName;
    private List<PlaceMarker> allPlaces = new ArrayList<>();
    private Map<String, Marker> placeMarkers = new HashMap<>();
    private LatLngBounds cityBounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_places);

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

        // Get city information from intent
        selectedCityName = getIntent().getStringExtra("city_name");
        double cityLat = getIntent().getDoubleExtra("city_lat", 0);
        double cityLng = getIntent().getDoubleExtra("city_lng", 0);
        selectedCityCoordinates = new LatLng(cityLat, cityLng);
        selectedCategoryName = getIntent().getStringExtra("category_name");

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

        // Fetch city boundaries
        fetchCityBoundaries(selectedCityName, bounds -> {
            if (bounds != null) {
                cityBounds = bounds; // Store as a class field

                // If we already have places loaded, filter them now
                if (allPlaces != null && !allPlaces.isEmpty()) {
                    List<PlaceMarker> cityPlaces = filterPlacesToCityOnly(allPlaces, cityBounds);
                    updatePlacesDisplay(cityPlaces);
                }

                // Focus map on the city boundaries
                if (googleMap != null) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(cityBounds, 50));
                }
            }

            // Start fetching places
            fetchPlacesForCategory(selectedCityName, selectedCategoryName);
        });
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
                        break;
                    }
                }
            }
        });

        // If we already have city bounds, zoom to them
        if (cityBounds != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(cityBounds, 50));
        }
    }

    private void fetchCityBoundaries(String cityName, final BoundariesCallback callback) {
        String encodedCityName;
        try {
            encodedCityName = URLEncoder.encode(cityName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("MapPlacesActivity", "Error encoding city name", e);
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

                // Return the boundaries on the main thread
                runOnUiThread(() -> callback.onBoundariesFetched(bounds));

            } catch (Exception e) {
                Log.e("MapPlacesActivity", "Error fetching city boundaries", e);
                runOnUiThread(() -> callback.onBoundariesFetched(null));
            }
        }).start();
    }
    private LatLngBounds parseBoundaries(JSONObject jsonResponse) throws JSONException {
        if (jsonResponse.getString("status").equals("OK")) {
            JSONArray results = jsonResponse.getJSONArray("results");
            if (results.length() > 0) {
                JSONObject result = results.getJSONObject(0);

                // Check if there's a viewport in the geometry
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
                            new LatLng(swLat, swLng),  // Southwest corner
                            new LatLng(neLat, neLng)   // Northeast corner
                    );
                }
            }
        }
        return null;
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

        // Build the URL
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                "?query=" + query +
                "&key=" + API_KEY;

        allPlaces.clear();
        fetchPlacesFromUrl(url);
    }

    private void fetchPlacesFromUrl(final String url) {
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

                // Process response on UI thread
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
            List<PlaceMarker> page = convertJsonToPlaceMarkers(response);
            allPlaces.addAll(page);

            List<PlaceMarker> cityPlaces = filterPlacesToCityOnly(allPlaces, cityBounds);
            updatePlacesDisplay(cityPlaces);

            // Update status text with count - show actual count after filtering
            TextView titleText = findViewById(R.id.titleText);
            if (titleText != null && selectedCategoryName != null) {
                titleText.setText(
                        String.format("%s in %s (%d places)",
                                selectedCategoryName,
                                selectedCityName,
                                cityPlaces.size()
                        )
                );
            }
            if (response.has("next_page_token")) {
                final String nextPageToken = response.getString("next_page_token");
                new android.os.Handler().postDelayed(() -> {
                    String nextUrl = "https://maps.googleapis.com/maps/api/place/textsearch/json"
                            + "?pagetoken=" + nextPageToken
                            + "&key=" + API_KEY;
                    fetchPlacesFromUrl(nextUrl);
                    }, 2000);
            }

        } catch (JSONException e) {
            Log.e("MapPlacesActivity", "Error parsing places response", e);
        }
    }

    private void updatePlacesDisplay(List<PlaceMarker> places) {
        // Update RecyclerView
        placesAdapter = new PlacesAdapter(places, this, this);
        recyclerView.setAdapter(placesAdapter);

        // Update map markers
        updateMapMarkers(places);
    }

    private List<PlaceMarker> convertJsonToPlaceMarkers(JSONObject jsonResponse) throws JSONException {
        List<PlaceMarker> places = new ArrayList<>();

        if (jsonResponse.getString("status").equals("OK")) {
            JSONArray results = jsonResponse.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);

                String placeId = place.getString("place_id");
                String name = place.getString("name");

                String vicinity = place.has("vicinity") ? place.getString("vicinity") :
                        (place.has("formatted_address") ? place.getString("formatted_address") : "");

                double rating = place.has("rating") ? place.getDouble("rating") : 0;

                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");

                List<String> photoUrls = new ArrayList<>();
                if (place.has("photos")) {
                    JSONArray photos = place.getJSONArray("photos");
                    for (int j = 0; j < Math.min(photos.length(), 5); j++) {
                        JSONObject photo = photos.getJSONObject(j);
                        String photoReference = photo.getString("photo_reference");
                        String photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                                "?maxwidth=400" +
                                "&photoreference=" + photoReference +
                                "&key=" + API_KEY;
                        photoUrls.add(photoUrl);
                    }
                }

                PlaceMarker placeMarker = new PlaceMarker(placeId, name, vicinity, lat, lng, rating, photoUrls);
                places.add(placeMarker);
            }
        }

        return places;
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
            if (place.isSelected()) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            }

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

    private void updateMarkerIcon(Marker marker, boolean isSelected) {
        if (marker != null) {
            float hue = isSelected ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED;
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(hue));
        }
    }

    private void updateSelectedCount() {
        List<PlaceMarker> selectedPlaces = placesAdapter.getSelectedPlaces();
        String countText = selectedPlaces.size() + " places selected";
        selectedCountText.setText(countText);
    }

    private void finishWithSelectedPlaces() {
        List<PlaceMarker> selectedPlaces = placesAdapter.getSelectedPlaces();

        if (selectedPlaces.isEmpty()) {
            Toast.makeText(this, "Please select at least one place", Toast.LENGTH_SHORT).show();
            return;
        }

        // Here you would typically pass these selected places back to the previous activity
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

    public void onPlaceSelectionChanged(PlaceMarker place, boolean isSelected) {
        // Update marker icon on the map
        Marker marker = placeMarkers.get(place.getPlaceId());
        updateMarkerIcon(marker, isSelected);

        // Update the selected count
        updateSelectedCount();
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
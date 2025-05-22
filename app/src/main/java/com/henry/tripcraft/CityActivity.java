package com.henry.tripcraft;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.henry.tripcraft.models.GeoNamesResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CityActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AutoCompleteTextView searchCity;
    private MaterialButton nextButton;
    private ImageButton backButton;
    private View loadingIndicator;
    private GoogleMap mMap;
    private String geoNamesUsername = "henryhar";
    private ArrayAdapter<String> cityAdapter;
    private boolean isMapReady = false;
    private final HashMap<String, LatLng> cityCoordinates = new HashMap<>();
    private String selectedCityName = "";

    // Minimum population threshold
    private static final int MIN_POPULATION = 1000;

    // Debounce delay for search (milliseconds)
    private static final long SEARCH_DEBOUNCE_DELAY = 300;

    // Cache for previous search results to improve responsiveness
    private final HashMap<String, HashMap<String, LatLng>> searchCache = new HashMap<>();

    // Global set to track all city keys across queries to prevent duplicates
    private final HashSet<String> allCityKeys = new HashSet<>();

    // Global set to track all display names to prevent duplicates in the adapter
    private final HashSet<String> allDisplayNames = new HashSet<>();

    // Use an executor for background tasks
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Keep track of the latest search query
    private String latestQuery = "";

    // Handler for debouncing search input
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Track if a map update is pending
    private boolean pendingMapUpdate = false;
    private LatLng pendingMapLocation = null;
    private String pendingCityName = "";

    // Track the currently running API call
    private Call<GeoNamesResponse> currentApiCall;

    // Retrofit client for API calls
    private GeoNamesAPI geoNamesAPI;

    // NumberFormat for formatting population numbers
    private NumberFormat numberFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city);


        // Initialize the number formatter for population display
        numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

        searchCity = findViewById(R.id.search_city);
        nextButton = findViewById(R.id.next_button);
        loadingIndicator = findViewById(R.id.loading_indicator);
        backButton = findViewById(R.id.back_button);

        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CityActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Initialize Retrofit early
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://secure.geonames.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        geoNamesAPI = retrofit.create(GeoNamesAPI.class);

        // Set up a custom adapter with better styling
        cityAdapter = new CityAdapter(this, android.R.layout.simple_dropdown_item_1line);
        searchCity.setAdapter(cityAdapter);
        searchCity.setThreshold(2);  // Show dropdown after 2 characters
        nextButton.setEnabled(false);

        // Initialize map eagerly
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Apply animations to the search field
        searchCity.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                searchCity.setHint("Type city name...");
                if (!latestQuery.isEmpty() && cityAdapter.getCount() > 0) {
                    searchCity.showDropDown();
                }
            } else {
                searchCity.setHint("Where are you going?");
            }
        });

        searchCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                final String query = charSequence.toString().trim();
                latestQuery = query;

                // Reset the selected city when typing
                if (!query.equals(selectedCityName)) {
                    selectedCityName = "";
                    nextButton.setEnabled(false);
                }

                // Cancel any pending search operation
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Cancel any ongoing API call
                if (currentApiCall != null) {
                    currentApiCall.cancel();
                }

                // Check cache and show immediate results
                if (query.length() >= 2) {
                    checkAndShowFromCache(query);

                    // Show loading indicator
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.VISIBLE);
                    }

                    // Debounce the search to avoid too many API calls
                    searchRunnable = () -> {
                        // First check if we already have exact match in cache
                        if (!searchCache.containsKey(query)) {
                            executorService.execute(() -> fetchCitySuggestions(query));
                        } else {
                            // Just hide loading indicator if we're only using cache
                            runOnUiThread(() -> {
                                if (loadingIndicator != null) {
                                    loadingIndicator.setVisibility(View.GONE);
                                }
                            });
                        }
                    };

                    // Delay the search execution
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
                } else if (loadingIndicator != null) {
                    loadingIndicator.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        searchCity.setOnItemClickListener((parent, view, position, id) -> {
            selectedCityName = (String) parent.getItemAtPosition(position);
            if (cityCoordinates.containsKey(selectedCityName)) {
                updateMap(cityCoordinates.get(selectedCityName), selectedCityName);
                nextButton.setEnabled(true);

                // Hide keyboard after selection
                searchCity.clearFocus();
            }
        });

        nextButton.setOnClickListener(v -> {
            if (!selectedCityName.isEmpty() && cityCoordinates.containsKey(selectedCityName)) {
                // Add a subtle feedback animation
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                        .start();

                LatLng cityLatLng = cityCoordinates.get(selectedCityName);

                // Extract the basic city and country name from the formatted display string
                String cityCountryOnly = extractCityCountryName(selectedCityName);

                Intent intent = new Intent(CityActivity.this, CalendarActivity.class);
                intent.putExtra("city", cityCountryOnly);
                intent.putExtra("latitude", cityLatLng.latitude);
                intent.putExtra("longitude", cityLatLng.longitude);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                Toast.makeText(this, "Please select a city before proceeding.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Check several cache variants to find potential matches
     */
    private void checkAndShowFromCache(String query) {
        // Early return for very short queries
        if (query.length() < 2) {
            return;
        }

        // Check for precise cache hits
        if (searchCache.containsKey(query)) {
            updateAdapterFromCache(query, searchCache.get(query));
            return;
        }

        // Check for prefix-based cache hits (e.g., "Rom" would match "Rome" entries)
        for (String cacheKey : searchCache.keySet()) {
            if (cacheKey.toLowerCase().startsWith(query.toLowerCase()) ||
                    query.toLowerCase().startsWith(cacheKey.toLowerCase())) {
                // Found a prefix match in cache
                HashMap<String, LatLng> cachedData = searchCache.get(cacheKey);
                updateAdapterFromCache(query, cachedData);
                break;
            }
        }
    }

    /**
     * Extracts the city and country name from the formatted display string with population
     * Input format: "City, Country (Population: X,XXX,XXX)"
     * Output format: "City, Country"
     */
    private String extractCityCountryName(String formattedCityName) {
        int populationIndex = formattedCityName.indexOf(" (Population:");
        if (populationIndex > 0) {
            return formattedCityName.substring(0, populationIndex);
        }
        return formattedCityName;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Apply custom map style for a more modern look
        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                // Fallback to default style
            }
        } catch (Exception e) {
            // Error loading style
        }

        isMapReady = true;

        // Check if we have a pending map update
        if (pendingMapUpdate) {
            updateMap(pendingMapLocation, pendingCityName);
            pendingMapUpdate = false;
        }
    }

    private void updateAdapterFromCache(String query, HashMap<String, LatLng> cachedData) {
        if (cachedData == null || cachedData.isEmpty()) {
            return;
        }

        runOnUiThread(() -> {
            // Only update if this is still the latest query or related to it
            if (latestQuery.equals(query) || latestQuery.toLowerCase().contains(query.toLowerCase()) ||
                    query.toLowerCase().contains(latestQuery.toLowerCase())) {

                // Preserve the city coordinates
                cityCoordinates.putAll(cachedData);

                // Initialize the adapter if this is the first time
                if (cityAdapter.getCount() == 0) {
                    for (String cityName : cachedData.keySet()) {
                        // Add only if we haven't already added this display name
                        if (!allDisplayNames.contains(cityName)) {
                            allDisplayNames.add(cityName);
                            cityAdapter.add(cityName);
                        }
                    }
                } else {
                    // Don't add cities that are already in the adapter
                    boolean addedNewItems = false;
                    for (String cityName : cachedData.keySet()) {
                        if (!allDisplayNames.contains(cityName)) {
                            allDisplayNames.add(cityName);
                            cityAdapter.add(cityName);
                            addedNewItems = true;
                        }
                    }

                    // Only notify if we added anything new
                    if (addedNewItems) {
                        cityAdapter.notifyDataSetChanged();
                    }
                }

                // Force the dropdown to show
                if (searchCity.hasFocus() && cachedData.size() > 0) {
                    searchCity.showDropDown();
                }

                // Hide loading indicator
                if (loadingIndicator != null) {
                    loadingIndicator.setVisibility(View.GONE);
                }
            }
        });
    }

    private void fetchCitySuggestions(String query) {
        if (query.isEmpty()) return;

        // Store the query we're processing
        final String currentQuery = query;

        // Only search for cities and towns (exclude airports, villages, etc.)
        currentApiCall = geoNamesAPI.searchCity(
                query,
                "PPLC,PPLA,PPLA2,PPLA3,PPLA4", // Major cities and admin divisions
                20, // Fetch more to have options after filtering
                geoNamesUsername
        );

        currentApiCall.enqueue(new Callback<GeoNamesResponse>() {
            @Override
            public void onResponse(Call<GeoNamesResponse> call, Response<GeoNamesResponse> response) {
                if (response.isSuccessful() && response.body() != null && !call.isCanceled()) {
                    // Process on a background thread
                    executorService.execute(() -> {
                        HashMap<String, LatLng> newCityData = new HashMap<>();

                        if (response.body().geonames != null) {
                            for (GeoNamesResponse.City city : response.body().geonames) {
                                // Skip if essential data is missing or population is below threshold
                                if (city.name == null
                                        || city.lat == 0
                                        || city.lng == 0
                                        || city.population < MIN_POPULATION) {
                                    continue;
                                }

                                String country = city.countryName != null ? city.countryName : "";

                                // Create a truly unique key that combines all city attributes
                                String cityKey = city.name + "," + country + "," + city.lat + "," + city.lng;

                                // Format the display name
                                String formattedPopulation = city.population > 0
                                        ? " (Population: " + numberFormat.format(city.population) + ")"
                                        : "";
                                String cityName = city.name + ", " + country + formattedPopulation;

                                // Only add if we haven't seen this exact city before
                                if (!allCityKeys.contains(cityKey)) {
                                    allCityKeys.add(cityKey);

                                    // Check if we already have this display name
                                    if (!allDisplayNames.contains(cityName)) {
                                        newCityData.put(cityName, new LatLng(city.lat, city.lng));
                                    }
                                }
                            }
                        }

                        // Cache the results (only if we got some)
                        if (!newCityData.isEmpty()) {
                            searchCache.put(currentQuery, newCityData);
                            // also cache lowercase for improved matching
                            if (!currentQuery.equals(currentQuery.toLowerCase())) {
                                searchCache.put(currentQuery.toLowerCase(), newCityData);
                            }
                        }

                        // Only update UI if this is still relevant to latest query
                        if (latestQuery.equals(currentQuery)
                                || latestQuery.toLowerCase().startsWith(currentQuery.toLowerCase())
                                || currentQuery.toLowerCase().startsWith(latestQuery.toLowerCase())) {
                            updateAdapterFromCache(currentQuery, newCityData);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        if (loadingIndicator != null) {
                            loadingIndicator.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<GeoNamesResponse> call, Throwable t) {
                if (!call.isCanceled()) {
                    runOnUiThread(() -> {
                        if (loadingIndicator != null) {
                            loadingIndicator.setVisibility(View.GONE);
                        }
                        Toast.makeText(CityActivity.this,
                                "Network error. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateMap(LatLng cityLocation, String cityName) {
        if (!isMapReady) {
            // Store this update for when the map is ready
            pendingMapUpdate = true;
            pendingMapLocation = cityLocation;
            pendingCityName = cityName;
            return;
        }

        runOnUiThread(() -> {
            mMap.clear();

            // Extract basic city name without population for the marker
            String markerTitle = extractCityCountryName(cityName);

            mMap.addMarker(new MarkerOptions().position(cityLocation).title(markerTitle));
            // Use animate camera for smoother transitions
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cityLocation, 10));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any pending operations
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        if (currentApiCall != null) {
            currentApiCall.cancel();
        }
        // Shutdown the executor service
        executorService.shutdownNow();
    }
}
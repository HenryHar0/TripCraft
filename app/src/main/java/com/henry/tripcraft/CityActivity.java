package com.henry.tripcraft;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CityActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "CityActivity";

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

    // Reduced debounce delay for better responsiveness
    private static final long SEARCH_DEBOUNCE_DELAY = 300; // Reduced from 500

    // Cache for previous search results to improve responsiveness
    private final HashMap<String, HashMap<String, LatLng>> searchCache = new HashMap<>();

    // Improved duplicate tracking - use normalized city+country as key
    private final HashMap<String, String> cityCountryToDisplayName = new HashMap<>();
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

    // Retrofit client for API calls with improved networking
    private GeoNamesAPI geoNamesAPI;

    // NumberFormat for formatting population numbers
    private NumberFormat numberFormat;

    // Reduced retry mechanism for faster failure detection
    private static final int MAX_RETRIES = 2; // Reduced from 3
    private int currentRetryCount = 0;

    // Track request timing for debugging
    private long requestStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city);

        Log.d(TAG, "onCreate: Starting CityActivity");

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

        // Initialize Retrofit with improved networking configuration
        initializeRetrofit();

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

        setupSearchListeners();
        Log.d(TAG, "onCreate: Setup complete");
    }

    private void initializeRetrofit() {
        Log.d(TAG, "initializeRetrofit: Setting up network client");

        // Add logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message ->
                Log.d(TAG + "_HTTP", message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC); // Change to BODY for full debugging

        // Create OkHttpClient with optimized settings
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)     // Reduced from 15s
                .readTimeout(15, TimeUnit.SECONDS)        // Reduced from 20s
                .writeTimeout(15, TimeUnit.SECONDS)       // Reduced from 20s
                .retryOnConnectionFailure(true)
                .addInterceptor(loggingInterceptor)       // Add logging
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://secure.geonames.org/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        geoNamesAPI = retrofit.create(GeoNamesAPI.class);
        Log.d(TAG, "initializeRetrofit: Network client ready");
    }

    private void setupSearchListeners() {
        Log.d(TAG, "setupSearchListeners: Setting up UI listeners");

        // Apply animations to the search field
        searchCity.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "searchCity focus changed: " + hasFocus);
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

                Log.d(TAG, "onTextChanged: Query = '" + query + "', length = " + query.length());

                // Reset the selected city when typing
                if (!query.equals(selectedCityName)) {
                    selectedCityName = "";
                    nextButton.setEnabled(false);
                }

                // Cancel any pending search operation
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                    Log.d(TAG, "onTextChanged: Cancelled pending search");
                }

                // Cancel any ongoing API call
                if (currentApiCall != null && !currentApiCall.isCanceled()) {
                    currentApiCall.cancel();
                    Log.d(TAG, "onTextChanged: Cancelled ongoing API call");
                }

                // Reset retry count for new query
                currentRetryCount = 0;

                // Check cache and show immediate results
                if (query.length() >= 2) {
                    Log.d(TAG, "onTextChanged: Checking cache for query: " + query);
                    checkAndShowFromCache(query);

                    // Show loading indicator
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.VISIBLE);
                    }

                    // Debounce the search to avoid too many API calls
                    searchRunnable = () -> {
                        Log.d(TAG, "searchRunnable: Executing search for: " + query);
                        // First check if we already have exact match in cache
                        if (!searchCache.containsKey(query)) {
                            Log.d(TAG, "searchRunnable: No cache hit, making API call");
                            executorService.execute(() -> fetchCitySuggestions(query));
                        } else {
                            Log.d(TAG, "searchRunnable: Using cached results only");
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
                    Log.d(TAG, "onTextChanged: Scheduled search with " + SEARCH_DEBOUNCE_DELAY + "ms delay");
                } else {
                    Log.d(TAG, "onTextChanged: Query too short, hiding loading indicator");
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        searchCity.setOnItemClickListener((parent, view, position, id) -> {
            selectedCityName = (String) parent.getItemAtPosition(position);
            Log.d(TAG, "onItemClick: Selected city = " + selectedCityName);

            if (cityCoordinates.containsKey(selectedCityName)) {
                updateMap(cityCoordinates.get(selectedCityName), selectedCityName);
                nextButton.setEnabled(true);

                // Hide keyboard after selection
                searchCity.clearFocus();
            }
        });

        nextButton.setOnClickListener(v -> {
            Log.d(TAG, "nextButton clicked: selectedCityName = " + selectedCityName);

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
                Log.w(TAG, "nextButton clicked but no valid city selected");
                Toast.makeText(this, "Please select a city before proceeding.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Check several cache variants to find potential matches
     */
    private void checkAndShowFromCache(String query) {
        Log.d(TAG, "checkAndShowFromCache: Checking cache for query = " + query);

        // Early return for very short queries
        if (query.length() < 2) {
            Log.d(TAG, "checkAndShowFromCache: Query too short, returning");
            return;
        }

        // Check for precise cache hits
        if (searchCache.containsKey(query)) {
            Log.d(TAG, "checkAndShowFromCache: Found exact cache hit for: " + query);
            updateAdapterFromCache(query, searchCache.get(query), true);
            return;
        }

        // Check for prefix-based cache hits (e.g., "Rom" would match "Rome" entries)
        for (String cacheKey : searchCache.keySet()) {
            if (cacheKey.toLowerCase().startsWith(query.toLowerCase()) ||
                    query.toLowerCase().startsWith(cacheKey.toLowerCase())) {
                Log.d(TAG, "checkAndShowFromCache: Found prefix cache hit - cacheKey: " + cacheKey + ", query: " + query);
                // Found a prefix match in cache
                HashMap<String, LatLng> cachedData = searchCache.get(cacheKey);
                updateAdapterFromCache(query, cachedData, true);
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

    /**
     * Creates a normalized key for duplicate detection
     * @param cityName The city name
     * @param countryName The country name
     * @return A normalized key like "london,united kingdom"
     */
    private String createNormalizedKey(String cityName, String countryName) {
        return (cityName + "," + countryName).toLowerCase().trim();
    }

    /**
     * Check for duplicates before showing dropdown
     */
    private void checkForDuplicatesAndShowDropdown() {
        Log.d(TAG, "checkForDuplicatesAndShowDropdown: Checking " + cityAdapter.getCount() + " items");

        // Create a temporary map to check for duplicates
        HashMap<String, Integer> duplicateCount = new HashMap<>();

        // Count occurrences of each city-country combination
        for (int i = 0; i < cityAdapter.getCount(); i++) {
            String displayName = cityAdapter.getItem(i);
            String cityCountry = extractCityCountryName(displayName);
            String normalizedKey = createNormalizedKey(
                    cityCountry.split(",")[0].trim(),
                    cityCountry.contains(",") ? cityCountry.split(",")[1].trim() : ""
            );

            duplicateCount.put(normalizedKey, duplicateCount.getOrDefault(normalizedKey, 0) + 1);
        }

        // Log duplicates found (for debugging)
        for (String key : duplicateCount.keySet()) {
            if (duplicateCount.get(key) > 1) {
                Log.w(TAG, "Duplicate found: " + key + " appears " + duplicateCount.get(key) + " times");
            }
        }

        // Show dropdown if we have cities and search field has focus
        if (searchCity.hasFocus() && cityAdapter.getCount() > 0) {
            searchCity.showDropDown();
            Log.d(TAG, "checkForDuplicatesAndShowDropdown: Showing dropdown with " + cityAdapter.getCount() + " items");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Apply custom map style for a more modern look
        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            Log.d(TAG, "onMapReady: Map style applied successfully: " + success);
        } catch (Exception e) {
            Log.e(TAG, "onMapReady: Error applying map style", e);
        }

        isMapReady = true;

        // Check if we have a pending map update
        if (pendingMapUpdate) {
            Log.d(TAG, "onMapReady: Processing pending map update");
            updateMap(pendingMapLocation, pendingCityName);
            pendingMapUpdate = false;
        }
    }

    private void updateAdapterFromCache(String query, HashMap<String, LatLng> cachedData, boolean checkDuplicates) {
        if (cachedData == null || cachedData.isEmpty()) {
            Log.d(TAG, "updateAdapterFromCache: No cached data available");
            return;
        }

        Log.d(TAG, "updateAdapterFromCache: Updating adapter with " + cachedData.size() + " cached items");

        runOnUiThread(() -> {
            // Only update if this is still the latest query or related to it
            if (latestQuery.equals(query) || latestQuery.toLowerCase().contains(query.toLowerCase()) ||
                    query.toLowerCase().contains(latestQuery.toLowerCase())) {

                // Preserve the city coordinates
                cityCoordinates.putAll(cachedData);

                boolean addedNewItems = false;

                for (String cityName : cachedData.keySet()) {
                    // Extract city and country for duplicate checking
                    String cityCountry = extractCityCountryName(cityName);
                    String[] parts = cityCountry.split(",");
                    String city = parts.length > 0 ? parts[0].trim() : "";
                    String country = parts.length > 1 ? parts[1].trim() : "";
                    String normalizedKey = createNormalizedKey(city, country);

                    // Check for duplicates if requested
                    if (checkDuplicates) {
                        // If we already have this city-country combination, skip it
                        if (cityCountryToDisplayName.containsKey(normalizedKey)) {
                            // Keep the one with higher population if possible
                            String existingDisplayName = cityCountryToDisplayName.get(normalizedKey);
                            if (shouldReplaceExistingCity(existingDisplayName, cityName)) {
                                // Remove the old one
                                allDisplayNames.remove(existingDisplayName);
                                cityCoordinates.remove(existingDisplayName);

                                // Remove from adapter
                                for (int i = 0; i < cityAdapter.getCount(); i++) {
                                    if (cityAdapter.getItem(i).equals(existingDisplayName)) {
                                        cityAdapter.remove(existingDisplayName);
                                        break;
                                    }
                                }

                                // Add the new one
                                cityCountryToDisplayName.put(normalizedKey, cityName);
                                allDisplayNames.add(cityName);
                                cityAdapter.add(cityName);
                                addedNewItems = true;
                            }
                            continue; // Skip adding if we're keeping the existing one
                        }
                    }

                    // Add only if we haven't already added this display name
                    if (!allDisplayNames.contains(cityName)) {
                        allDisplayNames.add(cityName);
                        cityCountryToDisplayName.put(normalizedKey, cityName);
                        cityAdapter.add(cityName);
                        addedNewItems = true;
                    }
                }

                // Only notify if we added anything new
                if (addedNewItems) {
                    cityAdapter.notifyDataSetChanged();
                    Log.d(TAG, "updateAdapterFromCache: Added new items, adapter now has " + cityAdapter.getCount() + " items");
                }

                // Check for duplicates before showing dropdown
                if (checkDuplicates) {
                    checkForDuplicatesAndShowDropdown();
                } else {
                    // Force the dropdown to show
                    if (searchCity.hasFocus() && cachedData.size() > 0) {
                        searchCity.showDropDown();
                    }
                }

                // Hide loading indicator
                if (loadingIndicator != null) {
                    loadingIndicator.setVisibility(View.GONE);
                }
            } else {
                Log.d(TAG, "updateAdapterFromCache: Skipping update - query no longer relevant");
            }
        });
    }

    /**
     * Determines if we should replace an existing city with a new one
     * (e.g., prefer higher population)
     */
    private boolean shouldReplaceExistingCity(String existingDisplayName, String newDisplayName) {
        // Extract population from both display names
        long existingPop = extractPopulationFromDisplayName(existingDisplayName);
        long newPop = extractPopulationFromDisplayName(newDisplayName);

        // Prefer the one with higher population
        return newPop > existingPop;
    }

    /**
     * Extracts population number from display name
     */
    private long extractPopulationFromDisplayName(String displayName) {
        try {
            int startIndex = displayName.indexOf("Population: ");
            if (startIndex == -1) return 0;

            startIndex += "Population: ".length();
            int endIndex = displayName.indexOf(")", startIndex);
            if (endIndex == -1) return 0;

            String popStr = displayName.substring(startIndex, endIndex);
            // Remove commas from population string
            popStr = popStr.replaceAll(",", "");
            return Long.parseLong(popStr);
        } catch (Exception e) {
            return 0;
        }
    }

    private void fetchCitySuggestions(String query) {
        Log.d(TAG, "fetchCitySuggestions: Starting search for: " + query);
        requestStartTime = System.currentTimeMillis();
        fetchCitySuggestionsWithRetry(query, 0);
    }

    private void fetchCitySuggestionsWithRetry(String query, int retryCount) {
        if (query.isEmpty()) {
            Log.d(TAG, "fetchCitySuggestionsWithRetry: Empty query, returning");
            return;
        }

        Log.d(TAG, "fetchCitySuggestionsWithRetry: Query = " + query + ", retry = " + retryCount);

        // Store the query we're processing
        final String currentQuery = query;

        // Only search for cities and towns (exclude airports, villages, etc.)
        currentApiCall = geoNamesAPI.searchCity(
                query,
                "PPLC,PPLA,PPLA2,PPLA3,PPLA4", // Major cities and admin divisions
                20, // Fetch more to have options after filtering
                geoNamesUsername
        );

        Log.d(TAG, "fetchCitySuggestionsWithRetry: API call created, making request...");

        currentApiCall.enqueue(new Callback<GeoNamesResponse>() {
            @Override
            public void onResponse(Call<GeoNamesResponse> call, Response<GeoNamesResponse> response) {
                long responseTime = System.currentTimeMillis() - requestStartTime;
                Log.d(TAG, "onResponse: Received response in " + responseTime + "ms, code = " + response.code());

                if (response.isSuccessful() && response.body() != null && !call.isCanceled()) {
                    Log.d(TAG, "onResponse: Successful response, processing data...");

                    // Reset retry count on success
                    currentRetryCount = 0;

                    // Process on a background thread
                    executorService.execute(() -> {
                        HashMap<String, LatLng> newCityData = new HashMap<>();

                        if (response.body().geonames != null) {
                            Log.d(TAG, "onResponse: Processing " + response.body().geonames.size() + " cities from API");

                            for (GeoNamesResponse.City city : response.body().geonames) {
                                // Skip if essential data is missing or population is below threshold
                                if (city.name == null
                                        || city.lat == 0
                                        || city.lng == 0
                                        || city.population < MIN_POPULATION) {
                                    Log.d(TAG, "onResponse: Skipping city " + (city.name != null ? city.name : "null") +
                                            " - population: " + city.population);
                                    continue;
                                }

                                String country = city.countryName != null ? city.countryName : "";

                                // Format the display name
                                String formattedPopulation = city.population > 0
                                        ? " (Population: " + numberFormat.format(city.population) + ")"
                                        : "";
                                String cityName = city.name + ", " + country + formattedPopulation;

                                // Create normalized key for duplicate checking
                                String normalizedKey = createNormalizedKey(city.name, country);

                                // Only add if we don't already have this city-country combination
                                if (!cityCountryToDisplayName.containsKey(normalizedKey)) {
                                    newCityData.put(cityName, new LatLng(city.lat, city.lng));
                                }
                            }
                        } else {
                            Log.w(TAG, "onResponse: No geonames data in response");
                        }

                        Log.d(TAG, "onResponse: Processed " + newCityData.size() + " unique cities");

                        // Cache the results (only if we got some)
                        if (!newCityData.isEmpty()) {
                            searchCache.put(currentQuery, newCityData);
                            // also cache lowercase for improved matching
                            if (!currentQuery.equals(currentQuery.toLowerCase())) {
                                searchCache.put(currentQuery.toLowerCase(), newCityData);
                            }
                            Log.d(TAG, "onResponse: Cached results for query: " + currentQuery);
                        }

                        // Only update UI if this is still relevant to latest query
                        if (latestQuery.equals(currentQuery)
                                || latestQuery.toLowerCase().startsWith(currentQuery.toLowerCase())
                                || currentQuery.toLowerCase().startsWith(latestQuery.toLowerCase())) {
                            Log.d(TAG, "onResponse: Updating UI with new data");
                            updateAdapterFromCache(currentQuery, newCityData, true);
                        } else {
                            Log.d(TAG, "onResponse: Skipping UI update - query no longer relevant");
                        }
                    });
                } else {
                    Log.w(TAG, "onResponse: Unsuccessful response - code: " + response.code() +
                            ", body: " + (response.body() != null) + ", cancelled: " + call.isCanceled());
                    // Handle unsuccessful response
                    handleApiError(currentQuery, retryCount, "Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GeoNamesResponse> call, Throwable t) {
                long responseTime = System.currentTimeMillis() - requestStartTime;
                Log.e(TAG, "onFailure: Request failed after " + responseTime + "ms", t);

                if (!call.isCanceled()) {
                    handleApiError(currentQuery, retryCount, "Network error: " + t.getMessage());
                } else {
                    Log.d(TAG, "onFailure: Call was cancelled, not handling as error");
                }
            }
        });
    }

    private void handleApiError(String query, int retryCount, String errorMessage) {
        Log.w(TAG, "handleApiError: Query = " + query + ", attempt = " + (retryCount + 1) +
                ", error = " + errorMessage);

        if (retryCount < MAX_RETRIES) {
            // Exponential backoff with jitter to avoid thundering herd
            long baseDelay = (long) Math.pow(2, retryCount) * 1000; // 1s, 2s
            long jitter = (long) (Math.random() * 500); // Add up to 500ms random delay
            long delayMs = baseDelay + jitter;

            Log.d(TAG, "handleApiError: Scheduling retry in " + delayMs + "ms");

            searchHandler.postDelayed(() -> {
                if (latestQuery.equals(query)) { // Only retry if still relevant
                    Log.d(TAG, "handleApiError: Executing retry for: " + query);
                    runOnUiThread(() -> {
                        if (loadingIndicator != null) {
                            loadingIndicator.setVisibility(View.VISIBLE);
                        }
                    });
                    fetchCitySuggestionsWithRetry(query, retryCount + 1);
                } else {
                    Log.d(TAG, "handleApiError: Skipping retry - query no longer relevant");
                }
            }, delayMs);

            // Show different messages based on retry attempt
            runOnUiThread(() -> {
                String message;
                switch (retryCount) {
                    case 0:
                        message = "Connection slow, retrying...";
                        break;
                    case 1:
                        message = "Still connecting, please wait...";
                        break;
                    default:
                        message = "Having trouble connecting, final attempt...";
                        break;
                }

                // Only show toast if we don't have cached results to fall back on
                if (!hasRelevantCachedResults(query)) {
                    Toast.makeText(CityActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.w(TAG, "handleApiError: Max retries reached for query: " + query);
            // Max retries reached - check if we can use cached results
            runOnUiThread(() -> {
                if (loadingIndicator != null) {
                    loadingIndicator.setVisibility(View.GONE);
                }

                // Try to show cached results from similar queries
                if (tryFallbackToCache(query)) {
                    Log.d(TAG, "handleApiError: Using fallback cache results");
                    Toast.makeText(CityActivity.this,
                            "Using cached results (connection unstable)",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Show retry option
                    showRetryDialog(query);
                }
            });
        }
    }

    private boolean hasRelevantCachedResults(String query) {
        if (query.length() < 2) return false;

        for (String cacheKey : searchCache.keySet()) {
            if (cacheKey.toLowerCase().startsWith(query.toLowerCase()) ||
                    query.toLowerCase().startsWith(cacheKey.toLowerCase())) {
                HashMap<String, LatLng> cached = searchCache.get(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Try to use cached results as fallback
     */
    private boolean tryFallbackToCache(String query) {
        // Look for partial matches in cache
        for (String cacheKey : searchCache.keySet()) {
            if (cacheKey.toLowerCase().contains(query.toLowerCase()) ||
                    query.toLowerCase().contains(cacheKey.toLowerCase())) {
                HashMap<String, LatLng> cached = searchCache.get(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    updateAdapterFromCache(query, cached, true);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Show dialog with retry option
     */
    private void showRetryDialog(String query) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Connection Problem")
                .setMessage("Unable to load cities. This might be due to network issues or server maintenance.")
                .setPositiveButton("Retry", (dialog, which) -> {
                    currentRetryCount = 0; // Reset retry count
                    fetchCitySuggestions(query);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Try fallback cache one more time
                    tryFallbackToCache(query);
                })
                .show();
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
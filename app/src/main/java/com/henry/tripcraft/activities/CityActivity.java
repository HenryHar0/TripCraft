package com.henry.tripcraft.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.henry.tripcraft.R;
import com.henry.tripcraft.adapters.CityAutoCompleteAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CityActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "CityActivity";
    private static final String GEODB_API_URL = "https://wft-geo-db.p.rapidapi.com/v1/geo/cities";
    private static final String RAPIDAPI_KEY = "b0b96f2159msh6a3d44fc56ef3c8p10d0c3jsn4368e4ace4b1";
    private static final String RAPIDAPI_HOST = "wft-geo-db.p.rapidapi.com";
    private static final int SEARCH_DELAY_MS = 300;

    // UI Components
    private AppCompatAutoCompleteTextView searchCity;
    private MaterialButton nextButton;
    private ImageButton backButton;
    private ProgressBar loadingIndicator;
    private GoogleMap googleMap;

    // Data
    private CityAutoCompleteAdapter autoCompleteAdapter;
    private List<City> cityList;
    private City selectedCity;
    private Marker currentMarker;
    private Handler searchHandler;
    private Runnable searchRunnable;
    private OkHttpClient httpClient;
    private ExecutorService executorService;

    // Country code mapping for better flag support
    private static final Map<String, String> COUNTRY_CODE_MAP = new HashMap<>();
    static {
        // Add common country mappings (ISO 3166-1 alpha-2 codes)
        COUNTRY_CODE_MAP.put("United States", "US");
        COUNTRY_CODE_MAP.put("United Kingdom", "GB");
        COUNTRY_CODE_MAP.put("Canada", "CA");
        COUNTRY_CODE_MAP.put("Australia", "AU");
        COUNTRY_CODE_MAP.put("Germany", "DE");
        COUNTRY_CODE_MAP.put("France", "FR");
        COUNTRY_CODE_MAP.put("Spain", "ES");
        COUNTRY_CODE_MAP.put("Italy", "IT");
        COUNTRY_CODE_MAP.put("Japan", "JP");
        COUNTRY_CODE_MAP.put("China", "CN");
        COUNTRY_CODE_MAP.put("India", "IN");
        COUNTRY_CODE_MAP.put("Brazil", "BR");
        COUNTRY_CODE_MAP.put("Russia", "RU");
        COUNTRY_CODE_MAP.put("Mexico", "MX");
        COUNTRY_CODE_MAP.put("Netherlands", "NL");
        COUNTRY_CODE_MAP.put("Belgium", "BE");
        COUNTRY_CODE_MAP.put("Switzerland", "CH");
        COUNTRY_CODE_MAP.put("Austria", "AT");
        COUNTRY_CODE_MAP.put("Sweden", "SE");
        COUNTRY_CODE_MAP.put("Norway", "NO");
        COUNTRY_CODE_MAP.put("Denmark", "DK");
        COUNTRY_CODE_MAP.put("Finland", "FI");
        COUNTRY_CODE_MAP.put("Poland", "PL");
        COUNTRY_CODE_MAP.put("Czech Republic", "CZ");
        COUNTRY_CODE_MAP.put("Hungary", "HU");
        COUNTRY_CODE_MAP.put("Portugal", "PT");
        COUNTRY_CODE_MAP.put("Greece", "GR");
        COUNTRY_CODE_MAP.put("Turkey", "TR");
        COUNTRY_CODE_MAP.put("South Korea", "KR");
        COUNTRY_CODE_MAP.put("Thailand", "TH");
        COUNTRY_CODE_MAP.put("Singapore", "SG");
        COUNTRY_CODE_MAP.put("Malaysia", "MY");
        COUNTRY_CODE_MAP.put("Indonesia", "ID");
        COUNTRY_CODE_MAP.put("Philippines", "PH");
        COUNTRY_CODE_MAP.put("Vietnam", "VN");
        COUNTRY_CODE_MAP.put("South Africa", "ZA");
        COUNTRY_CODE_MAP.put("Egypt", "EG");
        COUNTRY_CODE_MAP.put("Argentina", "AR");
        COUNTRY_CODE_MAP.put("Chile", "CL");
        COUNTRY_CODE_MAP.put("Colombia", "CO");
        COUNTRY_CODE_MAP.put("Peru", "PE");
        COUNTRY_CODE_MAP.put("Venezuela", "VE");
        COUNTRY_CODE_MAP.put("Israel", "IL");
        COUNTRY_CODE_MAP.put("Saudi Arabia", "SA");
        COUNTRY_CODE_MAP.put("United Arab Emirates", "AE");
        COUNTRY_CODE_MAP.put("New Zealand", "NZ");
        COUNTRY_CODE_MAP.put("Ireland", "IE");
        COUNTRY_CODE_MAP.put("Iceland", "IS");
        COUNTRY_CODE_MAP.put("Luxembourg", "LU");
        COUNTRY_CODE_MAP.put("Monaco", "MC");
        COUNTRY_CODE_MAP.put("San Marino", "SM");
        COUNTRY_CODE_MAP.put("Vatican City", "VA");
        COUNTRY_CODE_MAP.put("Armenia", "AM");
        COUNTRY_CODE_MAP.put("Georgia", "GE");
        COUNTRY_CODE_MAP.put("Azerbaijan", "AZ");
        COUNTRY_CODE_MAP.put("Kazakhstan", "KZ");
        COUNTRY_CODE_MAP.put("Uzbekistan", "UZ");
        COUNTRY_CODE_MAP.put("Ukraine", "UA");
        COUNTRY_CODE_MAP.put("Belarus", "BY");
        COUNTRY_CODE_MAP.put("Moldova", "MD");
        COUNTRY_CODE_MAP.put("Romania", "RO");
        COUNTRY_CODE_MAP.put("Bulgaria", "BG");
        COUNTRY_CODE_MAP.put("Serbia", "RS");
        COUNTRY_CODE_MAP.put("Croatia", "HR");
        COUNTRY_CODE_MAP.put("Slovenia", "SI");
        COUNTRY_CODE_MAP.put("Bosnia and Herzegovina", "BA");
        COUNTRY_CODE_MAP.put("Montenegro", "ME");
        COUNTRY_CODE_MAP.put("North Macedonia", "MK");
        COUNTRY_CODE_MAP.put("Albania", "AL");
        COUNTRY_CODE_MAP.put("Kosovo", "XK");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city);

        initializeComponents();
        setupEventListeners();
        setupMap();
        setupAutoComplete();
    }

    private void initializeComponents() {
        searchCity = findViewById(R.id.search_city);
        nextButton = findViewById(R.id.next_button);
        backButton = findViewById(R.id.back_button);
        loadingIndicator = findViewById(R.id.loading_indicator);

        cityList = new ArrayList<>();
        searchHandler = new Handler(Looper.getMainLooper());
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        executorService = Executors.newSingleThreadExecutor();

        // Initially disable continue button
        nextButton.setEnabled(false);
        nextButton.setAlpha(0.5f);
    }

    private void setupEventListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        nextButton.setOnClickListener(v -> {
            if (selectedCity != null) {
                navigateToCalendarActivity();
            } else {
                Toast.makeText(this, "Please select a city first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupAutoComplete() {
        autoCompleteAdapter = new CityAutoCompleteAdapter(this, cityList);
        searchCity.setAdapter(autoCompleteAdapter);
        searchCity.setThreshold(2);

        searchCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString().trim();
                if (query.length() >= 2) {
                    // Check for custom Rome handling
                    if (isRomeQuery(query)) {
                        handleRomeQuery();
                    } else {
                        searchRunnable = () -> searchCities(query);
                        searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                        showLoading(true);
                    }
                } else {
                    cityList.clear();
                    autoCompleteAdapter.notifyDataSetChanged();
                    showLoading(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchCity.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < cityList.size()) {
                    selectedCity = cityList.get(position);
                    updateMapWithSelectedCity();
                    enableContinueButton();
                    searchCity.clearFocus();
                }
            }
        });

        // Handle manual selection clearing
        searchCity.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && selectedCity != null) {
                // Clear selection if user starts typing again
                String currentText = searchCity.getText().toString();
                String expectedText = selectedCity.getName() + ", " + selectedCity.getCountry();
                if (!currentText.equals(expectedText)) {
                    clearSelection();
                }
            }
        });
    }

    private boolean isRomeQuery(String query) {
        return query.toLowerCase().equals("rome") ||
                query.toLowerCase().equals("rom") ||
                query.toLowerCase().startsWith("rome");
    }

    private void handleRomeQuery() {
        // Create custom Rome city object
        City romeCity = new City(
                "Rome",
                "Italy",
                "Lazio",
                41.9028,
                12.4964,
                2873000,
                "IT"
        );

        runOnUiThread(() -> {
            cityList.clear();
            cityList.add(romeCity);
            autoCompleteAdapter.notifyDataSetChanged();
            showLoading(false);
            searchCity.showDropDown();
        });
    }

    private void searchCities(String query) {
        String url = GEODB_API_URL + "?namePrefix=" + query +
                "&limit=10&offset=0&sort=-population&types=CITY";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-RapidAPI-Key", RAPIDAPI_KEY)
                .addHeader("X-RapidAPI-Host", RAPIDAPI_HOST)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API call failed", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(CityActivity.this, "Failed to search cities", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    parseCitiesResponse(responseBody);
                }
            }
        });
    }

    private void parseCitiesResponse(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray citiesArray = jsonResponse.optJSONArray("data");

            List<City> newCities = new ArrayList<>();

            if (citiesArray != null) {
                for (int i = 0; i < citiesArray.length(); i++) {
                    JSONObject cityJson = citiesArray.getJSONObject(i);

                    String name = cityJson.optString("name", "");
                    String country = cityJson.optString("country", "");
                    String region = cityJson.optString("region", "");
                    double latitude = cityJson.optDouble("latitude", 0.0);
                    double longitude = cityJson.optDouble("longitude", 0.0);
                    int population = cityJson.optInt("population", 0);

                    // Filter out "Metropolitan City of Rome" entries
                    if (name.toLowerCase().contains("metropolitan city of rome") ||
                            region.toLowerCase().contains("metropolitan city of rome")) {
                        continue;
                    }

                    // Extract country code if available, otherwise map from country name
                    String countryCode = cityJson.optString("countryCode", "");
                    if (countryCode.isEmpty()) {
                        countryCode = getCountryCode(country);
                    }

                    if (!name.isEmpty() && !country.isEmpty() && latitude != 0.0 && longitude != 0.0 && population > 0) {
                        City city = new City(name, country, region, latitude, longitude, population, countryCode);
                        newCities.add(city);
                    }
                }
            }

            runOnUiThread(() -> {
                cityList.clear();
                cityList.addAll(newCities);
                autoCompleteAdapter.notifyDataSetChanged();
                showLoading(false);

                if (!cityList.isEmpty()) {
                    searchCity.showDropDown();
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing cities response", e);
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(CityActivity.this, "Error processing search results", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String getCountryCode(String countryName) {
        return COUNTRY_CODE_MAP.getOrDefault(countryName, "");
    }

    private void updateMapWithSelectedCity() {
        if (googleMap != null && selectedCity != null) {
            LatLng cityLatLng = new LatLng(selectedCity.getLatitude(), selectedCity.getLongitude());

            // Clear previous marker
            if (currentMarker != null) {
                currentMarker.remove();
            }

            // Add new marker
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(cityLatLng)
                    .title(selectedCity.getName())
                    .snippet(selectedCity.getCountry())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            currentMarker = googleMap.addMarker(markerOptions);

            // Animate camera to selected city
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(cityLatLng, 12f),
                    1000,
                    null
            );
        }
    }

    private void enableContinueButton() {
        nextButton.setEnabled(true);
        nextButton.setAlpha(1.0f);
        nextButton.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(100)
                .withEndAction(() -> nextButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start())
                .start();
    }

    private void clearSelection() {
        selectedCity = null;
        nextButton.setEnabled(false);
        nextButton.setAlpha(0.5f);
        if (currentMarker != null) {
            currentMarker.remove();
            currentMarker = null;
        }
    }

    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void navigateToCalendarActivity() {
        if (selectedCity != null) {
            String cityCountryOnly = selectedCity.getName() + ", " + selectedCity.getCountry();
            LatLng cityLatLng = new LatLng(selectedCity.getLatitude(), selectedCity.getLongitude());

            Intent intent = new Intent(CityActivity.this, CalendarActivity.class);
            intent.putExtra("city", cityCountryOnly);
            intent.putExtra("latitude", cityLatLng.latitude);
            intent.putExtra("longitude", cityLatLng.longitude);
            startActivity(intent);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Configure map settings
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);

        // Set initial camera position (world view)
        LatLng initialPosition = new LatLng(20.0, 0.0);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 2f));

        // Apply custom map style (optional)
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            );
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
        // Clear flag cache to free memory
        if (autoCompleteAdapter != null) {
            autoCompleteAdapter.clearFlagCache();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // Updated City data class with country code support
    public static class City {
        private final String name;
        private final String country;
        private final String region;
        private final double latitude;
        private final double longitude;
        private final int population;
        private final String countryCode;

        public City(String name, String country, String region, double latitude, double longitude, int population, String countryCode) {
            this.name = name;
            this.country = country;
            this.region = region;
            this.latitude = latitude;
            this.longitude = longitude;
            this.population = population;
            this.countryCode = countryCode;
        }

        public String getName() { return name; }
        public String getCountry() { return country; }
        public String getRegion() { return region; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public int getPopulation() { return population; }
        public String getCountryCode() { return countryCode; }

        public String getDisplayName() {
            if (region != null && !region.isEmpty() && !region.equals(country)) {
                return name + ", " + region + ", " + country;
            }
            return name + ", " + country;
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }
}
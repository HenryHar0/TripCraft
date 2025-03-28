package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CityActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AutoCompleteTextView searchCity;
    private MaterialButton nextButton;
    private FloatingActionButton myLocationButton;
    private GoogleMap mMap;
    private String geoNamesUsername = "henryhar";
    private ArrayAdapter<String> cityAdapter;
    private boolean isMapReady = false;
    private final HashMap<String, LatLng> cityCoordinates = new HashMap<>(); // Store city coordinates
    private Timer searchDebounceTimer = new Timer();
    private String selectedCityName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city);

        // Initialize views based on the new layout
        searchCity = findViewById(R.id.search_city);
        nextButton = findViewById(R.id.next_button);
        myLocationButton = findViewById(R.id.my_location_button);

        // Setup adapter for city autocomplete
        cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        searchCity.setAdapter(cityAdapter);
        nextButton.setEnabled(false);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup text change listener with debounce for API calls
        searchCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                searchDebounceTimer.cancel();
                searchDebounceTimer = new Timer();
                searchDebounceTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> fetchCitySuggestions(charSequence.toString().trim()));
                    }
                }, 500); // Debounce API calls (500ms delay)
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Handle selection from dropdown
        searchCity.setOnItemClickListener((parent, view, position, id) -> {
            selectedCityName = (String) parent.getItemAtPosition(position);
            if (cityCoordinates.containsKey(selectedCityName)) {
                updateMap(cityCoordinates.get(selectedCityName), selectedCityName);
                nextButton.setEnabled(true);
            }
        });

        // Set up my location button
        myLocationButton.setOnClickListener(v -> {
            if (isMapReady) {
                // This would typically request location permissions and center the map
                // on the user's current location, but for simplicity we'll just show a toast
                Toast.makeText(this, "Location functionality would center map on your position",
                        Toast.LENGTH_SHORT).show();
            }
        });

        nextButton.setOnClickListener(v -> {
            if (!selectedCityName.isEmpty() && cityCoordinates.containsKey(selectedCityName)) {
                LatLng cityLatLng = cityCoordinates.get(selectedCityName);

                Intent intent = new Intent(CityActivity.this, CalendarActivity.class);
                intent.putExtra("city", selectedCityName);
                intent.putExtra("selected_city_coordinates", cityLatLng);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please select a city before proceeding.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        isMapReady = true;
    }

    private void fetchCitySuggestions(String query) {
        if (query.isEmpty()) return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://secure.geonames.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GeoNamesAPI geoNamesAPI = retrofit.create(GeoNamesAPI.class);
        Call<GeoNamesResponse> call = geoNamesAPI.searchCity(query, 10, geoNamesUsername);

        call.enqueue(new Callback<GeoNamesResponse>() {
            @Override
            public void onResponse(Call<GeoNamesResponse> call, Response<GeoNamesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cityAdapter.clear();
                    cityCoordinates.clear();
                    for (GeoNamesResponse.City city : response.body().geonames) {
                        String cityName = city.name + ", " + city.countryName;
                        cityAdapter.add(cityName);
                        cityCoordinates.put(cityName, new LatLng(city.lat, city.lng));
                    }
                    cityAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<GeoNamesResponse> call, Throwable t) {
                Toast.makeText(CityActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMap(LatLng cityLocation, String cityName) {
        if (!isMapReady) {
            Toast.makeText(this, "Map is not ready yet. Please try again later.", Toast.LENGTH_SHORT).show();
            return;
        }
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(cityLocation).title(cityName));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cityLocation, 10));
    }
}
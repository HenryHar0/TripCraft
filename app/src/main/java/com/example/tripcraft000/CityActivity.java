package com.example.tripcraft000;

import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CityActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AutoCompleteTextView searchCity;
    private Button nextButton;
    private GoogleMap mMap;
    private String geoNamesUsername = "henryhar";
    private ArrayAdapter<String> cityAdapter;
    private boolean isMapReady = false; // Flag to track if the map is ready

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city);

        searchCity = findViewById(R.id.search_city);
        nextButton = findViewById(R.id.next_button);
        cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        searchCity.setAdapter(cityAdapter);

        // Setup map fragment and load the map asynchronously
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Add listener for city search
        searchCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String city = charSequence.toString().trim();
                if (!city.isEmpty()) {
                    fetchCitySuggestions(city);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Button click listener for city selection
        nextButton.setOnClickListener(v -> {
            String city = searchCity.getText().toString().trim();
            if (city.isEmpty()) {
                Toast.makeText(CityActivity.this, "Please enter a city.", Toast.LENGTH_SHORT).show();
            } else {
                fetchCityDetails(city);
            }
        });
    }

    // Callback when the map is ready to be used
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        isMapReady = true; // Set flag to true when the map is ready
    }

    // Fetch city suggestions based on user input
    private void fetchCitySuggestions(String city) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://secure.geonames.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GeoNamesAPI geoNamesAPI = retrofit.create(GeoNamesAPI.class);
        Call<GeoNamesResponse> call = geoNamesAPI.searchCity(city, 10, geoNamesUsername);

        call.enqueue(new Callback<GeoNamesResponse>() {
            @Override
            public void onResponse(Call<GeoNamesResponse> call, Response<GeoNamesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cityAdapter.clear();
                    for (GeoNamesResponse.City city : response.body().geonames) {
                        cityAdapter.add(city.name + ", " + city.countryName);
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

    // Fetch city details (latitude, longitude) when a city is selected
    private void fetchCityDetails(String cityWithCountry) {
        if (!isMapReady) {
            Toast.makeText(CityActivity.this, "Map is not ready yet. Please try again later.", Toast.LENGTH_SHORT).show();
            return;
        }

        String city = cityWithCountry.split(",")[0].trim();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://secure.geonames.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GeoNamesAPI geoNamesAPI = retrofit.create(GeoNamesAPI.class);
        Call<GeoNamesResponse> call = geoNamesAPI.searchCity(city, 1, geoNamesUsername);

        call.enqueue(new Callback<GeoNamesResponse>() {
            @Override
            public void onResponse(Call<GeoNamesResponse> call, Response<GeoNamesResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().geonames.isEmpty()) {
                    GeoNamesResponse.City selectedCity = response.body().geonames.get(0);
                    LatLng cityLocation = new LatLng(Double.parseDouble(String.valueOf(selectedCity.lat)), Double.parseDouble(String.valueOf(selectedCity.lng)));

                    // Safely manipulate map after ensuring it's ready
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(cityLocation).title(selectedCity.name));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cityLocation, 10));
                }
            }

            @Override
            public void onFailure(Call<GeoNamesResponse> call, Throwable t) {
                Toast.makeText(CityActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

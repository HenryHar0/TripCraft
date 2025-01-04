package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CityActivity extends AppCompatActivity {

    private AutoCompleteTextView searchCity;
    private Button nextButton;
    private String geoNamesUsername = "henryhar";
    private ArrayAdapter<String> cityAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city);

        searchCity = findViewById(R.id.search_city);
        nextButton = findViewById(R.id.next_button);

        // Set up AutoComplete for city input
        cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        searchCity.setAdapter(cityAdapter);

        // Listen for text changes in the searchCity EditText
        searchCity.addTextChangedListener(new android.text.TextWatcher() {
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
            public void afterTextChanged(android.text.Editable editable) {}
        });

        // Handle item selection from the dropdown
        searchCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = cityAdapter.getItem(position);
            if (selectedCity != null) {
                searchCity.setText(selectedCity); // Set the selected city in the input
            }
        });

        // Handle "Next" button click
        nextButton.setOnClickListener(v -> {
            String city = searchCity.getText().toString().trim();

            if (city.isEmpty()) {
                Toast.makeText(CityActivity.this, "Please enter a city.", Toast.LENGTH_SHORT).show();
            } else {
                fetchCityDetails(city);
            }
        });
    }

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
                    GeoNamesResponse geoNamesResponse = response.body();
                    if (geoNamesResponse.geonames.size() > 0) {
                        // Prepare suggestions list
                        cityAdapter.clear();
                        for (GeoNamesResponse.City city : geoNamesResponse.geonames) {
                            String cityWithCountry = city.name + ", " + city.countryName;
                            cityAdapter.add(cityWithCountry);
                        }
                        cityAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(CityActivity.this, "No cities found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CityActivity.this, "Failed to fetch city data.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeoNamesResponse> call, Throwable t) {
                Toast.makeText(CityActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCityDetails(String cityWithCountry) {
        // Extract city name from "City, Country"
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
                if (response.isSuccessful() && response.body() != null) {
                    GeoNamesResponse geoNamesResponse = response.body();
                    if (geoNamesResponse.geonames.size() > 0) {
                        GeoNamesResponse.City selectedCity = geoNamesResponse.geonames.get(0);

                        Intent intent = new Intent(CityActivity.this, CalendarActivity.class);
                        intent.putExtra("city", selectedCity.name);
                        intent.putExtra("country", selectedCity.countryName);
                        intent.putExtra("lat", selectedCity.lat);
                        intent.putExtra("lng", selectedCity.lng);
                        startActivity(intent);
                    } else {
                        Toast.makeText(CityActivity.this, "City not found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CityActivity.this, "Failed to fetch city data.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeoNamesResponse> call, Throwable t) {
                Toast.makeText(CityActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

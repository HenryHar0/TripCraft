package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InterestsActivity extends AppCompatActivity {

    private static final String TAG = "InterestsActivity";
    private static final int REQUEST_CODE_PLACE_SELECTION = 100;
    private static final int MAX_CATEGORIES = 6;

    // UI Components
    private LinearLayout interestsLayout;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button applyFiltersButton;
    private LinearLayout categoriesLayout;
    private Button nextButton;
    private TextView categoryLimitText;

    // Data
    private String selectedCityName;
    private LatLng selectedCityCoordinates;
    private Set<String> selectedCategories = new HashSet<>();
    private List<String> allCategories = new ArrayList<>();
    private final Set<String> selectedPlaceIds = new HashSet<>();
    private CheckBox selectAllCheckbox;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);

        // Initialize UI components
        initializeUI();

        // Get data from intent
        getDataFromIntent();

        // Fetch city coordinates if needed
        if (selectedCityCoordinates.latitude == 0 && selectedCityCoordinates.longitude == 0) {
            fetchCityCoordinatesFromGeoNames(selectedCityName);
        }

        // Set city title
        setPageTitle();

        // Setup button click listeners
        setupButtonListeners();

        // Load predefined categories
        loadPredefinedCategories();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void initializeUI() {
        interestsLayout = findViewById(R.id.interestsLayout);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        categoriesLayout = findViewById(R.id.categoriesLayout);
        nextButton = findViewById(R.id.nextButton);

        // Add category limit text
        categoryLimitText = new TextView(this);
        categoryLimitText.setText("Select up to " + MAX_CATEGORIES + " categories");
        categoryLimitText.setTextSize(14);
        categoryLimitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        categoryLimitText.setPadding(16, 4, 16, 16);
        categoriesLayout.addView(categoryLimitText);
    }

    private void getDataFromIntent() {
        selectedCityName = getIntent().getStringExtra("city");
        if (selectedCityName == null) {
            selectedCityName = "Your destination";
        }

        // Get coordinates from intent
        double lat = getIntent().getDoubleExtra("city_lat", 0);
        double lng = getIntent().getDoubleExtra("city_lng", 0);

        // If coordinates were passed, use them
        selectedCityCoordinates = new LatLng(lat, lng);
    }

    private void setPageTitle() {
        TextView titleTextView = findViewById(R.id.titleText);
        if (titleTextView != null) {
            titleTextView.setText("Explore " + selectedCityName);
        }
    }

    private void setupButtonListeners() {
        if (applyFiltersButton != null) {
            applyFiltersButton.setOnClickListener(v -> applyFilters());
        }

        if (nextButton != null) {
            nextButton.setOnClickListener(v -> navigateToNextScreen());
            nextButton.setEnabled(true);
        }
    }

    private void fetchCityCoordinatesFromGeoNames(String cityName) {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Fetching location data...");

        executorService.execute(() -> {
            try {
                String apiUrl = "http://api.geonames.org/searchJSON?q=" + cityName +
                        "&maxRows=1&username=henryhar";
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    java.io.InputStream inputStream = connection.getInputStream();
                    java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";

                    org.json.JSONObject json = new org.json.JSONObject(response);
                    if (json.has("geonames") && json.getJSONArray("geonames").length() > 0) {
                        org.json.JSONObject location = json.getJSONArray("geonames").getJSONObject(0);
                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");

                        selectedCityCoordinates = new LatLng(lat, lng);
                        Log.d(TAG, "Fetched coordinates: " + lat + ", " + lng);
                    } else {
                        Log.w(TAG, "No coordinates found for: " + cityName);
                    }
                } else {
                    Log.e(TAG, "Error response code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception fetching coordinates", e);
            } finally {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Choose categories to explore in " + selectedCityName);
                });
            }
        });
    }

    private void loadPredefinedCategories() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Loading categories...");

        // Clear previous data
        allCategories.clear();

        // Add all possible categories from getFormattedPlaceType method
        allCategories.add("🏛 Museum");
        allCategories.add("📸 Tourist Attraction");
        allCategories.add("🍽 Restaurant");
        allCategories.add("☕ Cafe");
        allCategories.add("🍹 Bar");
        allCategories.add("🛍 Shopping Mall");
        allCategories.add("🎭 Theater");
        allCategories.add("🎬 Cinema");
        allCategories.add("🎶 Night Club");
        allCategories.add("🌳 Park");
        allCategories.add("🏖 Beach");
        allCategories.add("🏞 Nature Spot");
        allCategories.add("🖼 Art Gallery");
        allCategories.add("🙏 Place of Worship");
        allCategories.add("🦁 Zoo");
        allCategories.add("🐠 Aquarium");
        allCategories.add("🎢 Amusement Park");
        allCategories.add("🚂 Train Station");
        allCategories.add("🚇 Metro Station");

        // Update UI with categories
        updateUI();
    }

    private synchronized void updateUI() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            categoriesLayout.setVisibility(View.VISIBLE);

            // Remove all views except the category limit text
            for (int i = categoriesLayout.getChildCount() - 1; i >= 0; i--) {
                if (categoriesLayout.getChildAt(i) != categoryLimitText) {
                    categoriesLayout.removeViewAt(i);
                }
            }

            if (allCategories.isEmpty()) {
                statusText.setText("No categories available. Please try again.");
                return;
            }

            statusText.setText("Choose categories to explore in " + selectedCityName);

            // Add "Select All" checkbox
            addSelectAllCheckbox();

            // Add category checkboxes
            addCategoryCheckboxes();

            // Display initially selected categories
            displaySelectedCategories();

            // Update category count indicator
            updateCategoryCountIndicator();
        });
    }

    private void addSelectAllCheckbox() {
        selectAllCheckbox = new CheckBox(this);
        selectAllCheckbox.setText("Select All");
        selectAllCheckbox.setChecked(false);
        selectAllCheckbox.setTextSize(16);
        selectAllCheckbox.setPadding(8, 16, 8, 16);

        selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && allCategories.size() > MAX_CATEGORIES) {
                // Prevent selecting all if it would exceed the limit
                Toast.makeText(this, "You can select a maximum of " + MAX_CATEGORIES + " categories",
                        Toast.LENGTH_SHORT).show();
                selectAllCheckbox.setChecked(false);
                return;
            }

            // Update all checkboxes and selected categories at once
            for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
                View child = categoriesLayout.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout row = (LinearLayout) child;
                    for (int j = 0; j < row.getChildCount(); j++) {
                        View checkboxView = row.getChildAt(j);
                        if (checkboxView instanceof CheckBox && checkboxView != buttonView) {
                            ((CheckBox) checkboxView).setChecked(isChecked);

                            // Update selected categories
                            if (checkboxView.getTag() != null) {
                                String category = (String) checkboxView.getTag();
                                if (isChecked) {
                                    selectedCategories.add(category);
                                } else {
                                    selectedCategories.remove(category);
                                }
                            }
                        }
                    }
                }
            }

            // Update displayed categories immediately
            displaySelectedCategories();
            updateCategoryCountIndicator();
        });

        LinearLayout selectAllRow = new LinearLayout(this);
        selectAllRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        selectAllRow.addView(selectAllCheckbox);
        selectAllRow.setPadding(16, 8, 16, 16);
        categoriesLayout.addView(selectAllRow);
    }

    private void addCategoryCheckboxes() {
        int columnCount = 2;
        LinearLayout currentRow = null;

        for (int i = 0; i < allCategories.size(); i++) {
            if (i % columnCount == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setPadding(16, 8, 16, 8);
                categoriesLayout.addView(currentRow);
            }

            String category = allCategories.get(i);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(category);
            checkBox.setTag(category);
            checkBox.setChecked(selectedCategories.contains(category));
            checkBox.setTextSize(14);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String categoryName = (String) buttonView.getTag();

                if (isChecked) {
                    // Check if adding this would exceed the limit
                    if (selectedCategories.size() >= MAX_CATEGORIES) {
                        Toast.makeText(this, "You can select a maximum of " + MAX_CATEGORIES + " categories",
                                Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                        return;
                    }
                    selectedCategories.add(categoryName);
                } else {
                    selectedCategories.remove(categoryName);
                    // Uncheck "Select All" if any item is unchecked
                    if (selectAllCheckbox.isChecked()) {
                        selectAllCheckbox.setChecked(false);
                    }
                }

                // Update UI immediately for better user feedback
                updateCategoryCountIndicator();
                displaySelectedCategories();
            });

            LinearLayout.LayoutParams checkboxParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f);
            checkBox.setLayoutParams(checkboxParams);

            if (currentRow != null) {
                currentRow.addView(checkBox);
            }
        }
    }

    private void updateCategoryCountIndicator() {
        int count = selectedCategories.size();

        if (count > 0) {
            categoryLimitText.setText(count + " of " + MAX_CATEGORIES + " categories selected");

            if (count == MAX_CATEGORIES) {
                categoryLimitText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
            } else {
                categoryLimitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            }
        } else {
            categoryLimitText.setText("Select up to " + MAX_CATEGORIES + " categories");
            categoryLimitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        if (count > 0) {
            statusText.setText("Selected " + count + " categories");
        } else {
            statusText.setText("Choose categories to explore in " + selectedCityName);
        }
    }

    private void applyFilters() {
        displaySelectedCategories();
        Toast.makeText(this, selectedCategories.size() + " categories selected", Toast.LENGTH_SHORT).show();
    }

    private void displaySelectedCategories() {
        interestsLayout.removeAllViews();

        if (selectedCategories.isEmpty()) {
            CardView messageCard = createTitleCard("Select categories from the list above");
            interestsLayout.addView(messageCard);
            return;
        }

        CardView titleCard = createTitleCard("Your Interests in " + selectedCityName);
        interestsLayout.addView(titleCard);

        for (String category : allCategories) {
            if (selectedCategories.contains(category)) {
                createCategoryCard(category);
            }
        }
    }

    private CardView createTitleCard(String title) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(16, 16, 16, 16);
        card.setLayoutParams(cardParams);
        card.setCardElevation(8);
        card.setRadius(12);

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(18);
        titleText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        titleText.setPadding(24, 24, 24, 24);
        titleText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        card.addView(titleText);
        return card;
    }

    private void createCategoryCard(final String category) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(16, 8, 16, 8);
        card.setLayoutParams(cardParams);
        card.setCardElevation(4);
        card.setRadius(12);

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(16, 16, 16, 16);

        TextView categoryText = new TextView(this);
        categoryText.setText(category);
        categoryText.setTextSize(16);
        categoryText.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        Button viewButton = new Button(this);
        viewButton.setText("View Places");
        viewButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        viewButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        viewButton.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, 16, 0, 0);
        viewButton.setLayoutParams(buttonParams);

        viewButton.setOnClickListener(v -> {
            Intent intent = new Intent(InterestsActivity.this, MapPlacesActivity.class);
            intent.putExtra("city_name", selectedCityName);
            intent.putExtra("city_lat", selectedCityCoordinates.latitude);
            intent.putExtra("city_lng", selectedCityCoordinates.longitude);
            intent.putExtra("start_date", getIntent().getStringExtra("start_date"));
            intent.putExtra("end_date", getIntent().getStringExtra("end_date"));
            intent.putExtra("category_name", category);

            // Start activity for result to get the selected places back
            startActivityForResult(intent, REQUEST_CODE_PLACE_SELECTION);
        });

        cardLayout.addView(categoryText);
        cardLayout.addView(viewButton);

        card.addView(cardLayout);
        interestsLayout.addView(card);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PLACE_SELECTION && resultCode == RESULT_OK && data != null) {
            ArrayList<String> newSelectedPlaceIds = data.getStringArrayListExtra("selected_place_ids");

            if (newSelectedPlaceIds != null && !newSelectedPlaceIds.isEmpty()) {
                // Add the newly selected places to our set
                selectedPlaceIds.addAll(newSelectedPlaceIds);

                Log.d(TAG, "Added " + newSelectedPlaceIds.size() +
                        " places. Total selected: " + selectedPlaceIds.size());

                // Show feedback to user
                Toast.makeText(this, "Added " + newSelectedPlaceIds.size() + " places to your plan",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToNextScreen() {
        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, "Please select at least one category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create intent for TimeActivity
        Intent intent = new Intent(InterestsActivity.this, TimeActivity.class);

        // Pass along all the information received from the previous activity
        intent.putExtra("city", selectedCityName);
        intent.putExtra("start_date", getIntent().getStringExtra("start_date"));
        intent.putExtra("end_date", getIntent().getStringExtra("end_date"));

        // Pass city coordinates
        intent.putExtra("city_lat", selectedCityCoordinates.latitude);
        intent.putExtra("city_lng", selectedCityCoordinates.longitude);

        Log.d(TAG, "City Coordinates: Latitude: " + selectedCityCoordinates.latitude +
                ", Longitude: " + selectedCityCoordinates.longitude);

        // Pass selected categories
        intent.putStringArrayListExtra("selected_categories", new ArrayList<>(selectedCategories));

        // Pass selected places
        ArrayList<String> selectedPlacesList = new ArrayList<>(selectedPlaceIds);
        Log.d(TAG, "Passing " + selectedPlacesList.size() + " selected places to TimeActivity");
        intent.putStringArrayListExtra("selected_place_ids", selectedPlacesList);

        // Start PlanActivity
        startActivity(intent);
        finish();
    }
}
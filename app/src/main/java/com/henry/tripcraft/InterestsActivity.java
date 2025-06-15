package com.henry.tripcraft;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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
    private ChipGroup categoriesChipGroup;
    private MaterialButton nextButton;
    private ImageButton backButton;
    private TextView titleTextView;
    private TextView subtitleTextView;

    // Data
    private String selectedCityName;
    private Set<String> selectedCategories = new HashSet<>();
    private final Set<String> selectedPlaceIds = new HashSet<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Map of chip IDs to categories
    private final List<String> categoryNamesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);

        // Initialize UI components
        initializeUI();

        // Get data from intent
        getDataFromIntent();

        // Set city title
        setPageTitle();

        // Setup button click listeners
        setupButtonListeners();

        // Setup chip listeners
        setupChipListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void initializeUI() {
        interestsLayout = findViewById(R.id.interestsLayout);
        categoriesChipGroup = findViewById(R.id.categoriesLayout);
        nextButton = findViewById(R.id.next_button);
        backButton = findViewById(R.id.back_button);
        titleTextView = findViewById(R.id.title);
        subtitleTextView = findViewById(R.id.subtitle);
    }

    private void getDataFromIntent() {
        selectedCityName = getIntent().getStringExtra("city");
        if (selectedCityName == null) {
            selectedCityName = "Your destination";
        };
    }

    private void setPageTitle() {
        if (titleTextView != null) {
            titleTextView.setText("Explore " + selectedCityName);
        }
    }

    private void setupButtonListeners() {
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> navigateToNextScreen());
            nextButton.setEnabled(true);
        }

        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(InterestsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

    }

    private void setupChipListeners() {
        // Map all category chips
        setupCategoryMapping();

        // Setup click listeners for all chips
        for (int i = 0; i < categoriesChipGroup.getChildCount(); i++) {
            View view = categoriesChipGroup.getChildAt(i);
            if (view instanceof Chip) {
                Chip chip = (Chip) view;

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    String category = buttonView.getText().toString();

                    if (isChecked) {
                        // Check if adding this would exceed the limit
                        if (selectedCategories.size() >= MAX_CATEGORIES) {
                            Toast.makeText(this, "You can select a maximum of " + MAX_CATEGORIES + " categories",
                                    Toast.LENGTH_SHORT).show();
                            buttonView.setChecked(false);
                            return;
                        }
                        selectedCategories.add(category);
                    } else {
                        selectedCategories.remove(category);
                    }

                    // Update UI for better user feedback
                    displaySelectedCategories();
                });
            }
        }
    }

    private void setupCategoryMapping() {
        // Clear previous mappings
        categoryNamesList.clear();

        // Add each chip ID and its corresponding category name
        categoryNamesList.add("Museum");
        categoryNamesList.add("Tourist Attraction");
        categoryNamesList.add("Theater");
        categoryNamesList.add("Night Club");
        categoryNamesList.add("Park");
        categoryNamesList.add("Beach");
        categoryNamesList.add("Art Gallery");
        categoryNamesList.add("Place of Worship");
        categoryNamesList.add("Zoo");
        categoryNamesList.add("Aquarium");
        categoryNamesList.add("Amusement Park");
    }


    private void displaySelectedCategories() {
        interestsLayout.removeAllViews();

        if (selectedCategories.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("Select categories from the bottom");
            emptyView.setTextSize(16);
            emptyView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            emptyView.setPadding(16, 16, 16, 16);
            interestsLayout.addView(emptyView);
            return;
        }

        for (String category : selectedCategories) {
            createCategoryCard(category);
        }
    }

    private void createCategoryCard(final String category) {
        // Create a MaterialCardView with enhanced design
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(16, 12, 16, 12); // Increased margins for better spacing
        card.setLayoutParams(cardParams);
        card.setRadius(24); // Increased corner radius for modern look
        card.setCardElevation(4); // Slightly increased elevation
        card.setStrokeWidth(0); // No stroke

        // Set card background color based on category
        card.setCardBackgroundColor(getColorForCategory(category));

        // Set up ripple effect for card touch feedback
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        card.setForeground(getDrawable(outValue.resourceId));
        card.setClickable(true);

        // Create card content layout
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setPadding(24, 20, 24, 20); // Increased padding for better spacing
        cardLayout.setGravity(Gravity.CENTER_VERTICAL); // Center items vertically
        cardLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Find the appropriate icon based on category
        int iconResId = getIconResourceForCategory(category);

        // Create icon view with improved design
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(iconResId);
        int iconSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                32, // Larger icon size
                getResources().getDisplayMetrics());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMarginEnd(16);
        iconView.setLayoutParams(iconParams);

        // Apply appropriate tint to the icon based on category
        iconView.setColorFilter(ContextCompat.getColor(this, android.R.color.white),
                PorterDuff.Mode.SRC_IN);

        // Create text layout for category name with subtle shadow
        TextView categoryText = new TextView(this);
        categoryText.setText(category);
        categoryText.setTextSize(18); // Slightly larger text
        categoryText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); // Medium weight font
        categoryText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        // Add subtle text shadow for better readability on colored backgrounds
        categoryText.setShadowLayer(1.5f, 0.5f, 0.5f, Color.parseColor("#33000000"));

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        categoryText.setLayoutParams(textParams);

        // Enhanced "View" button
        MaterialButton viewButton = new MaterialButton(this);
        viewButton.setText("View");
        viewButton.setTextSize(14);
        viewButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        viewButton.setTextColor(getCardButtonTextColor(category));
        viewButton.setBackgroundColor(getCardButtonColor(category));
        viewButton.setStrokeWidth(0);
        viewButton.setCornerRadius(20); // Increased corner radius
        viewButton.setMinimumWidth(120);
        viewButton.setPadding(24, 12, 24, 12); // Better button padding

        // Add ripple effect for better touch feedback
        viewButton.setRippleColor(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")));

        // Create button wrapper to help with animations
        FrameLayout buttonWrapper = new FrameLayout(this);
        buttonWrapper.addView(viewButton);

        viewButton.setOnClickListener(v -> {
            // Add button press animation
            viewButton.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        viewButton.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100);

                        // Original functionality preserved
                        Intent intent = new Intent(InterestsActivity.this, MapPlacesActivity.class);
                        intent.putExtra("city_name", selectedCityName);
                        intent.putExtra("start_date", getIntent().getStringExtra("start_date"));
                        intent.putExtra("end_date", getIntent().getStringExtra("end_date"));
                        intent.putExtra("category_name", category);

                        // Start activity for result to get the selected places back
                        startActivityForResult(intent, REQUEST_CODE_PLACE_SELECTION);
                    });
        });

        // Add all views to card layout
        cardLayout.addView(iconView);
        cardLayout.addView(categoryText);
        cardLayout.addView(buttonWrapper);

        card.addView(cardLayout);

        // Add card with entrance animation
        interestsLayout.addView(card);

        // Run entrance animation
        card.setAlpha(0f);
        card.setScaleX(0.9f);
        card.setScaleY(0.9f);
        card.setTranslationY(50);

        card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    // Helper method to get background color based on category
    private int getColorForCategory(String category) {
        // Create a hash from the category name to generate consistent colors
        int hash = category.hashCode();

        // Select from a set of predefined vibrant colors based on category hash
        String[] colorPalette = {
                "#4285F4", // Google Blue
                "#EA4335", // Google Red
                "#FBBC05", // Google Yellow
                "#34A853", // Google Green
                "#8E24AA", // Purple
                "#0097A7", // Teal
                "#F57C00", // Orange
                "#C2185B", // Pink
                "#7CB342", // Light Green
                "#00ACC1"  // Cyan
        };

        int index = Math.abs(hash % colorPalette.length);
        return Color.parseColor(colorPalette[index]);
    }

    // Helper method to get button color based on category
    private int getCardButtonColor(String category) {
        // Create semi-transparent white button for contrast against colored card
        return Color.parseColor("#DDFFFFFF");
    }

    // Helper method to get button text color based on category
    private int getCardButtonTextColor(String category) {
        // Get base color and darken it for text
        int baseColor = getColorForCategory(category);

        // Convert color to HSV, reduce value (darken)
        float[] hsv = new float[3];
        Color.colorToHSV(baseColor, hsv);
        hsv[2] *= 0.7f; // Darken by reducing value

        return Color.HSVToColor(hsv);
    }


    private int getIconResourceForCategory(String category) {
        // Map category names to drawable resources
        // Note: replace with actual drawable resources from your project
        switch (category) {
            case "Museum":
                return R.drawable.ic_museum;
            case "Tourist Attraction":
                return R.drawable.ic_tourist_attraction;
            case "Theater":
                return R.drawable.ic_theater;
            case "Night Club":
                return R.drawable.ic_night_club;
            case "Park":
                return R.drawable.ic_park;
            case "Beach":
                return R.drawable.ic_beach;
            case "Art Gallery":
                return R.drawable.ic_art_gallery;
            case "Place of Worship":
                return R.drawable.ic_place_of_worship;
            case "Zoo":
                return R.drawable.ic_zoo;
            case "Aquarium":
                return R.drawable.ic_aquarium;
            case "Amusement Park":
                return R.drawable.ic_amusement_park;
            default:
                return 0; // No matching icon
        }
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


        // Pass selected categories
        intent.putStringArrayListExtra("selected_categories", new ArrayList<>(selectedCategories));

        // Pass selected places
        ArrayList<String> selectedPlacesList = new ArrayList<>(selectedPlaceIds);
        intent.putStringArrayListExtra("selected_place_ids", selectedPlacesList);


        // Start TimeActivity
        startActivity(intent);
        finish();
    }
}
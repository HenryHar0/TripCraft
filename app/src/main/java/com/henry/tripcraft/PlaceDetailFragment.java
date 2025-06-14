package com.henry.tripcraft;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceDetailFragment extends Fragment {

    private static final String ARG_PLACE_DATA = "place_data";
    private static final String ARG_API_KEY = "api_key";
    private static final String COHERE_API_URL = "https://api.cohere.ai/v1/generate";

    private PlaceData placeData;
    private String apiKey;
    private String coherekey = "1fVwl5TkD22DoOXqV1ksxpB9ZOUU4wDtZO8LtkYH";
    private CardView backButton; // Changed from ImageView to CardView
    private OkHttpClient httpClient;
    private Map<String, String> aboutCache;

    // Views from item_place11 layout
    private ImageView placeImage;
    private TextView placeName;
    private TextView placeType;
    private TextView placeRating;
    private ImageView typeDrawable;
    private TextView placeTypeValue;
    private TextView priceLevelValue;
    private TextView openingHoursValue;
    private TextView priceLevelText;
    private ImageView ticketDrawable;
    private TextView aboutText;
    private LinearLayout viewMapsButton;

    // Interface for async callback
    private interface DescriptionCallback {
        void onSuccess(String description);
        void onError(String error);
    }

    public static PlaceDetailFragment newInstance(PlaceData placeData, String apiKey) {
        PlaceDetailFragment fragment = new PlaceDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLACE_DATA, placeData);
        args.putString(ARG_API_KEY, apiKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            placeData = (PlaceData) getArguments().getSerializable(ARG_PLACE_DATA);
            apiKey = getArguments().getString(ARG_API_KEY);
        }
        httpClient = new OkHttpClient();
        aboutCache = new HashMap<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the item_place11 layout directly
        View view = inflater.inflate(R.layout.item_place11, container, false);

        // Add back button to the layout
        addBackButton(view);

        // Initialize views
        initializeViews(view);

        // Populate the views with place data
        populateViews();

        return view;
    }

    private void addBackButton(View rootView) {
        backButton = rootView.findViewById(R.id.backButton); // Now correctly finds CardView
        if (backButton != null) {
            setupBackButton();
        }
    }

    private void initializeViews(View view) {
        placeImage = view.findViewById(R.id.placeImage);
        placeName = view.findViewById(R.id.placeName);
        placeType = view.findViewById(R.id.placeType);
        placeRating = view.findViewById(R.id.placeRating);
        typeDrawable = view.findViewById(R.id.typeDrawable);
        placeTypeValue = view.findViewById(R.id.placeTypeValue);
        priceLevelValue = view.findViewById(R.id.priceLevelValue);
        openingHoursValue = view.findViewById(R.id.openingHoursValue);
        priceLevelText = view.findViewById(R.id.priceLevelText);
        ticketDrawable = view.findViewById(R.id.ticketDrawable);
        aboutText = view.findViewById(R.id.aboutText);
        viewMapsButton = view.findViewById(R.id.viewMapsButton);
    }

    private void populateViews() {
        if (placeData == null) return;

        // Set place name
        placeName.setText(placeData.getName());

        // Set place type in the main card
        String formattedType = getFormattedPlaceType(placeData.getPlaceType());
        if (formattedType != null) {
            if (placeType != null) {
                placeType.setText(formattedType);
                placeType.setVisibility(View.VISIBLE);
            }
            placeTypeValue.setText(formattedType);
        } else {
            if (placeType != null) {
                placeType.setVisibility(View.GONE);
            }
            placeTypeValue.setText("N/A");
        }

        // Set type drawable
        setTypeDrawable(placeData.getPlaceType());

        // Set rating
        float rating = placeData.getRating();
        if (rating > 0) {
            placeRating.setText(String.format("%.1f", rating));
            placeRating.setVisibility(View.VISIBLE);
        } else {
            placeRating.setVisibility(View.GONE);
        }

        // Set price level or website
        setPriceLevelOrWebsite();

        // Set opening hours
        setOpeningHours();

        // Load image
        loadPlaceImage();

        // Generate about section
        generateAboutSection();

        // Set maps button
        setupMapsButton();
    }

    private String getFormattedPlaceType(String type) {
        switch (type) {
            case "museum":
                return "Museum";
            case "tourist_attraction":
                return "Tourist Attraction";
            case "art_gallery":
                return "Art Gallery";
            case "church":
            case "hindu_temple":
            case "mosque":
            case "synagogue":
                return "Place of Worship";
            case "concert_hall":
            case "performing_arts_theater":
                return "Theater";
            case "night_club":
                return "Night Club";
            case "amusement_park":
            case "bowling_alley":
                return "Amusement Park";
            case "park":
                return "Park";
            case "beach":
                return "Beach";
            case "zoo":
                return "Zoo";
            case "aquarium":
                return "Aquarium";
            case "restaurant":
                return "Restaurant";
            case "cafe":
                return "Cafe";
            case "bar":
                return "Bar";
            case "shopping_mall":
                return "Shopping Mall";
            case "store":
                return "Store";
            default:
                return type != null ? type.replace("_", " ").toUpperCase() : null;
        }
    }

    private void setTypeDrawable(String type) {
        int drawableRes;
        switch (type) {
            case "museum":
                drawableRes = R.drawable.ic_museum;
                break;
            case "tourist_attraction":
                drawableRes = R.drawable.ic_tourist_attraction;
                break;
            case "art_gallery":
                drawableRes = R.drawable.ic_art_gallery;
                break;
            case "church":
            case "hindu_temple":
            case "mosque":
            case "synagogue":
                drawableRes = R.drawable.ic_place_of_worship;
                break;
            case "concert_hall":
            case "performing_arts_theater":
                drawableRes = R.drawable.ic_theater;
                break;
            case "night_club":
                drawableRes = R.drawable.ic_night_club;
                break;
            case "amusement_park":
            case "bowling_alley":
                drawableRes = R.drawable.ic_amusement_park;
                break;
            case "park":
                drawableRes = R.drawable.ic_park;
                break;
            case "beach":
                drawableRes = R.drawable.ic_beach;
                break;
            case "zoo":
                drawableRes = R.drawable.ic_zoo;
                break;
            case "aquarium":
                drawableRes = R.drawable.ic_aquarium;
                break;
            default:
                drawableRes = R.drawable.ic_default_place;
                break;
        }
        typeDrawable.setImageResource(drawableRes);
    }

    private void setPriceLevelOrWebsite() {
        String placeType = placeData.getPlaceType();

        if (isRestaurantCafeOrService(placeType)) {
            setPriceLevel();
            ticketDrawable.setImageResource(R.drawable.ic_money);
            priceLevelText.setText("Price Level");
        } else {
            setWebsite();
            ticketDrawable.setImageResource(R.drawable.ic_website);
            priceLevelText.setText("Website");
        }
    }

    private boolean isRestaurantCafeOrService(String placeType) {
        if (placeType == null) return false;

        return placeType.equals("acai_shop") ||
                placeType.equals("afghani_restaurant") ||
                placeType.equals("african_restaurant") ||
                placeType.equals("american_restaurant") ||
                placeType.equals("asian_restaurant") ||
                placeType.equals("bagel_shop") ||
                placeType.equals("bakery") ||
                placeType.equals("bar") ||
                placeType.equals("bar_and_grill") ||
                placeType.equals("barbecue_restaurant") ||
                placeType.equals("brazilian_restaurant") ||
                placeType.equals("breakfast_restaurant") ||
                placeType.equals("brunch_restaurant") ||
                placeType.equals("buffet_restaurant") ||
                placeType.equals("cafe") ||
                placeType.equals("cafeteria") ||
                placeType.equals("candy_store") ||
                placeType.equals("cat_cafe") ||
                placeType.equals("chinese_restaurant") ||
                placeType.equals("chocolate_factory") ||
                placeType.equals("chocolate_shop") ||
                placeType.equals("coffee_shop") ||
                placeType.equals("confectionery") ||
                placeType.equals("deli") ||
                placeType.equals("dessert_restaurant") ||
                placeType.equals("dessert_shop") ||
                placeType.equals("diner") ||
                placeType.equals("dog_cafe") ||
                placeType.equals("donut_shop") ||
                placeType.equals("fast_food_restaurant") ||
                placeType.equals("fine_dining_restaurant") ||
                placeType.equals("food_court") ||
                placeType.equals("french_restaurant") ||
                placeType.equals("greek_restaurant") ||
                placeType.equals("hamburger_restaurant") ||
                placeType.equals("ice_cream_shop") ||
                placeType.equals("indian_restaurant") ||
                placeType.equals("indonesian_restaurant") ||
                placeType.equals("italian_restaurant") ||
                placeType.equals("japanese_restaurant") ||
                placeType.equals("juice_shop") ||
                placeType.equals("korean_restaurant") ||
                placeType.equals("lebanese_restaurant") ||
                placeType.equals("meal_delivery") ||
                placeType.equals("meal_takeaway") ||
                placeType.equals("mediterranean_restaurant") ||
                placeType.equals("mexican_restaurant") ||
                placeType.equals("middle_eastern_restaurant") ||
                placeType.equals("pizza_restaurant") ||
                placeType.equals("pub") ||
                placeType.equals("ramen_restaurant") ||
                placeType.equals("restaurant") ||
                placeType.equals("sandwich_shop") ||
                placeType.equals("seafood_restaurant") ||
                placeType.equals("spanish_restaurant") ||
                placeType.equals("steak_house") ||
                placeType.equals("sushi_restaurant") ||
                placeType.equals("tea_house") ||
                placeType.equals("thai_restaurant") ||
                placeType.equals("turkish_restaurant") ||
                placeType.equals("vegan_restaurant") ||
                placeType.equals("vegetarian_restaurant") ||
                placeType.equals("vietnamese_restaurant") ||
                placeType.equals("wine_bar") ||
                placeType.contains("service") ||
                placeType.contains("store") ||
                placeType.contains("shop");
    }

    private void setPriceLevel() {
        try {
            int priceLevel = placeData.getPriceLevel();
            String priceText;
            int color;

            switch (priceLevel) {
                case 0:
                    priceText = "Free";
                    color = Color.GRAY;
                    break;
                case 1:
                    priceText = "$";
                    color = Color.parseColor("#4CAF50");
                    break;
                case 2:
                    priceText = "$$";
                    color = Color.parseColor("#FFEB3B");
                    break;
                case 3:
                    priceText = "$$$";
                    color = Color.parseColor("#FF9800");
                    break;
                case 4:
                    priceText = "$$$$";
                    color = Color.parseColor("#F44336");
                    break;
                default:
                    priceText = "N/A";
                    color = Color.GRAY;
                    break;
            }

            priceLevelValue.setText(priceText);
            priceLevelValue.setTextColor(color);
            priceLevelValue.setOnClickListener(null);
            priceLevelValue.setClickable(false);
        } catch (Exception e) {
            priceLevelValue.setText("N/A");
            priceLevelValue.setTextColor(Color.GRAY);
            priceLevelValue.setOnClickListener(null);
            priceLevelValue.setClickable(false);
        }
    }

    private void setWebsite() {
        try {
            String website = placeData.getWebsite();
            if (website != null && !website.trim().isEmpty()) {
                priceLevelValue.setText("Visit Website");
                priceLevelValue.setClickable(true);

                priceLevelValue.setOnClickListener(v -> {
                    try {
                        String url = website;
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Log.e("PlaceDetailFragment", "Error opening website: " + e.getMessage());
                    }
                });

                priceLevelValue.setTextColor(ContextCompat.getColor(requireContext(),
                        android.R.color.holo_blue_dark));
            } else {
                priceLevelValue.setText("No Website");
                priceLevelValue.setOnClickListener(null);
                priceLevelValue.setClickable(false);
                priceLevelValue.setTextColor(ContextCompat.getColor(requireContext(),
                        android.R.color.black));
            }
        } catch (Exception e) {
            priceLevelValue.setText("No Website");
            priceLevelValue.setOnClickListener(null);
            priceLevelValue.setClickable(false);
        }
    }

    private void setOpeningHours() {
        try {
            String rawHours = placeData.getOpeningHours();
            if (rawHours != null && !rawHours.trim().isEmpty()) {
                String todayStatus = parseTodayOpeningHours(rawHours);
                if (todayStatus != null) {
                    openingHoursValue.setText(todayStatus);
                } else {
                    String formattedHours = rawHours.replace(";", "\n");
                    openingHoursValue.setText(formattedHours);
                }
            } else {
                openingHoursValue.setText("Opening hours not available");
            }
        } catch (Exception e) {
            openingHoursValue.setText("Opening hours not available");
        }
    }

    private String parseTodayOpeningHours(String rawHours) {
        try {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            String today = dayFormat.format(new Date());

            String[] hourLines = rawHours.split("[;\n]");

            for (String line : hourLines) {
                line = line.trim();
                if (line.toLowerCase().startsWith(today.toLowerCase())) {
                    return formatTodayHours(line, today);
                }
            }

            String todayAbbrev = today.substring(0, 3);
            for (String line : hourLines) {
                line = line.trim();
                if (line.toLowerCase().startsWith(todayAbbrev.toLowerCase())) {
                    return formatTodayHours(line, today);
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String formatTodayHours(String todayLine, String dayName) {
        try {
            String hoursOnly = todayLine.replaceFirst("(?i)" + dayName, "").trim();
            hoursOnly = hoursOnly.replaceFirst("(?i)" + dayName.substring(0, 3), "").trim();
            hoursOnly = hoursOnly.replaceFirst("^[:\\-]\\s*", "").trim();

            if (hoursOnly.toLowerCase().contains("closed")) {
                return "Closed today";
            }

            Pattern timePattern = Pattern.compile("(\\d{1,2}:?\\d{0,2}\\s*(?:AM|PM|am|pm))\\s*[-–]\\s*(\\d{1,2}:?\\d{0,2}\\s*(?:AM|PM|am|pm))");
            Matcher matcher = timePattern.matcher(hoursOnly);

            if (matcher.find()) {
                String closingTime = matcher.group(2);
                return "Open today: closes " + closingTime.toLowerCase();
            }

            Pattern time24Pattern = Pattern.compile("(\\d{1,2}:?\\d{2})\\s*[-–]\\s*(\\d{1,2}:?\\d{2})");
            Matcher matcher24 = time24Pattern.matcher(hoursOnly);

            if (matcher24.find()) {
                String closingTime24 = matcher24.group(2);
                String closingTime12 = convertTo12Hour(closingTime24);
                return "Open today: closes " + closingTime12;
            }

            return "Today: " + hoursOnly;

        } catch (Exception e) {
            return null;
        }
    }

    private String convertTo12Hour(String time24) {
        try {
            SimpleDateFormat format24 = new SimpleDateFormat("HH:mm");
            SimpleDateFormat format12 = new SimpleDateFormat("h:mm a");
            Date date = format24.parse(time24);
            return format12.format(date).toLowerCase();
        } catch (Exception e) {
            return time24;
        }
    }

    private void loadPlaceImage() {
        if (placeData.getPhotoReferences() != null && !placeData.getPhotoReferences().isEmpty()) {
            String photoReference = placeData.getPhotoReferences().get(0);
            Log.d("PlaceDetailFragment", "Photo reference: " + photoReference);

            String photoUrl;
            if (photoReference.startsWith("http")) {
                photoUrl = photoReference;
            } else {
                photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                        "?maxwidth=400" +
                        "&photo_reference=" + photoReference +
                        "&key=" + apiKey;
            }

            Log.d("PlaceDetailFragment", "Final photo URL: " + photoUrl);

            Glide.with(this)
                    .load(photoUrl)
                    .transform(new CenterCrop(), new RoundedCorners(24))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(placeImage);
        } else {
            Log.d("PlaceDetailFragment", "No photo references available for: " + placeData.getName());
            placeImage.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void generateAboutSection() {
        String placeKey = placeData.getName() + "_" + placeData.getPlaceType();

        // Check if we already have a cached description
        if (aboutCache.containsKey(placeKey)) {
            aboutText.setText(aboutCache.get(placeKey));
            return;
        }

        // Show loading text while generating
        aboutText.setText("Generating description...");

        // Generate description asynchronously
        generateDescription(placeData, new DescriptionCallback() {
            @Override
            public void onSuccess(String description) {
                // Cache the result
                aboutCache.put(placeKey, description);

                // Update UI on main thread
                if (aboutText != null) {
                    aboutText.post(() -> {
                        aboutText.setText(description);
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e("PlaceDetailFragment", "Error generating description: " + error);
                if (aboutText != null) {
                    aboutText.post(() -> {
                        aboutText.setText(getDefaultDescription(placeData));
                    });
                }
            }
        });
    }

    private void generateDescription(PlaceData place, DescriptionCallback callback) {
        // Create prompt for Cohere
        String prompt = createPrompt(place);

        // Create JSON request body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "command-light");
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", 150);
            requestBody.put("temperature", 0.7);
            requestBody.put("k", 0);
            requestBody.put("stop_sequences", new JSONArray());
            requestBody.put("return_likelihoods", "NONE");
        } catch (Exception e) {
            callback.onError("Error creating request: " + e.getMessage());
            return;
        }

        // Create HTTP request
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(COHERE_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + coherekey)
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute request asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (jsonResponse.has("generations")) {
                            JSONArray generations = jsonResponse.getJSONArray("generations");
                            if (generations.length() > 0) {
                                String generatedText = generations.getJSONObject(0)
                                        .getString("text")
                                        .trim();
                                callback.onSuccess(generatedText);
                            } else {
                                callback.onError("No generations in response");
                            }
                        } else {
                            callback.onError("Invalid response format");
                        }
                    } else {
                        callback.onError("API request failed: " + response.code());
                    }
                } catch (Exception e) {
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    private String createPrompt(PlaceData place) {
        String placeType = getFormattedPlaceType(place.getPlaceType());
        String rating = place.getRating() > 0 ? String.format("%.1f", place.getRating()) : "not rated";

        StringBuilder prompt = new StringBuilder();
        prompt.append("Write a brief, engaging description for a travel destination called \"")
                .append(place.getName())
                .append("\". ");

        if (placeType != null) {
            prompt.append("It's a ").append(placeType.toLowerCase()).append(". ");
        }

        prompt.append("It has a rating of ").append(rating).append(" stars. ");

        prompt.append("Write 2-3 sentences that would interest a traveler, focusing on what makes this place special and worth visiting. ");
        prompt.append("Keep it informative but exciting, like a travel guide description.\n\n");

        return prompt.toString();
    }

    private String getDefaultDescription(PlaceData place) {
        String placeType = getFormattedPlaceType(place.getPlaceType());
        if (placeType != null) {
            return place.getName() + " is a popular " + placeType.toLowerCase() +
                    " that offers visitors a unique experience. " +
                    "This destination is worth exploring for its distinctive character and local appeal.";
        } else {
            return place.getName() + " is an interesting destination that offers visitors a unique experience worth exploring.";
        }
    }

    private void setupMapsButton() {
        viewMapsButton.setOnClickListener(v -> {
            if (placeData.getLatLng() != null) {
                Uri gmmIntentUri = Uri.parse("geo:" + placeData.getLatLng().latitude +
                        "," + placeData.getLatLng().longitude + "?q=" + Uri.encode(placeData.getName()));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            }
        });
    }

    private void setupBackButton() {
        backButton.setOnClickListener(v -> {
            View fragmentContainer = requireActivity().findViewById(R.id.fragment_container);
            if (fragmentContainer != null) {
                fragmentContainer.setVisibility(View.GONE);
            }

            View activitiesLabel = requireActivity().findViewById(R.id.activitiesLabel);
            if (activitiesLabel != null && activitiesLabel.getParent() instanceof View) {
                View allPlacesSection = (View) activitiesLabel.getParent();
                allPlacesSection.setVisibility(View.VISIBLE);
            }

            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().onBackPressed();
            }
        });
    }
}
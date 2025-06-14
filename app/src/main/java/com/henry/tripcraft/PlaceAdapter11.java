package com.henry.tripcraft;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;

public class PlaceAdapter11 extends RecyclerView.Adapter<PlaceAdapter11.PlaceViewHolder> {

    private List<PlaceData> placesList;
    private String apiKey;
    private String cohereApiKey;
    private OkHttpClient httpClient;
    private Map<String, String> aboutCache; // Cache for generated descriptions

    private static final String COHERE_API_URL = "https://api.cohere.ai/v1/generate";

    public PlaceAdapter11(List<PlaceData> placesList, String apiKey, String cohereApiKey) {
        this.placesList = placesList;
        this.apiKey = apiKey;
        this.cohereApiKey = cohereApiKey;
        this.httpClient = new OkHttpClient();
        this.aboutCache = new HashMap<>();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place11, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        PlaceData place = placesList.get(position);

        // Set place name
        holder.placeName.setText(place.getName());

        // Set place type in the main card
        String formattedType = getFormattedPlaceType(place.getPlaceType());
        if (formattedType != null) {
            holder.placeType.setText(formattedType);
            holder.placeType.setVisibility(View.VISIBLE);
            // Also set it in the info squares section
            holder.placeTypeValue.setText(formattedType);
        } else {
            holder.placeType.setVisibility(View.GONE);
            holder.placeTypeValue.setText("N/A");
        }

        // Set type drawable based on place type
        setTypeDrawable(holder, place.getPlaceType());

        // Set rating
        float rating = place.getRating();
        if (rating > 0) {
            holder.placeRating.setText(String.format("%.1f", rating));
            holder.placeRating.setVisibility(View.VISIBLE);
        } else {
            holder.placeRating.setVisibility(View.GONE);
        }

        // Set price level OR website based on place type
        setPriceLevelOrWebsite(holder, place);

        // Set opening hours (if available in PlaceData)
        setOpeningHours(holder, place);

        // Load the main image
        loadPlaceImage(holder, place);

        // Generate and set about section
        generateAboutSection(holder, place);

        // Set click listener for "View on Maps" button only
        holder.viewMapsButton.setOnClickListener(v -> {
            if (place.getLatLng() != null) {
                Uri gmmIntentUri = Uri.parse("geo:" + place.getLatLng().latitude +
                        "," + place.getLatLng().longitude + "?q=" + Uri.encode(place.getName()));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(holder.itemView.getContext().getPackageManager()) != null) {
                    holder.itemView.getContext().startActivity(mapIntent);
                }
            }
        });

        // Remove the main card click listener - no navigation on image/card click
        holder.itemView.setOnClickListener(null);

        // Highlight selected items (optional - you can remove this if not needed)
        if (place.isUserSelected()) {
            holder.itemView.setAlpha(0.8f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }
    }

    private void generateAboutSection(PlaceViewHolder holder, PlaceData place) {
        String placeKey = place.getName() + "_" + place.getPlaceType();

        // Check if we already have a cached description
        if (aboutCache.containsKey(placeKey)) {
            holder.aboutText.setText(aboutCache.get(placeKey));
            return;
        }

        // Show loading text while generating
        holder.aboutText.setText("Generating description...");

        // Generate description asynchronously
        generateDescription(place, new DescriptionCallback() {
            @Override
            public void onSuccess(String description) {
                // Cache the result
                aboutCache.put(placeKey, description);

                // Update UI on main thread
                holder.aboutText.post(() -> {
                    holder.aboutText.setText(description);
                });
            }

            @Override
            public void onError(String error) {
                Log.e("PlaceAdapter", "Error generating description: " + error);
                holder.aboutText.post(() -> {
                    holder.aboutText.setText(getDefaultDescription(place));
                });
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
                .addHeader("Authorization", "Bearer " + cohereApiKey)
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

    // Interface for async callback
    private interface DescriptionCallback {
        void onSuccess(String description);
        void onError(String error);
    }

    private void setTypeDrawable(PlaceViewHolder holder, String type) {
        int drawableRes;
        switch (type) {
            // Cultural & tourist attractions
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
            // Nature & outdoor
            case "park":
                drawableRes = R.drawable.ic_park;
                break;
            case "beach":
                drawableRes = R.drawable.ic_beach;
                break;
            // Family-friendly
            case "zoo":
                drawableRes = R.drawable.ic_zoo;
                break;
            case "aquarium":
                drawableRes = R.drawable.ic_aquarium;
                break;
            // Default for unknown types
            default:
                drawableRes = R.drawable.ic_default_place;
                break;
        }

        holder.typeDrawable.setImageResource(drawableRes);
    }

    private void setPriceLevelOrWebsite(PlaceViewHolder holder, PlaceData place) {
        String placeType = place.getPlaceType();

        // Check if it's a restaurant, cafe, or service type
        if (isRestaurantCafeOrService(placeType)) {
            // Show price level for restaurants, cafes, and services
            setPriceLevel(holder, place);
            holder.ticketDrawable.setImageResource(R.drawable.ic_money);
            holder.priceLevelText.setText("Price Level");
        } else {
            // Show website for other types (attractions, museums, etc.)
            setWebsite(holder, place);
            holder.ticketDrawable.setImageResource(R.drawable.ic_website);
            holder.priceLevelText.setText("Website");
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

    private void setPriceLevel(PlaceViewHolder holder, PlaceData place) {
        try {
            int priceLevel = place.getPriceLevel();
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

            holder.priceLevelValue.setText(priceText);
            holder.priceLevelValue.setTextColor(color);
            holder.priceLevelValue.setOnClickListener(null);
            holder.priceLevelValue.setClickable(false);
        } catch (Exception e) {
            holder.priceLevelValue.setText("N/A");
            holder.priceLevelValue.setTextColor(Color.GRAY);
            holder.priceLevelValue.setOnClickListener(null);
            holder.priceLevelValue.setClickable(false);
        }
    }

    private void setWebsite(PlaceViewHolder holder, PlaceData place) {
        try {
            String website = place.getWebsite();
            if (website != null && !website.trim().isEmpty()) {
                holder.priceLevelValue.setText("Visit Website");
                holder.priceLevelValue.setClickable(true);

                holder.priceLevelValue.setOnClickListener(v -> {
                    try {
                        String url = website;
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        holder.itemView.getContext().startActivity(browserIntent);
                    } catch (Exception e) {
                        Log.e("PlaceAdapter", "Error opening website: " + e.getMessage());
                    }
                });

                holder.priceLevelValue.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                        android.R.color.holo_blue_dark));
            } else {
                holder.priceLevelValue.setText("No Website");
                holder.priceLevelValue.setOnClickListener(null);
                holder.priceLevelValue.setClickable(false);
                holder.priceLevelValue.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                        android.R.color.black));
            }
        } catch (Exception e) {
            holder.priceLevelValue.setText("No Website");
            holder.priceLevelValue.setOnClickListener(null);
            holder.priceLevelValue.setClickable(false);
        }
    }

    private void setOpeningHours(PlaceViewHolder holder, PlaceData place) {
        try {
            String rawHours = place.getOpeningHours();
            if (rawHours != null && !rawHours.trim().isEmpty()) {
                String todayStatus = parseTodayOpeningHours(rawHours);
                if (todayStatus != null) {
                    holder.openingHoursValue.setText(todayStatus);
                } else {
                    String formattedHours = rawHours.replace(";", "\n");
                    holder.openingHoursValue.setText(formattedHours);
                }
            } else {
                holder.openingHoursValue.setText("Opening hours not available");
            }
        } catch (Exception e) {
            holder.openingHoursValue.setText("Opening hours not available");
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

    private void loadPlaceImage(PlaceViewHolder holder, PlaceData place) {
        if (place.getPhotoReferences() != null && !place.getPhotoReferences().isEmpty()) {
            String photoReference = place.getPhotoReferences().get(0);
            Log.d("PlaceAdapter", "Photo reference: " + photoReference);

            String photoUrl;
            if (photoReference.startsWith("http")) {
                photoUrl = photoReference;
            } else {
                photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                        "?maxwidth=400" +
                        "&photo_reference=" + photoReference +
                        "&key=" + apiKey;
            }

            Log.d("PlaceAdapter", "Final photo URL: " + photoUrl);

            Glide.with(holder.itemView.getContext())
                    .load(photoUrl)
                    .transform(new CenterCrop(), new RoundedCorners(24))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.placeImage);
        } else {
            Log.d("PlaceAdapter", "No photo references available for: " + place.getName());
            holder.placeImage.setImageResource(R.drawable.placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return placesList == null ? 0 : placesList.size();
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {
        ImageView placeImage;
        TextView placeName;
        TextView placeType;
        TextView placeRating;
        ImageView typeDrawable;
        TextView placeTypeValue;
        TextView priceLevelValue;
        TextView openingHoursValue;
        TextView priceLevelText;
        ImageView ticketDrawable;
        TextView aboutText;
        LinearLayout viewMapsButton;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            placeImage = itemView.findViewById(R.id.placeImage);
            placeName = itemView.findViewById(R.id.placeName);
            placeType = itemView.findViewById(R.id.placeType);
            placeRating = itemView.findViewById(R.id.placeRating);
            typeDrawable = itemView.findViewById(R.id.typeDrawable);
            placeTypeValue = itemView.findViewById(R.id.placeTypeValue);
            priceLevelValue = itemView.findViewById(R.id.priceLevelValue);
            openingHoursValue = itemView.findViewById(R.id.openingHoursValue);
            priceLevelText = itemView.findViewById(R.id.priceLevelText);
            ticketDrawable = itemView.findViewById(R.id.ticketDrawable);
            aboutText = itemView.findViewById(R.id.aboutText);
            viewMapsButton = itemView.findViewById(R.id.viewMapsButton);
        }
    }

    public void updatePlaces(List<PlaceData> newPlaces) {
        this.placesList = newPlaces;
        notifyDataSetChanged();
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
}
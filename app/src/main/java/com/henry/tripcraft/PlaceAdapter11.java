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
import java.util.List;

public class PlaceAdapter11 extends RecyclerView.Adapter<PlaceAdapter11.PlaceViewHolder> {

    private List<PlaceData> placesList;
    private String apiKey;

    public PlaceAdapter11(List<PlaceData> placesList, String apiKey) {
        this.placesList = placesList;
        this.apiKey = apiKey;
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
                drawableRes = R.drawable.ic_default_place; // You might want to create a default icon
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
            // Keep the original icon for price level (you might want to set this to a price/dollar icon)
            holder.ticketDrawable.setImageResource(R.drawable.ic_money); // Assuming you have a price icon
            holder.priceLevelText.setText("Price Level");
        } else {
            // Show website for other types (attractions, museums, etc.)
            setWebsite(holder, place);
            // Change to website icon
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
                    color = Color.parseColor("#4CAF50"); // Green
                    break;
                case 2:
                    priceText = "$$";
                    color = Color.parseColor("#FFEB3B"); // Yellow
                    break;
                case 3:
                    priceText = "$$$";
                    color = Color.parseColor("#FF9800"); // Orange
                    break;
                case 4:
                    priceText = "$$$$";
                    color = Color.parseColor("#F44336"); // Red
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

                // Set click listener to open website
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

                // Optional: Change text color to indicate it's clickable
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
                String formattedHours = rawHours.replace(";", "\n");
                holder.openingHoursValue.setText(formattedHours);
            } else {
                holder.openingHoursValue.setText("Opening hours not available");
            }
        } catch (Exception e) {
            holder.openingHoursValue.setText("Opening hours not available");
        }
    }

    private void loadPlaceImage(PlaceViewHolder holder, PlaceData place) {
        if (place.getPhotoReferences() != null && !place.getPhotoReferences().isEmpty()) {
            // Use the first photo reference
            String photoReference = place.getPhotoReferences().get(0);
            Log.d("PlaceAdapter", "Photo reference: " + photoReference);

            // Check if it's already a full URL or just a photo reference
            String photoUrl;
            if (photoReference.startsWith("http")) {
                // It's already a full URL
                photoUrl = photoReference;
            } else {
                // It's a photo reference, construct the URL
                photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                        "?maxwidth=400" +
                        "&photo_reference=" + photoReference +
                        "&key=" + apiKey;
            }

            Log.d("PlaceAdapter", "Final photo URL: " + photoUrl);

            Glide.with(holder.itemView.getContext())
                    .load(photoUrl)
                    .transform(new CenterCrop(), new RoundedCorners(24)) // 12dp corner radius * 2 for density
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.placeImage);
        } else {
            Log.d("PlaceAdapter", "No photo references available for: " + place.getName());
            // Use placeholder if no image available
            holder.placeImage.setImageResource(R.drawable.placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return placesList == null ? 0 : placesList.size();
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {
        // Main card elements
        ImageView placeImage;
        TextView placeName;
        TextView placeType;
        TextView placeRating;

        // Type drawable
        ImageView typeDrawable;

        // Info squares elements
        TextView placeTypeValue;
        TextView priceLevelValue;
        TextView openingHoursValue;
        TextView priceLevelText; // Added this for the label
        ImageView ticketDrawable; // Added this for the icon

        // About section (if you want to populate it dynamically)
        TextView aboutText;

        // View Maps button
        LinearLayout viewMapsButton;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Main card elements
            placeImage = itemView.findViewById(R.id.placeImage);
            placeName = itemView.findViewById(R.id.placeName);
            placeType = itemView.findViewById(R.id.placeType);
            placeRating = itemView.findViewById(R.id.placeRating);

            // Type drawable
            typeDrawable = itemView.findViewById(R.id.typeDrawable);

            // Info squares elements
            placeTypeValue = itemView.findViewById(R.id.placeTypeValue);
            priceLevelValue = itemView.findViewById(R.id.priceLevelValue);
            openingHoursValue = itemView.findViewById(R.id.openingHoursValue);
            priceLevelText = itemView.findViewById(R.id.priceLevelText);
            ticketDrawable = itemView.findViewById(R.id.ticketDrawable);

            // About section
            aboutText = itemView.findViewById(R.id.aboutText);

            // View Maps button
            viewMapsButton = itemView.findViewById(R.id.viewMapsButton);
        }
    }

    public void updatePlaces(List<PlaceData> newPlaces) {
        this.placesList = newPlaces;
        notifyDataSetChanged();
    }

    /**
     * Maps raw place type strings to user-friendly display names.
     */
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
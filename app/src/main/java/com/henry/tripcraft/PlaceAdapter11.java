package com.henry.tripcraft;

import android.content.Intent;
import android.net.Uri;
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

        // Set rating
        float rating = place.getRating();
        if (rating > 0) {
            holder.placeRating.setText(String.format("%.1f", rating));
            holder.placeRating.setVisibility(View.VISIBLE);
        } else {
            holder.placeRating.setVisibility(View.GONE);
        }

        // Set price level (if available in PlaceData)
        setPriceLevel(holder, place);

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

    private void setPriceLevel(PlaceViewHolder holder, PlaceData place) {
        // Assuming PlaceData has a getPriceLevel() method that returns an integer (0-4)
        // If not available, you'll need to add this to your PlaceData class
        try {
            int priceLevel = place.getPriceLevel(); // You may need to add this method to PlaceData
            String priceText;
            switch (priceLevel) {
                case 0:
                    priceText = "Free";
                    break;
                case 1:
                    priceText = "$";
                    break;
                case 2:
                    priceText = "$$";
                    break;
                case 3:
                    priceText = "$$$";
                    break;
                case 4:
                    priceText = "$$$$";
                    break;
                default:
                    priceText = "N/A";
                    break;
            }
            holder.priceLevelValue.setText(priceText);
        } catch (Exception e) {
            // If price level is not available, show N/A
            holder.priceLevelValue.setText("N/A");
        }
    }

    private void setOpeningHours(PlaceViewHolder holder, PlaceData place) {
        // Assuming PlaceData has opening hours information
        // You may need to add methods like isOpenNow(), getCurrentOpeningHours(), etc.
        try {
            // This is a placeholder - implement based on your PlaceData structure
            String openingHours = place.getOpeningHours(); // You may need to add this method
            if (openingHours != null && !openingHours.isEmpty()) {
                holder.openingHoursValue.setText(openingHours);
            } else {
                holder.openingHoursValue.setText("N/A");
            }
        } catch (Exception e) {
            // If opening hours are not available
            holder.openingHoursValue.setText("N/A");
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

        // Info squares elements
        TextView placeTypeValue;
        TextView priceLevelValue;
        TextView openingHoursValue;

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

            // Info squares elements
            placeTypeValue = itemView.findViewById(R.id.placeTypeValue);
            priceLevelValue = itemView.findViewById(R.id.priceLevelValue);
            openingHoursValue = itemView.findViewById(R.id.openingHoursValue);

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
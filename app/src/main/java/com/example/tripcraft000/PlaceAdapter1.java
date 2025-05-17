package com.example.tripcraft000;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PlaceAdapter1 extends RecyclerView.Adapter<PlaceAdapter1.PlaceViewHolder> {

    private List<PlaceData> placesList;
    private String apiKey;

    public PlaceAdapter1(List<PlaceData> placesList, String apiKey) {
        this.placesList = placesList;
        this.apiKey = apiKey;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place1, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        PlaceData place = placesList.get(position);

        holder.placeName.setText(place.getName());
        holder.placeAddress.setText(place.getAddress());

        // Format and display type text only
        String formattedType = getFormattedPlaceType(place.getPlaceType());
        if (formattedType != null) {
            holder.placeType.setText(formattedType);
            holder.placeType.setVisibility(View.VISIBLE);
        } else {
            holder.placeType.setVisibility(View.GONE);
        }

        holder.placeRating.setRating(place.getRating());

        // Display number of ratings
        int count = place.getUserRatingsTotal();
        holder.placeRatingCount.setText("(" + count + ")");

        // Set up images recycler view
        PlaceImagesAdapter imagesAdapter = new PlaceImagesAdapter(place.getPhotoReferences(), apiKey);
        holder.placeImagesRecyclerView.setAdapter(imagesAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
        holder.placeImagesRecyclerView.setLayoutManager(layoutManager);

        // Show scroll indicator if there are multiple images
        if (place.getPhotoReferences() != null && place.getPhotoReferences().size() > 1) {
            holder.scrollIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.scrollIndicator.setVisibility(View.GONE);
        }

        // Set up "View on Maps" button
        holder.viewOnMapsButton.setOnClickListener(v -> {
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

        // Highlight selected items
        if (place.isUserSelected()) {
            holder.itemView.setBackgroundColor(0xFFFFFACD);
        } else {
            holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.background)
            );
        }
    }

    @Override
    public int getItemCount() {
        return placesList == null ? 0 : placesList.size();
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView placeName;
        TextView placeAddress;
        TextView placeType;
        RatingBar placeRating;
        TextView placeRatingCount;
        RecyclerView placeImagesRecyclerView;
        ImageView scrollIndicator;
        Button viewOnMapsButton;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.placeName);
            placeAddress = itemView.findViewById(R.id.placeAddress);
            placeType = itemView.findViewById(R.id.placeType);
            placeRating = itemView.findViewById(R.id.placeRating);
            placeRatingCount = itemView.findViewById(R.id.placeRatingCount);
            placeImagesRecyclerView = itemView.findViewById(R.id.placeImagesRecyclerView);
            scrollIndicator = itemView.findViewById(R.id.scroll_indicator);
            viewOnMapsButton = itemView.findViewById(R.id.viewOnMapsButton);
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

            case "restaurant":
                return "Restaurant";
            case "cafe":
            case "bakery":
                return "Cafe";
            case "bar":
                return "Bar";

            case "shopping_mall":
                return "Shopping Mall";
            case "concert_hall":
            case "performing_arts_theater":
                return "Theater";
            case "movie_theater":
                return "Cinema";
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

            default:
                return null;
        }
    }
}


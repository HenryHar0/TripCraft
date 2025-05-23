package com.henry.tripcraft;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import android.util.Log;
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

        // Set place name
        holder.placeName.setText(place.getName());

        // Set place type
        String formattedType = getFormattedPlaceType(place.getPlaceType());
        if (formattedType != null) {
            holder.placeType.setText(formattedType);
            holder.placeType.setVisibility(View.VISIBLE);
        } else {
            holder.placeType.setVisibility(View.GONE);
        }

        // Set rating
        float rating = place.getRating();
        if (rating > 0) {
            holder.placeRating.setText(String.format("%.1f", rating));
            holder.placeRating.setVisibility(View.VISIBLE);
        } else {
            holder.placeRating.setVisibility(View.GONE);
        }

        // Load the main image
        loadPlaceImage(holder, place);

        // Set click listener for the image to open detailed view
        holder.placeImage.setOnClickListener(v -> {
            if (holder.itemView.getContext() instanceof FragmentActivity) {
                FragmentActivity activity = (FragmentActivity) holder.itemView.getContext();

                PlaceDetailFragment detailFragment = PlaceDetailFragment.newInstance(place, apiKey);

                View allPlacesSection = activity.findViewById(R.id.activitiesLabel).getParent() instanceof View ?
                        (View) activity.findViewById(R.id.activitiesLabel).getParent() : null;
                if (allPlacesSection != null) {
                    allPlacesSection.setVisibility(View.GONE);
                }

                View fragmentContainer = activity.findViewById(R.id.fragment_container);
                if (fragmentContainer != null) {
                    fragmentContainer.setVisibility(View.VISIBLE);
                    fragmentContainer.bringToFront();
                }

                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });


        // Set click listener for the entire card to open in Maps (optional - you can remove this)
        holder.itemView.setOnClickListener(v -> {
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

        // Highlight selected items (optional - you can remove this if not needed)
        if (place.isUserSelected()) {
            holder.itemView.setAlpha(0.8f);
        } else {
            holder.itemView.setAlpha(1.0f);
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
        ImageView placeImage;
        TextView placeName;
        TextView placeType;
        TextView placeRating;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            placeImage = itemView.findViewById(R.id.placeImage);
            placeName = itemView.findViewById(R.id.placeName);
            placeType = itemView.findViewById(R.id.placeType);
            placeRating = itemView.findViewById(R.id.placeRating);
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
            default:
                return null;
        }
    }
}
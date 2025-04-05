package com.example.tripcraft000;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {

    private final List<MapPlacesActivity.PlaceMarker> places;
    private final OnPlaceClickListener listener;
    private final OnPlaceSelectionChangedListener selectionListener;

    public interface OnPlaceClickListener {
        void onPlaceClick(MapPlacesActivity.PlaceMarker place);
    }

    public interface OnPlaceSelectionChangedListener {
        void onPlaceSelectionChanged(MapPlacesActivity.PlaceMarker place, boolean isSelected, boolean isMandatory);
    }

    public PlacesAdapter(List<MapPlacesActivity.PlaceMarker> places, OnPlaceClickListener listener,
                         OnPlaceSelectionChangedListener selectionListener) {
        this.places = places;
        this.listener = listener;
        this.selectionListener = selectionListener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        MapPlacesActivity.PlaceMarker place = places.get(position);
        holder.bind(place, listener, selectionListener);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    // Method to get selected places
    public List<MapPlacesActivity.PlaceMarker> getSelectedPlaces() {
        List<MapPlacesActivity.PlaceMarker> selectedPlaces = new ArrayList<>();
        for (MapPlacesActivity.PlaceMarker place : places) {
            if (place.isSelected()) {
                selectedPlaces.add(place);
            }
        }
        return selectedPlaces;
    }

    // Method to get mandatory places
    public List<MapPlacesActivity.PlaceMarker> getMandatoryPlaces() {
        List<MapPlacesActivity.PlaceMarker> mandatoryPlaces = new ArrayList<>();
        for (MapPlacesActivity.PlaceMarker place : places) {
            if (place.isSelected() && place.isMandatory()) {
                mandatoryPlaces.add(place);
            }
        }
        return mandatoryPlaces;
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView addressTextView;
        private final RatingBar ratingBar;
        private final RecyclerView imagesRecyclerView;
        private final CheckBox placeCheckbox;
        private final TextView mandatoryTag;
        private final Button viewOnMapsButton;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.placeName);
            addressTextView = itemView.findViewById(R.id.placeAddress);
            ratingBar = itemView.findViewById(R.id.placeRating);
            imagesRecyclerView = itemView.findViewById(R.id.placeImagesRecyclerView);
            placeCheckbox = itemView.findViewById(R.id.placeCheckbox);
            mandatoryTag = itemView.findViewById(R.id.mandatoryTag);
            viewOnMapsButton = itemView.findViewById(R.id.viewOnMapsButton);
        }

        public void bind(final MapPlacesActivity.PlaceMarker place,
                         final OnPlaceClickListener listener,
                         final OnPlaceSelectionChangedListener selectionListener) {
            nameTextView.setText(place.getName());
            addressTextView.setText(place.getVicinity());

            // Set checkbox state
            placeCheckbox.setChecked(place.isSelected());

            // Set mandatory tag visibility
            mandatoryTag.setVisibility(place.isSelected() && place.isMandatory() ? View.VISIBLE : View.GONE);

            if (place.getRating() > 0) {
                ratingBar.setVisibility(View.VISIBLE);
                ratingBar.setRating(place.getRating());
            } else {
                ratingBar.setVisibility(View.GONE);
            }

            // Setup horizontal image gallery
            List<String> photoUrls = place.getPhotoUrls();
            if (photoUrls != null && !photoUrls.isEmpty()) {
                imagesRecyclerView.setVisibility(View.VISIBLE);
                LinearLayoutManager layoutManager = new LinearLayoutManager(
                        itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
                imagesRecyclerView.setLayoutManager(layoutManager);
                ImageAdapter imageAdapter = new ImageAdapter(itemView.getContext(), photoUrls);
                imagesRecyclerView.setAdapter(imageAdapter);
            } else {
                imagesRecyclerView.setVisibility(View.GONE);
            }

            // Set click listener for the checkbox
            placeCheckbox.setOnClickListener(v -> {
                boolean isSelected = placeCheckbox.isChecked();
                place.setSelected(isSelected);

                // If unselected, then it can't be mandatory
                if (!isSelected) {
                    place.setMandatory(false);
                    mandatoryTag.setVisibility(View.GONE);
                }

                if (selectionListener != null) {
                    selectionListener.onPlaceSelectionChanged(place, isSelected, place.isMandatory());
                }
            });

            // Long press to mark as mandatory
            itemView.setOnLongClickListener(v -> {
                if (place.isSelected()) {
                    boolean isMandatory = !place.isMandatory();
                    place.setMandatory(isMandatory);
                    mandatoryTag.setVisibility(isMandatory ? View.VISIBLE : View.GONE);

                    if (selectionListener != null) {
                        selectionListener.onPlaceSelectionChanged(place, true, isMandatory);
                    }
                    return true;
                }
                return false;
            });

            // Set click listener for the whole item (except checkbox)
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });

            // Set click listener for Maps button
            viewOnMapsButton.setOnClickListener(v -> {
                Context context = itemView.getContext();
                Uri gmmIntentUri = Uri.parse("geo:" + place.getLatitude() + "," + place.getLongitude() +
                        "?q=" + Uri.encode(place.getName()));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(mapIntent);
                }
            });
        }
    }

    // Nested adapter for the horizontal image gallery
    static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        private final Context context;
        private final List<String> imageUrls;

        ImageAdapter(Context context, List<String> imageUrls) {
            this.context = context;
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_place_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String imageUrl = imageUrls.get(position);
            Glide.with(context)
                    .load(imageUrl)
                    .apply(new RequestOptions()
                            .centerCrop()
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image))
                    .into(holder.imageView);

            // Add click listener to open full-screen image view
            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullscreenImageActivity.class);
                intent.putExtra("IMAGE_URL", imageUrl);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;

            ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.placeImageItem);
            }
        }
    }
}
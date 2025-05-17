package com.example.tripcraft000;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> implements Filterable {

    private final List<MapPlacesActivity.PlaceMarker> places;
    private final List<MapPlacesActivity.PlaceMarker> placesFull;
    private final OnPlaceClickListener listener;
    private final OnPlaceSelectionChangedListener selectionListener;

    public interface OnPlaceClickListener {
        void onPlaceClick(MapPlacesActivity.PlaceMarker place);
    }

    public interface OnPlaceSelectionChangedListener {
        void onPlaceSelectionChanged(MapPlacesActivity.PlaceMarker place, boolean isSelected);
    }

    public PlacesAdapter(List<MapPlacesActivity.PlaceMarker> places,
                         OnPlaceClickListener listener,
                         OnPlaceSelectionChangedListener selectionListener) {
        this.places = new ArrayList<>(places);
        this.placesFull = new ArrayList<>(places);
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

    @Override
    public Filter getFilter() {
        return placeFilter;
    }

    private final Filter placeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<MapPlacesActivity.PlaceMarker> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(placesFull);
            } else {
                String[] keywords = constraint.toString().toLowerCase().trim().split("\\s+");
                for (MapPlacesActivity.PlaceMarker place : placesFull) {
                    String name = place.getName().toLowerCase();
                    String vicinity = place.getVicinity().toLowerCase();
                    boolean matches = true;
                    for (String kw : keywords) {
                        if (!name.contains(kw) && !vicinity.contains(kw)) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        filteredList.add(place);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            places.clear();
            //noinspection unchecked
            places.addAll((List<MapPlacesActivity.PlaceMarker>) results.values);
            notifyDataSetChanged();
        }
    };

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

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView addressTextView;
        private final RatingBar ratingBar;
        private final RecyclerView imagesRecyclerView;
        private final CheckBox placeCheckbox;
        private final Button viewOnMapsButton;
        private final ImageView scrollIndicator;


        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.placeName);
            addressTextView = itemView.findViewById(R.id.placeAddress);
            ratingBar = itemView.findViewById(R.id.placeRating);
            imagesRecyclerView = itemView.findViewById(R.id.placeImagesRecyclerView);
            placeCheckbox = itemView.findViewById(R.id.placeCheckbox);
            viewOnMapsButton = itemView.findViewById(R.id.viewOnMapsButton);
            scrollIndicator = itemView.findViewById(R.id.scroll_indicator);

        }

        public void bind(final MapPlacesActivity.PlaceMarker place,
                         final OnPlaceClickListener listener,
                         final OnPlaceSelectionChangedListener selectionListener) {
            nameTextView.setText(place.getName());
            addressTextView.setText(place.getVicinity());

            // Set checkbox state
            placeCheckbox.setChecked(place.isSelected());

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

                // Create and set horizontal layout manager with a page-like scrolling behavior
                LinearLayoutManager layoutManager = new LinearLayoutManager(
                        itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
                imagesRecyclerView.setLayoutManager(layoutManager);

                // Apply spacing between items
                int spacingInPixels = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.image_spacing);
                imagesRecyclerView.addItemDecoration(new ItemSpacingDecoration(spacingInPixels));

                // Add snap helper for better scrolling experience
                SnapHelper snapHelper = new PagerSnapHelper();
                if (imagesRecyclerView.getOnFlingListener() == null) {
                    snapHelper.attachToRecyclerView(imagesRecyclerView);
                }

                ImageAdapter imageAdapter = new ImageAdapter(itemView.getContext(), photoUrls);
                imagesRecyclerView.setAdapter(imageAdapter);

                // Show scroll indicator if there are multiple images
                if (photoUrls.size() > 1 && scrollIndicator != null) {
                    scrollIndicator.setVisibility(View.VISIBLE);
                } else if (scrollIndicator != null) {
                    scrollIndicator.setVisibility(View.GONE);
                }
            } else {
                imagesRecyclerView.setVisibility(View.GONE);
                if (scrollIndicator != null) {
                    scrollIndicator.setVisibility(View.GONE);
                }
            }

            // Set click listener for the checkbox
            placeCheckbox.setOnClickListener(v -> {
                boolean isSelected = placeCheckbox.isChecked();
                place.setSelected(isSelected);

                if (selectionListener != null) {
                    selectionListener.onPlaceSelectionChanged(place, isSelected);
                }
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

    // Item spacing decoration for RecyclerView
    public static class ItemSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public ItemSpacingDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.right = spacing;
            // Add top and bottom spacing for better visual appearance
            outRect.top = spacing / 2;
            outRect.bottom = spacing / 2;

            // Add left spacing only to the first item
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.left = spacing;
            }
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

            // Show image number for better indication of multiple images
            holder.imageCountText.setText((position + 1) + "/" + imageUrls.size());
            holder.imageCountText.setVisibility(imageUrls.size() > 1 ? View.VISIBLE : View.GONE);

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
            final TextView imageCountText;

            ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.placeImageItem);
                imageCountText = itemView.findViewById(R.id.imageCountText);
            }
        }
    }
}

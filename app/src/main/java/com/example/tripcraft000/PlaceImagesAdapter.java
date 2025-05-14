package com.example.tripcraft000;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PlaceImagesAdapter extends RecyclerView.Adapter<PlaceImagesAdapter.ImageViewHolder> {

    private List<String> photoReferences;
    private String apiKey;

    public PlaceImagesAdapter(List<String> photoReferences, String apiKey) {
        this.photoReferences = photoReferences;
        this.apiKey = apiKey;
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
        String photoUrl = photoReferences.get(position);

        Glide.with(holder.itemView.getContext())
                .load(photoUrl)
                .centerCrop()
                .into(holder.placeImageItem);

        holder.imageCountText.setText((position + 1) + "/" + photoReferences.size());
    }


    @Override
    public int getItemCount() {
        return photoReferences == null ? 0 : photoReferences.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView placeImageItem;
        TextView imageCountText;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            placeImageItem = itemView.findViewById(R.id.placeImageItem);
            imageCountText = itemView.findViewById(R.id.imageCountText);
        }
    }
}

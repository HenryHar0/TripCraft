package com.henry.tripcraft;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavedPlansAdapter extends RecyclerView.Adapter<SavedPlansAdapter.PlanViewHolder> {

    private final List<SavedPlansActivity.SavedPlan> plans;
    private final OnPlanClickListener listener;
    private final OnRenameClickListener renameListener;

    public interface OnPlanClickListener {
        void onPlanClick(int position);
    }

    public interface OnRenameClickListener {
        void onRenameClick(int position);
    }

    public SavedPlansAdapter(List<SavedPlansActivity.SavedPlan> plans,
                             OnPlanClickListener listener,
                             OnRenameClickListener renameListener) {
        this.plans = plans;
        this.listener = listener;
        this.renameListener = renameListener;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_plan_enhanced, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        SavedPlansActivity.SavedPlan plan = plans.get(position);

        holder.titleTextView.setText(plan.getTitle());

        // Show different content based on whether it's a new format or legacy format
        if (plan.getTripPlan() != null) {
            // New format - show enhanced details
            TripPlanStorageManager.TripPlan tripPlan = plan.getTripPlan();
            String enhancedDetails = createEnhancedDetails(tripPlan);
            holder.detailsTextView.setText(enhancedDetails);

            // Set appearance for new format plans
            holder.cardView.setAlpha(1.0f);
            holder.detailsTextView.setTextColor(Color.parseColor("#2E7D32")); // Green for new format
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E8")); // Light green background
        } else if (!plan.isEmpty()) {
            // Legacy format
            holder.detailsTextView.setText(plan.getDetails());
            holder.cardView.setAlpha(1.0f);
            holder.detailsTextView.setTextColor(Color.parseColor("#1976D2")); // Blue for legacy
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E3F2FD")); // Light blue background
        } else {
            // Empty slot
            holder.detailsTextView.setText(plan.getDetails());
            holder.cardView.setAlpha(0.7f);
            holder.detailsTextView.setTextColor(Color.parseColor("#757575")); // Gray for empty
            holder.cardView.setCardBackgroundColor(Color.parseColor("#F5F5F5")); // Light gray background
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlanClick(position);
            }
        });

        holder.renameButton.setOnClickListener(v -> {
            if (renameListener != null) {
                renameListener.onRenameClick(position);
            }
        });
    }

    private String createEnhancedDetails(TripPlanStorageManager.TripPlan tripPlan) {
        StringBuilder details = new StringBuilder();

        if (tripPlan.destination != null) {
            details.append("📍 ").append(tripPlan.destination).append("\n");
        }

        if (tripPlan.duration != null) {
            details.append("⏱️ ").append(tripPlan.duration).append("\n");
        }

        if (tripPlan.startDate != null && !tripPlan.startDate.isEmpty()) {
            details.append("📅 ").append(tripPlan.startDate).append("\n");
        }

        if (tripPlan.activitiesListData != null && !tripPlan.activitiesListData.isEmpty()) {
            int totalPlaces = 0;
            for (List<PlaceData> dayPlaces : tripPlan.activitiesListData) {
                if (dayPlaces != null) {
                    totalPlaces += dayPlaces.size();
                }
            }
            details.append("🗓️ ").append(tripPlan.activitiesListData.size()).append(" days");
            if (totalPlaces > 0) {
                details.append(" • ").append(totalPlaces).append(" places");
            }
        } else {
            details.append("📝 No activities planned");
        }

        return details.toString();
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView titleTextView;
        TextView detailsTextView;
        ImageButton renameButton;

        PlanViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.planCardView);
            titleTextView = itemView.findViewById(R.id.planTitleTextView);
            detailsTextView = itemView.findViewById(R.id.planDetailsTextView);
            renameButton = itemView.findViewById(R.id.renameButton);
        }
    }
}
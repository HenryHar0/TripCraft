package com.henry.tripcraft;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavedPlansAdapter extends RecyclerView.Adapter<SavedPlansAdapter.PlanViewHolder> {

    private final List<SavedPlansActivity.SavedPlan> plans;
    private final OnPlanClickListener listener;

    public interface OnPlanClickListener {
        void onPlanClick(int position);
    }

    public SavedPlansAdapter(List<SavedPlansActivity.SavedPlan> plans, OnPlanClickListener listener) {
        this.plans = plans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        SavedPlansActivity.SavedPlan plan = plans.get(position);

        holder.titleTextView.setText(plan.getTitle());
        holder.detailsTextView.setText(plan.getDetails());

        // Set different appearance for empty slots
        if (plan.isEmpty()) {
            holder.cardView.setAlpha(0.7f);
            holder.detailsTextView.setTextColor(0xFF888888);
        } else {
            holder.cardView.setAlpha(1.0f);
            holder.detailsTextView.setTextColor(0xFF333333);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlanClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView titleTextView;
        TextView detailsTextView;

        PlanViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.planCardView);
            titleTextView = itemView.findViewById(R.id.planTitleTextView);
            detailsTextView = itemView.findViewById(R.id.planDetailsTextView);
        }
    }
}
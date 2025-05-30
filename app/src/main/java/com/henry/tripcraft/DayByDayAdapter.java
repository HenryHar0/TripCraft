package com.henry.tripcraft;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DayByDayAdapter extends RecyclerView.Adapter<DayByDayAdapter.DayViewHolder> {
    private List<List<PlaceData>> schedule;
    private String apiKey;
    private SavedPlansActivity savedPlansActivity;
    private PlanActivity planActivity; // Add reference to PlanActivity

    // Constructor for SavedPlansActivity
    public DayByDayAdapter(List<List<PlaceData>> schedule, String apiKey, SavedPlansActivity savedPlansActivity) {
        this.schedule = schedule;
        this.apiKey = apiKey;
        this.savedPlansActivity = savedPlansActivity;
        this.planActivity = null;
    }

    // Constructor for PlanActivity
    public DayByDayAdapter(List<List<PlaceData>> schedule, String apiKey, PlanActivity planActivity) {
        this.schedule = schedule;
        this.apiKey = apiKey;
        this.savedPlansActivity = null;
        this.planActivity = planActivity;
    }

    // Keep the old constructor for backward compatibility with other activities
    public DayByDayAdapter(List<List<PlaceData>> schedule, String apiKey) {
        this.schedule = schedule;
        this.apiKey = apiKey;
        this.savedPlansActivity = null;
        this.planActivity = null;
    }

    public void updateSchedule(List<List<PlaceData>> newSchedule) {
        this.schedule = newSchedule;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_with_places, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        List<PlaceData> dayPlaces = schedule.get(position);
        holder.dayTitle.setText("Day " + (position + 1));

        // Create PlaceAdapter1 with callback to handle place clicks
        PlaceAdapter1 placeAdapter = new PlaceAdapter1(dayPlaces, apiKey, new PlaceAdapter1.OnPlaceClickListener() {
            @Override
            public void onPlaceClick(PlaceData placeData) {
                PlaceDetailFragment fragment = PlaceDetailFragment.newInstance(placeData, apiKey);

                if (savedPlansActivity != null) {
                    // Dismiss dialog first if we're in SavedPlansActivity
                    savedPlansActivity.dismissTripDialog();
                    savedPlansActivity.showFragment(fragment);
                } else if (planActivity != null) {
                    // Show fragment in PlanActivity
                    planActivity.showFragment(fragment);
                }
            }
        });

        holder.placesRecycler.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.placesRecycler.setAdapter(placeAdapter);
    }

    @Override
    public int getItemCount() {
        return schedule.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayTitle;
        RecyclerView placesRecycler;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTitle = itemView.findViewById(R.id.dayTitle);
            placesRecycler = itemView.findViewById(R.id.placesRecycler);
        }
    }
}
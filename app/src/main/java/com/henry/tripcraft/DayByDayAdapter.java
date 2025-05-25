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
    private SavedPlansActivity savedPlansActivity; // Add reference to SavedPlansActivity

    // Modified constructor to accept SavedPlansActivity
    public DayByDayAdapter(List<List<PlaceData>> schedule, String apiKey, SavedPlansActivity savedPlansActivity) {
        this.schedule = schedule;
        this.apiKey = apiKey;
        this.savedPlansActivity = savedPlansActivity;
    }

    // Keep the old constructor for backward compatibility with other activities
    public DayByDayAdapter(List<List<PlaceData>> schedule, String apiKey) {
        this.schedule = schedule;
        this.apiKey = apiKey;
        this.savedPlansActivity = null; // No dialog dismissal needed in other activities
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
                // Dismiss dialog first if we're in SavedPlansActivity
                if (savedPlansActivity != null) {
                    savedPlansActivity.dismissTripDialog();

                    // Show the place detail fragment (corrected line)
                    PlaceDetailFragment fragment = PlaceDetailFragment.newInstance(placeData, apiKey);
                    savedPlansActivity.showFragment(fragment);
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
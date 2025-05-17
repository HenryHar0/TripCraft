package com.example.tripcraft000;

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

    public DayByDayAdapter(List<List<PlaceData>> schedule, String apiKey) {
        this.schedule = schedule;
        this.apiKey = apiKey;
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

        PlaceAdapter1 placeAdapter = new PlaceAdapter1(dayPlaces, apiKey);
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


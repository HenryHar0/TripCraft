package com.henry.tripcraft;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DayAdapter1 extends RecyclerView.Adapter<DayAdapter1.DayViewHolder> {

    private final int totalDays;
    private String startDateStr;
    private final SimpleDateFormat displayDateFormat;
    private final SimpleDateFormat storageDateFormat;

    public DayAdapter1(int totalDays) {
        this.totalDays = totalDays;
        this.startDateStr = "2025-01-01";
        this.displayDateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        this.storageDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    public void setStartDate(String startDate) {
        this.startDateStr = startDate;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day1, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.dayNumber.setText(String.format(Locale.getDefault(), "Day %d", position + 1));
        String dayDate = getDateForDay(position);
        holder.dayDate.setText(dayDate);
    }

    private String getDateForDay(int dayIndex) {
        try {
            Date startDate = storageDateFormat.parse(startDateStr);
            if (startDate != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);
                calendar.add(Calendar.DAY_OF_MONTH, dayIndex);
                return displayDateFormat.format(calendar.getTime());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "Day " + (dayIndex + 1);
    }

    @Override
    public int getItemCount() {
        return totalDays;
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayNumber;
        TextView dayDate;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.dayNumber);
            dayDate = itemView.findViewById(R.id.dayDate);
        }
    }
}

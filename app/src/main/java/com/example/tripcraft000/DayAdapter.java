package com.example.tripcraft000;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private static final int MAX_HOURS_PER_DAY = 24;
    private static final int MIN_HOURS_PER_DAY = 0;

    private final int totalDays;
    private final HashMap<Integer, Integer> hoursPerDay;
    private final OnHoursChangedListener listener;
    private String startDateStr;
    private SimpleDateFormat displayDateFormat;
    private SimpleDateFormat storageDateFormat;

    public interface OnHoursChangedListener {
        void onHoursChanged(int dayIndex, int hours);
    }

    public DayAdapter(int totalDays, HashMap<Integer, Integer> hoursPerDay, OnHoursChangedListener listener) {
        this.totalDays = totalDays;
        this.hoursPerDay = hoursPerDay;
        this.listener = listener;

        // Initialize with default dates - will be set properly later
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
                .inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        // Set day number (1-indexed for user-friendliness)
        holder.dayNumber.setText(String.format(Locale.getDefault(), "Day %d", position + 1));

        // Set the date text for this day
        String dayDate = getDateForDay(position);
        holder.dayDate.setText(dayDate);

        // Get hours if available, default to 0
        Integer hours = hoursPerDay.getOrDefault(position, 0);

        // Set hours in input field
        holder.hoursInput.setText(String.valueOf(hours));

        // Setup hours validation and update
        setupHoursInput(holder, position);

        // Setup activity status text
        updateActivityStatus(holder, hours);

        // Setup increment and decrement buttons
        setupStepperButtons(holder, position);
    }

    private void updateActivityStatus(DayViewHolder holder, int hours) {
        if (hours > 0) {
            holder.activityStatus.setText(String.format(Locale.getDefault(),
                    "%d hours planned", hours));
            holder.activityStatus.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.secondary));
        } else {
            holder.activityStatus.setText("No activities planned yet");
            holder.activityStatus.setTextColor(holder.itemView.getContext()
                    .getColor(android.R.color.darker_gray));
        }
    }

    private void setupStepperButtons(DayViewHolder holder, final int position) {
        // Decrement button
        holder.decrementHours.setOnClickListener(v -> {
            Integer currentHours = hoursPerDay.getOrDefault(position, 0);
            if (currentHours > MIN_HOURS_PER_DAY) {
                int newHours = currentHours - 1;
                hoursPerDay.put(position, newHours);
                holder.hoursInput.setText(String.valueOf(newHours));
                updateActivityStatus(holder, newHours);

                if (listener != null) {
                    listener.onHoursChanged(position, newHours);
                }
            }
        });

        // Increment button
        holder.incrementHours.setOnClickListener(v -> {
            Integer currentHours = hoursPerDay.getOrDefault(position, 0);
            if (currentHours < MAX_HOURS_PER_DAY) {
                int newHours = currentHours + 1;
                hoursPerDay.put(position, newHours);
                holder.hoursInput.setText(String.valueOf(newHours));
                updateActivityStatus(holder, newHours);

                if (listener != null) {
                    listener.onHoursChanged(position, newHours);
                }
            } else {
                Toast.makeText(holder.itemView.getContext(),
                        "Maximum " + MAX_HOURS_PER_DAY + " hours allowed per day",
                        Toast.LENGTH_SHORT).show();
            }
        });
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
        return "Day " + (dayIndex + 1); // Fallback if date parsing fails
    }

    private void setupHoursInput(DayViewHolder holder, final int position) {
        holder.hoursInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateHoursForDay(holder, position);
            }
        });
    }

    private void updateHoursForDay(DayViewHolder holder, int position) {
        String input = holder.hoursInput.getText().toString().trim();

        if (input.isEmpty()) {
            hoursPerDay.remove(position);
            updateActivityStatus(holder, 0);
            return;
        }

        try {
            int hours = Integer.parseInt(input);

            if (hours < MIN_HOURS_PER_DAY || hours > MAX_HOURS_PER_DAY) {
                Toast.makeText(holder.itemView.getContext(),
                        "Hours must be between " + MIN_HOURS_PER_DAY +
                                " and " + MAX_HOURS_PER_DAY, Toast.LENGTH_SHORT).show();

                // Reset to previous valid value or empty
                Integer previousHours = hoursPerDay.getOrDefault(position, 0);
                holder.hoursInput.setText(String.valueOf(previousHours));
                return;
            }

            hoursPerDay.put(position, hours);
            updateActivityStatus(holder, hours);

            if (listener != null) {
                listener.onHoursChanged(position, hours);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(holder.itemView.getContext(),
                    "Please enter a valid number", Toast.LENGTH_SHORT).show();

            // Reset to previous valid value or zero
            Integer previousHours = hoursPerDay.getOrDefault(position, 0);
            holder.hoursInput.setText(String.valueOf(previousHours));
        }
    }

    @Override
    public int getItemCount() {
        return totalDays;
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayNumber;
        TextView dayDate;
        TextView activityStatus;
        TextInputEditText hoursInput;
        ImageButton incrementHours;
        ImageButton decrementHours;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.dayNumber);
            dayDate = itemView.findViewById(R.id.dayDate);
            activityStatus = itemView.findViewById(R.id.activityStatus);
            hoursInput = itemView.findViewById(R.id.hoursInput);
            incrementHours = itemView.findViewById(R.id.incrementHours);
            decrementHours = itemView.findViewById(R.id.decrementHours);
        }
    }
}
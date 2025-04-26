package com.example.tripcraft000;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashMap;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private final int totalDays;
    private final HashMap<Integer, Integer> hoursPerDay;
    private final OnHoursChangedListener listener;

    public interface OnHoursChangedListener {
        void onHoursChanged(int dayIndex, int hours);
    }

    public DayAdapter(int dayCount, HashMap<Integer, Integer> hoursMap, OnHoursChangedListener listener) {
        this.totalDays = dayCount;
        this.hoursPerDay = hoursMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return totalDays;
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayLabel;
        private final EditText hoursInput;
        private int currentDayIndex = -1;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayLabel = itemView.findViewById(R.id.dayLabel);
            hoursInput = itemView.findViewById(R.id.dayHoursInput);

            setupHoursChangeListener();
        }

        private void setupHoursChangeListener() {
            hoursInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (currentDayIndex >= 0 && !s.toString().isEmpty()) {
                        try {
                            int hours = Integer.parseInt(s.toString());
                            if (hours >= 0 && hours <= 24) {
                                listener.onHoursChanged(currentDayIndex, hours);
                            } else {
                                hoursInput.setError("Hours must be 0-24");
                            }
                        } catch (NumberFormatException e) {
                            hoursInput.setError("Invalid number");
                        }
                    }
                }
            });
        }

        public void bind(int position) {
            currentDayIndex = position;

            // Format date label (Day 1, Day 2, etc.)
            dayLabel.setText(String.format("Day %d", position + 1));

            // Get saved hours value
            Integer hours = hoursPerDay.get(position);

            // Set hours text (avoid triggering listener during initial binding)
            hoursInput.removeTextChangedListener(hoursInput.getTag() instanceof TextWatcher ?
                    (TextWatcher) hoursInput.getTag() : null);
            hoursInput.setText(hours != null ? String.valueOf(hours) : "");
            hoursInput.addTextChangedListener((TextWatcher) hoursInput.getTag());
        }
    }
}
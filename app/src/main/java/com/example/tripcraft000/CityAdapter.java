package com.example.tripcraft000;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CityAdapter extends ArrayAdapter<String> {
    private final List<String> items = new ArrayList<>();
    private final LayoutInflater inflater;

    public CityAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public void add(@Nullable String object) {
        if (object != null) {
            items.add(object);
            super.add(object);
        }
    }

    @Override
    public void clear() {
        items.clear();
        super.clear();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (view == null) {
            view = inflater.inflate(R.layout.item_city_dropdown, parent, false);
            holder = new ViewHolder();
            holder.text = view.findViewById(R.id.text_city_name);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        String cityInfo = getItem(position);

        if (cityInfo != null) {
            // Split city name and population
            int populationIndex = cityInfo.indexOf(" (Population:");
            if (populationIndex > 0) {
                String cityName = cityInfo.substring(0, populationIndex);
                String population = cityInfo.substring(populationIndex);

                // Set the city name and population in separate TextViews
                holder.text.setText(cityName);

                // You can set population to another TextView if you have one in your layout
                TextView populationText = view.findViewById(R.id.text_population);
                if (populationText != null) {
                    populationText.setText(population);
                }
            } else {
                holder.text.setText(cityInfo);
            }
        }

        return view;
    }

    private static class ViewHolder {
        TextView text;
    }
}
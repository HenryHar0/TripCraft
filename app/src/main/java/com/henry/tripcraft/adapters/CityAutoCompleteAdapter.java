package com.henry.tripcraft.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.henry.tripcraft.activities.CityActivity;
import com.henry.tripcraft.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CityAutoCompleteAdapter extends ArrayAdapter<CityActivity.City> {

    private final Context context;
    private final List<CityActivity.City> originalCities;
    private List<CityActivity.City> filteredCities;
    private final LayoutInflater inflater;
    private final NumberFormat numberFormat;

    // Flag caching
    private final Map<String, Drawable> flagCache = new HashMap<>();
    private static final String FLAGS_API_BASE_URL = "https://flagsapi.com/";

    public CityAutoCompleteAdapter(@NonNull Context context, @NonNull List<CityActivity.City> cities) {
        super(context, R.layout.item_city_dropdown, cities);
        this.context = context;
        this.originalCities = cities;
        this.filteredCities = new ArrayList<>(cities);
        this.inflater = LayoutInflater.from(context);
        this.numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
    }

    @Override
    public int getCount() {
        return filteredCities.size();
    }

    @Nullable
    @Override
    public CityActivity.City getItem(int position) {
        return filteredCities.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_city_dropdown, parent, false);
            holder = new ViewHolder();
            holder.flagIcon = convertView.findViewById(R.id.flag_icon);
            holder.cityName = convertView.findViewById(R.id.city_name);
            holder.cityDetails = convertView.findViewById(R.id.city_details);
            holder.populationText = convertView.findViewById(R.id.population_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CityActivity.City city = filteredCities.get(position);
        if (city != null) {
            holder.cityName.setText(city.getName());

            // Set city details (region and country)
            String details;
            if (city.getRegion() != null && !city.getRegion().isEmpty() && !city.getRegion().equals(city.getCountry())) {
                details = city.getRegion() + ", " + city.getCountry();
            } else {
                details = city.getCountry();
            }
            holder.cityDetails.setText(details);

            // Set population if available
            if (city.getPopulation() > 0) {
                String populationStr = "Pop: " + numberFormat.format(city.getPopulation());
                holder.populationText.setText(populationStr);
                holder.populationText.setVisibility(View.VISIBLE);
            } else {
                holder.populationText.setVisibility(View.GONE);
            }

            // Load country flag
            loadCountryFlag(holder.flagIcon, city.getCountryCode());
        }

        return convertView;
    }

    private void loadCountryFlag(ImageView flagImageView, String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            // Set placeholder if no country code
            flagImageView.setImageResource(R.drawable.ic_flag_placeholder);
            return;
        }

        // Check cache first
        if (flagCache.containsKey(countryCode)) {
            flagImageView.setImageDrawable(flagCache.get(countryCode));
            return;
        }

        // Build flag URL - using flat style with 64px size for better performance
        String flagUrl = FLAGS_API_BASE_URL + countryCode.toUpperCase() + "/flat/64.png";

        // Load flag using Glide
        Glide.with(context)
                .load(flagUrl)
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_flag_placeholder)
                        .error(R.drawable.ic_flag_placeholder)
                        .centerCrop()
                        .override(64, 64)) // Consistent size
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        // Cache the loaded flag
                        flagCache.put(countryCode, resource);
                        flagImageView.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        flagImageView.setImageDrawable(placeholder);
                    }
                });
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    results.values = originalCities;
                    results.count = originalCities.size();
                } else {
                    List<CityActivity.City> filtered = new ArrayList<>();
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (CityActivity.City city : originalCities) {
                        if (city.getName().toLowerCase().contains(filterPattern) ||
                                city.getCountry().toLowerCase().contains(filterPattern) ||
                                (city.getRegion() != null && city.getRegion().toLowerCase().contains(filterPattern))) {
                            filtered.add(city);
                        }
                    }

                    results.values = filtered;
                    results.count = filtered.size();
                }

                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredCities = (List<CityActivity.City>) results.values;
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    public void updateCities(List<CityActivity.City> newCities) {
        originalCities.clear();
        originalCities.addAll(newCities);
        filteredCities.clear();
        filteredCities.addAll(newCities);
        notifyDataSetChanged();
    }

    public void clearFlagCache() {
        flagCache.clear();
    }

    private static class ViewHolder {
        ImageView flagIcon;
        TextView cityName;
        TextView cityDetails;
        TextView populationText;
    }
}
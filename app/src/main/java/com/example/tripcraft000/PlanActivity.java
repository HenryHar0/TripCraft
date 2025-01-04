package com.example.tripcraft000;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class PlanActivity extends AppCompatActivity {

    private TextView planTitle, destinationLabel, destinationValue, durationLabel, durationValue, activitiesLabel, weatherInfo;
    private ListView activitiesList;

    private String startDate, endDate, city;
    private ArrayAdapter<String> activitiesAdapter;
    private ArrayList<String> activitiesListData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        planTitle = findViewById(R.id.planTitle);
        destinationLabel = findViewById(R.id.destinationLabel);
        destinationValue = findViewById(R.id.destinationValue);
        durationLabel = findViewById(R.id.durationLabel);
        durationValue = findViewById(R.id.durationValue);
        activitiesLabel = findViewById(R.id.activitiesLabel);
        activitiesList = findViewById(R.id.activitiesList);
        weatherInfo = findViewById(R.id.weatherInfo);

        Intent intent = getIntent();
        startDate = intent.getStringExtra("start_date");
        endDate = intent.getStringExtra("end_date");
        city = intent.getStringExtra("city");

        if (city != null) {
            destinationValue.setText(city);
        }

        if (startDate != null && endDate != null) {
            calculateDuration();
        }

        generateRandomActivities();

        if (city != null && startDate != null && endDate != null) {
            fetchNOAAWeatherData(city, startDate, endDate);
        }
    }

    private void calculateDuration() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            if (start != null && end != null) {
                long differenceInMillis = end.getTime() - start.getTime();
                long days = differenceInMillis / (1000 * 60 * 60 * 24);
                durationValue.setText(days + " days");
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error calculating duration", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateRandomActivities() {
        String[] activityPool = {
                "Visit local museum",
                "Go hiking",
                "Try traditional food",
                "Explore a local market",
                "Relax at a park",
                "Take a boat ride",
                "Attend a local event",
                "Visit a historic site",
                "Take a guided tour",
                "Try a cooking class"
        };

        activitiesListData = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 5; i++) {
            int index = random.nextInt(activityPool.length);
            activitiesListData.add(activityPool[index]);
        }

        activitiesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, activitiesListData);
        activitiesList.setAdapter(activitiesAdapter);
    }

    private void fetchNOAAWeatherData(String city, String startDate, String endDate) {
        String apiToken = "gPXPBClQgoFZLMdHrvERgpQNfESTqdEL";
        String apiUrl = "https://www.ncdc.noaa.gov/cdo-web/api/v2/data?datasetid=GHCND&startdate=" + startDate +
                "&enddate=" + endDate + "&locationid=CITY:" + city + "&datatypeid=TAVG&units=metric";

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("token", apiToken);  // Token in header instead of URL

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    return response.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        JSONArray results = jsonObject.getJSONArray("results");

                        if (results.length() > 0) {
                            double totalTemp = 0;
                            int count = 0;

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject record = results.getJSONObject(i);
                                totalTemp += record.getDouble("value");
                                count++;
                            }

                            double avgTemp = totalTemp / count;
                            weatherInfo.setText(String.format(Locale.getDefault(), "Avg Temp: %.1f°C", avgTemp));
                        } else {
                            weatherInfo.setText("No weather data available");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        weatherInfo.setText("Error parsing NOAA data");
                    }
                } else {
                    weatherInfo.setText("Error fetching NOAA data");
                }
            }
        }.execute();
    }

}

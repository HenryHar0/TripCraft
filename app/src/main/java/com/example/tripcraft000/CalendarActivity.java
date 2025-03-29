package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import androidx.cardview.widget.CardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private DatePicker datePicker;
    private MaterialButton nextButton;
    private TextView datePrompt, errorMessage, selectedDatesText;
    private CardView dateCard;

    private String startDate;
    private String city;
    private int geonameId;
    private double latitude;
    private double longitude;
    private SimpleDateFormat displayFormat;
    private SimpleDateFormat storageFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        datePicker = findViewById(R.id.date_picker);
        nextButton = findViewById(R.id.next_button);
        datePrompt = findViewById(R.id.date_prompt);
        errorMessage = findViewById(R.id.error_message);
        selectedDatesText = findViewById(R.id.selected_dates_text);
        dateCard = findViewById(R.id.date_card);

        Intent intent = getIntent();
        city = intent.getStringExtra("city");
        geonameId = intent.getIntExtra("geonameId", -1);

        // Add coordinate retrieval
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);

        Calendar calendar = Calendar.getInstance();
        int todayYear = calendar.get(Calendar.YEAR);
        int todayMonth = calendar.get(Calendar.MONTH);
        int todayDay = calendar.get(Calendar.DAY_OF_MONTH);

        datePicker.setMinDate(calendar.getTimeInMillis());
        datePicker.updateDate(todayYear, todayMonth, todayDay);

        datePrompt.setText("Choose your travel start date");
        selectedDatesText.setText("Selected period: Not selected yet");
        errorMessage.setVisibility(View.GONE);

        nextButton.setOnClickListener(v -> {
            if (startDate == null) {
                startDate = getSelectedDate(true);
                String formattedStartDate = formatDateForDisplay(startDate);

                datePrompt.setText("Choose your travel end date");
                selectedDatesText.setText("Selected period: " + formattedStartDate + " to ...");
                nextButton.setText("Continue");

                try {
                    Date start = storageFormat.parse(startDate);
                    if (start != null) {
                        Calendar minEndDate = Calendar.getInstance();
                        minEndDate.setTime(start);
                        datePicker.setMinDate(minEndDate.getTimeInMillis());
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }  else {
                String endDate = getSelectedDate(true);

                if (isEndDateEarlierThanStartDate(startDate, endDate)) {
                    errorMessage.setVisibility(View.VISIBLE);
                    errorMessage.setText("End date cannot be earlier than start date");
                } else {
                    errorMessage.setVisibility(View.GONE);

                    int durationDays = calculateDurationInDays(startDate, endDate);

                    Intent interestsIntent = new Intent(CalendarActivity.this, InterestsActivity.class);
                    interestsIntent.putExtra("start_date", startDate);
                    interestsIntent.putExtra("end_date", endDate);
                    interestsIntent.putExtra("duration_days", durationDays);
                    interestsIntent.putExtra("city", city);
                    interestsIntent.putExtra("geonameId", geonameId);

                    // Pass coordinates to next activity
                    interestsIntent.putExtra("latitude", latitude);
                    interestsIntent.putExtra("longitude", longitude);

                    startActivity(interestsIntent);
                }
            }
        });
    }

    private int calculateDurationInDays(String startDate, String endDate) {
        try {
            Date start = storageFormat.parse(startDate);
            Date end = storageFormat.parse(endDate);

            if (start != null && end != null) {
                long diffInMillis = Math.abs(end.getTime() - start.getTime());
                return (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getSelectedDate(boolean forStorage) {
        int year = datePicker.getYear();
        int month = datePicker.getMonth() + 1;
        int day = datePicker.getDayOfMonth();

        if (forStorage) {
            return String.format(Locale.US, "%d-%02d-%02d", year, month, day);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month - 1, day);
            return displayFormat.format(calendar.getTime());
        }
    }

    private String formatDateForDisplay(String storageFormatDate) {
        try {
            Date date = storageFormat.parse(storageFormatDate);
            if (date != null) {
                return displayFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return storageFormatDate;
    }

    private boolean isEndDateEarlierThanStartDate(String startDate, String endDate) {
        try {
            Date start = storageFormat.parse(startDate);
            Date end = storageFormat.parse(endDate);

            return end != null && start != null && end.before(start);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
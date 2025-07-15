package com.henry.tripcraft.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.henry.tripcraft.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    // Intent keys
    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";

    private static final int MAX_DURATION_DAYS = 30;

    private CalendarView calendarView;
    private MaterialButton nextButton;
    private MaterialButton decreaseDaysButton;
    private MaterialButton increaseDaysButton;
    private TextView arrivalDateText, departureDateText, daysCountText, daysCounterText;

    private String startDateStorage;
    private String endDateStorage;
    private int durationDays = 1;

    private ImageButton backButton;

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private String city;
    private double latitude;
    private double longitude;

    private final Calendar today = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        initViews();
        extractIntentData();
        ConfigureCalendar();

        // Calculate and set initial departure date
        updateDepartureDateFromDuration();

        setupDateChangeListener();
        setupDurationControls();
        setupNextButton();
        setupBackButton();
    }

    private void ConfigureCalendar(){
        // Set week to start with Monday
        calendarView.setFirstDayOfWeek(Calendar.MONDAY);

        calendarView.setMinDate(today.getTimeInMillis());
        calendarView.setDate(today.getTimeInMillis(), false, true);

        // Initialize with today's date
        Date todayDate = new Date(today.getTimeInMillis());
        startDateStorage = storageFormat.format(todayDate);
        arrivalDateText.setText(displayFormat.format(todayDate));
    }


    private void initViews() {
        calendarView = findViewById(R.id.date_picker);
        nextButton = findViewById(R.id.next_button);
        decreaseDaysButton = findViewById(R.id.decrease_days);
        increaseDaysButton = findViewById(R.id.increase_days);
        arrivalDateText = findViewById(R.id.arrival_date);
        departureDateText = findViewById(R.id.departure_date);
        daysCountText = findViewById(R.id.days_count);
        daysCounterText = findViewById(R.id.days_counter);
        backButton = findViewById(R.id.back_button);
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        city = intent.getStringExtra(EXTRA_CITY);
        latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0);
        longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0);
    }

    private void setupDateChangeListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String storageDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            String displayDate = displayFormat.format(cal.getTime());

            startDateStorage = storageDate;
            arrivalDateText.setText(displayDate);

            // Update departure date based on current duration
            updateDepartureDateFromDuration();
        });
    }



    private void setupDurationControls() {
        decreaseDaysButton.setOnClickListener(v -> {
            if (durationDays > 1) {
                durationDays--;
                updateDurationDisplay();
                updateDepartureDateFromDuration();
            }
        });

        increaseDaysButton.setOnClickListener(v -> {
            if (durationDays < MAX_DURATION_DAYS) {
                durationDays++;
                updateDurationDisplay();
                updateDepartureDateFromDuration();
            }
        });
    }

    private void updateDurationDisplay() {
        daysCounterText.setText(String.valueOf(durationDays));

        String durationText = getResources().getQuantityString(
                R.plurals.day_count,
                durationDays,
                durationDays
        );

        daysCountText.setText(durationText);
    }


    private void updateDepartureDateFromDuration() {
        if (startDateStorage != null) {
            try {
                Date startDate = storageFormat.parse(startDateStorage);
                if (startDate != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(startDate);
                    cal.add(Calendar.DAY_OF_MONTH, durationDays - 1); // -1 because if staying 1 day, departure is same day

                    Date departureDate = cal.getTime();
                    endDateStorage = storageFormat.format(departureDate);
                    departureDateText.setText(displayFormat.format(departureDate));
                }
            } catch (ParseException e) {
                Log.e("CalendarActivity", "Date parsing failed", e);
            }
        }
        updateDurationDisplay();
    }

    private void setupNextButton() {
        nextButton.setOnClickListener(v -> {
            if (startDateStorage == null || endDateStorage == null) {
                Toast.makeText(this, "Please select your arrival date.", Toast.LENGTH_SHORT).show();
                return;
            }

            startActivity(createExtraQuestionsIntent());
        });
    }

    private void setupBackButton(){
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }


    private Intent createExtraQuestionsIntent() {
        Intent intent = new Intent(this, ExtraQuestionsActivity.class);
        intent.putExtra("start_date", startDateStorage);
        intent.putExtra("end_date", endDateStorage);
        intent.putExtra("duration_days", durationDays);
        intent.putExtra(EXTRA_CITY, city);
        intent.putExtra(EXTRA_LATITUDE, latitude);
        intent.putExtra(EXTRA_LONGITUDE, longitude);
        return intent;
    }

    private Date parseStorageDate(String date) {
        try {
            return storageFormat.parse(date);
        } catch (ParseException e) {
            Log.e("CalendarActivity", "Date parsing failed", e);
            return null;
        }
    }
}
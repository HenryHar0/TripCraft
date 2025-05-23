package com.henry.tripcraft;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    // Intent keys
    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_GEONAME_ID = "geonameId";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";

    private CalendarView calendarView;
    private TabLayout dateTabLayout;
    private MaterialButton nextButton;
    private TextView arrivalDateText, departureDateText, daysCountText;

    private String startDateStorage;
    private String endDateStorage;
    private boolean isArrivalTab = true;
    private boolean suppressListener = false;

    private ImageButton backButton;

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private String city;
    private int geonameId;
    private double latitude;
    private double longitude;

    private final Calendar today = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        initViews();
        extractIntentData();

        // Set week to start with Monday
        calendarView.setFirstDayOfWeek(Calendar.MONDAY);

        calendarView.setMinDate(today.getTimeInMillis());
        calendarView.setDate(today.getTimeInMillis(), false, true);
        Date todayDate = new Date(today.getTimeInMillis());
        startDateStorage = storageFormat.format(todayDate);
        arrivalDateText.setText(displayFormat.format(todayDate));

        setupTabListener();
        setupDateChangeListener();
        setupNextButton();
        setupBackButton();
    }

    private void initViews() {
        calendarView = findViewById(R.id.date_picker);
        dateTabLayout = findViewById(R.id.date_tab_layout);
        nextButton = findViewById(R.id.next_button);
        arrivalDateText = findViewById(R.id.arrival_date);
        departureDateText = findViewById(R.id.departure_date);
        daysCountText = findViewById(R.id.days_count);
        backButton = findViewById(R.id.back_button);
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        city = intent.getStringExtra(EXTRA_CITY);
        geonameId = intent.getIntExtra(EXTRA_GEONAME_ID, -1);
        latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0);
        longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0);
    }

    private void setupTabListener() {
        dateTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isArrivalTab = (tab.getPosition() == 0);
                long targetDate = today.getTimeInMillis();

                suppressListener = true;

                if (isArrivalTab) {
                    calendarView.setMinDate(today.getTimeInMillis());
                    if (startDateStorage != null) {
                        targetDate = parseStorageToMillisCorrect(startDateStorage);
                    }
                } else {
                    if (startDateStorage != null) {
                        long startMillis = parseStorageToMillisCorrect(startDateStorage);
                        calendarView.setMinDate(startMillis);

                        if (endDateStorage != null) {
                            targetDate = parseStorageToMillisCorrect(endDateStorage);
                        } else {
                            targetDate = startMillis;
                        }
                    } else {
                        calendarView.setMinDate(today.getTimeInMillis());
                    }
                }

                calendarView.setDate(targetDate, false, true);
                suppressListener = false;
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupDateChangeListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            if (suppressListener) return;

            String storageDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            String displayDate = displayFormat.format(cal.getTime());

            if (isArrivalTab) {
                startDateStorage = storageDate;
                arrivalDateText.setText(displayDate);

                // Clear departure date if it becomes invalid
                if (endDateStorage != null && isEndBeforeStart(startDateStorage, endDateStorage)) {
                    endDateStorage = null;
                    departureDateText.setText("--");
                    daysCountText.setText("-- nights");
                }
            } else {
                if (startDateStorage == null || isEndBeforeStart(startDateStorage, storageDate)) {
                    Toast.makeText(this, "Departure date cannot be before arrival date", Toast.LENGTH_SHORT).show();

                    // Reset to previous valid date or arrival date
                    suppressListener = true;
                    if (endDateStorage != null) {
                        calendarView.setDate(parseStorageToMillisCorrect(endDateStorage), false, true);
                    } else if (startDateStorage != null) {
                        calendarView.setDate(parseStorageToMillisCorrect(startDateStorage), false, true);
                    }
                    suppressListener = false;
                    return;
                }

                endDateStorage = storageDate;
                departureDateText.setText(displayDate);
                updateDuration();
            }
        });
    }

    private void setupNextButton() {
        nextButton.setOnClickListener(v -> {
            if (startDateStorage == null || endDateStorage == null) {
                Toast.makeText(this, "Please select both arrival and departure dates.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEndBeforeStart(startDateStorage, endDateStorage)) {
                Toast.makeText(this, "Departure date cannot be before arrival date.", Toast.LENGTH_SHORT).show();
                return;
            }

            startActivity(createInterestsIntent());
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

    private Intent createInterestsIntent() {
        Intent intent = new Intent(this, InterestsActivity.class);
        intent.putExtra("start_date", startDateStorage);
        intent.putExtra("end_date", endDateStorage);
        intent.putExtra("duration_days", calculateDurationDays(startDateStorage, endDateStorage));
        intent.putExtra(EXTRA_CITY, city);
        intent.putExtra(EXTRA_GEONAME_ID, geonameId);
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

    private long parseStorageToMillis(String storageDate) {
        Date date = parseStorageDate(storageDate);
        return (date != null) ? date.getTime() : System.currentTimeMillis();
    }

    // Fixed method to handle date parsing correctly
    private long parseStorageToMillisCorrect(String storageDate) {
        try {
            Date date = storageFormat.parse(storageDate);
            if (date != null) {
                // Create calendar and set the parsed date to avoid timezone issues
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                // Ensure we're using the correct date at midnight
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            }
        } catch (ParseException e) {
            Log.e("CalendarActivity", "Date parsing failed", e);
        }
        return System.currentTimeMillis();
    }

    private boolean isEndBeforeStart(String start, String end) {
        Date d1 = parseStorageDate(start);
        Date d2 = parseStorageDate(end);
        return d1 != null && d2 != null && d2.before(d1);
    }

    private int calculateDurationDays(String start, String end) {
        Date d1 = parseStorageDate(start);
        Date d2 = parseStorageDate(end);
        if (d1 != null && d2 != null) {
            long diff = d2.getTime() - d1.getTime();
            return (int) (diff / (1000 * 60 * 60 * 24) + 1);
        }
        return 0;
    }

    private void updateDuration() {
        if (startDateStorage != null && endDateStorage != null) {
            int days = calculateDurationDays(startDateStorage, endDateStorage);
            daysCountText.setText(days + (days == 1 ? " night" : " nights"));
        }
    }
}
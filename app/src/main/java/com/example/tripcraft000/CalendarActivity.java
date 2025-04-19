package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;
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

    private CalendarView calendarView;
    private TabLayout dateTabLayout;
    private MaterialButton nextButton;
    private TextView arrivalDateText, departureDateText, daysCountText;

    private String startDateStorage;
    private String endDateStorage;
    private boolean isArrivalTab = true;
    private boolean suppressListener = false;

    private SimpleDateFormat displayFormat;
    private SimpleDateFormat storageFormat;

    // Intent extras
    private String city;
    private int geonameId;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        calendarView = findViewById(R.id.date_picker);
        dateTabLayout = findViewById(R.id.date_tab_layout);
        nextButton = findViewById(R.id.next_button);
        arrivalDateText = findViewById(R.id.arrival_date);
        departureDateText = findViewById(R.id.departure_date);
        daysCountText = findViewById(R.id.days_count);

        Intent intent = getIntent();
        city = intent.getStringExtra("city");
        geonameId = intent.getIntExtra("geonameId", -1);
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);

        Calendar today = Calendar.getInstance();
        calendarView.setMinDate(today.getTimeInMillis());

        dateTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isArrivalTab = (tab.getPosition() == 0);
                long targetDate = today.getTimeInMillis();
                if (isArrivalTab) {
                    calendarView.setMinDate(today.getTimeInMillis());
                    if (startDateStorage != null) {
                        targetDate = parseStorageToMillis(startDateStorage);
                    }
                } else {
                    if (startDateStorage != null) {
                        try {
                            Date start = storageFormat.parse(startDateStorage);
                            if (start != null) {
                                calendarView.setMinDate(start.getTime());
                                targetDate = (endDateStorage != null)
                                        ? parseStorageToMillis(endDateStorage)
                                        : start.getTime();
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        calendarView.setMinDate(today.getTimeInMillis());
                    }
                }
                // Clear or move highlight
                suppressListener = true;
                calendarView.setDate(targetDate, false, true);
                suppressListener = false;
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            if (suppressListener) return;
            String storageDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            String displayDate = displayFormat.format(cal.getTime());

            if (isArrivalTab) {
                startDateStorage = storageDate;
                arrivalDateText.setText(displayDate);
                if (endDateStorage != null && isEndBeforeStart(startDateStorage, endDateStorage)) {
                    endDateStorage = null;
                    departureDateText.setText("--");
                    daysCountText.setText("-- nights");
                }
            } else {
                if (startDateStorage != null && isEndBeforeStart(startDateStorage, storageDate)) {
                    Toast.makeText(CalendarActivity.this,
                            "Departure date cannot be before arrival date",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                endDateStorage = storageDate;
                departureDateText.setText(displayDate);
                updateDuration();
            }
        });

        nextButton.setOnClickListener(v -> {
            if (startDateStorage == null || endDateStorage == null) {
                Toast.makeText(CalendarActivity.this, "Please select both arrival and departure dates.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isEndBeforeStart(startDateStorage, endDateStorage)) {
                Toast.makeText(CalendarActivity.this, "Departure date cannot be before arrival date.", Toast.LENGTH_SHORT).show();
                return;
            }

            int durationDays = calculateDurationDays(startDateStorage, endDateStorage);

            Intent interestsIntent = new Intent(CalendarActivity.this, InterestsActivity.class);
            interestsIntent.putExtra("start_date", startDateStorage);
            interestsIntent.putExtra("end_date", endDateStorage);
            interestsIntent.putExtra("duration_days", durationDays);
            interestsIntent.putExtra("city", city);
            interestsIntent.putExtra("geonameId", geonameId);
            interestsIntent.putExtra("latitude", latitude);
            interestsIntent.putExtra("longitude", longitude);
            startActivity(interestsIntent);
        });
    }

    private long parseStorageToMillis(String storageDate) {
        try {
            Date date = storageFormat.parse(storageDate);
            if (date != null) return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis();
    }

    private void updateDuration() {
        if (startDateStorage != null && endDateStorage != null) {
            int days = calculateDurationDays(startDateStorage, endDateStorage);
            daysCountText.setText(days + (days == 1 ? " night" : " nights"));
        }
    }

    private int calculateDurationDays(String start, String end) {
        try {
            Date d1 = storageFormat.parse(start);
            Date d2 = storageFormat.parse(end);
            if (d1 != null && d2 != null) {
                long diff = d2.getTime() - d1.getTime();
                return (int) (diff / (1000 * 60 * 60 * 24));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private boolean isEndBeforeStart(String start, String end) {
        try {
            Date d1 = storageFormat.parse(start);
            Date d2 = storageFormat.parse(end);
            return d1 != null && d2 != null && d2.before(d1);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
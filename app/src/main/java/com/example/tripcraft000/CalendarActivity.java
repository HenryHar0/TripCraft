package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CalendarActivity extends AppCompatActivity {

    private DatePicker datePicker;
    private Button nextButton;
    private TextView datePrompt, errorMessage;

    private String startDate;
    private String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        datePicker = findViewById(R.id.date_picker);
        nextButton = findViewById(R.id.next_button);
        datePrompt = findViewById(R.id.date_prompt);
        errorMessage = findViewById(R.id.error_message);

        Intent intent = getIntent();
        city = intent.getStringExtra("city");

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int todayYear = calendar.get(java.util.Calendar.YEAR);
        int todayMonth = calendar.get(java.util.Calendar.MONTH);
        int todayDay = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        datePicker.updateDate(todayYear, todayMonth, todayDay);

        datePrompt.setText("Select Start Date");

        errorMessage.setVisibility(TextView.GONE);

        nextButton.setOnClickListener(v -> {
            if (startDate == null) {
                startDate = getSelectedDate();
                datePrompt.setText("Select End Date");
                nextButton.setText("Finish");
            } else {
                String endDate = getSelectedDate();

                if (isEndDateEarlierThanStartDate(startDate, endDate)) {
                    errorMessage.setVisibility(TextView.VISIBLE);
                    errorMessage.setText("End date cannot be earlier than start date.");
                    errorMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    errorMessage.setVisibility(TextView.GONE);

                    Intent planIntent = new Intent(CalendarActivity.this, PlanActivity.class);
                    planIntent.putExtra("start_date", startDate);
                    planIntent.putExtra("end_date", endDate);
                    planIntent.putExtra("city", city);
                    startActivity(planIntent);
                }
            }
        });
    }

    private String getSelectedDate() {
        int year = datePicker.getYear();
        int month = datePicker.getMonth() + 1;
        int day = datePicker.getDayOfMonth();
        return year + "-" + month + "-" + day;
    }

    private boolean isEndDateEarlierThanStartDate(String startDate, String endDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            return end != null && start != null && end.before(start);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}

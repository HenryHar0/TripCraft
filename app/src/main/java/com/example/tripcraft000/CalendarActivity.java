package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import androidx.appcompat.app.AppCompatActivity;

public class CalendarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        DatePicker datePicker = findViewById(R.id.date_picker);
        Button nextButton = findViewById(R.id.next_button);

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int todayYear = calendar.get(java.util.Calendar.YEAR);
        int todayMonth = calendar.get(java.util.Calendar.MONTH);
        int todayDay = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        datePicker.updateDate(todayYear, todayMonth, todayDay);

        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(CalendarActivity.this, PlanActivity.class);
            startActivity(intent);
        });
    }
}

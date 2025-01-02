package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PlanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        ListView activitiesList = findViewById(R.id.activitiesList);

        findViewById(R.id.editPlanButton).setOnClickListener(v -> {
            Toast.makeText(this, "Edit Plan clicked", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.deletePlanButton).setOnClickListener(v -> {
            Toast.makeText(this, "Delete Plan clicked", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.backToMainButton).setOnClickListener(v -> {
            Intent intent = new Intent(PlanActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}

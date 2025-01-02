package com.example.tripcraft000;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button generateNewPlanButton;
    private ListView savedPlansList;
    private TextView savedPlansLabel, footerText;

    private ArrayList<String> savedPlans;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        View profileButton = findViewById(R.id.profileButton);
        generateNewPlanButton = findViewById(R.id.generateNewPlanButton);
        savedPlansList = findViewById(R.id.savedPlansList);
        savedPlansLabel = findViewById(R.id.savedPlansLabel);
        footerText = findViewById(R.id.footerText);

        // Initialize saved plans list
        savedPlans = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, savedPlans);
        savedPlansList.setAdapter(adapter);

        // Profile button click listener
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Generate New Plan button click listener
        generateNewPlanButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CityActivity.class);
            startActivityForResult(intent, 1); // Use request code 1 to get results
        });

        // Saved plans list click listener
        savedPlansList.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String selectedPlan = savedPlans.get(position);
            Toast.makeText(MainActivity.this, "Selected Plan: " + selectedPlan, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Retrieve new plan from GeneratePlanActivity
            String newPlan = data.getStringExtra("plan");
            if (newPlan != null && !newPlan.isEmpty()) {
                // Add new plan to the list
                if (savedPlans.size() >= 5) {
                    savedPlans.remove(0); // Remove the oldest plan if the list is full
                }
                savedPlans.add(newPlan);
                adapter.notifyDataSetChanged();
            }
        }
    }
}

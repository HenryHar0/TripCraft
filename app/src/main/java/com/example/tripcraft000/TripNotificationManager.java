package com.example.tripcraft000;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TripNotificationManager {
    private static final String TAG = "TripNotificationManager";
    private static final String PREFS_NAME = "TripPlanPrefs";
    private static final String NOTIFICATION_PREF = "NotificationsEnabled";
    private static final String FIRST_TIME_KEY = "FirstTimeUser";
    private static final String CHANNEL_ID = "trip_reminder_channel";
    private static final int ONE_WEEK_NOTIFICATION_ID = 1;
    private static final int ONE_DAY_NOTIFICATION_ID = 2;

    private final Context context;
    private final ActivityResultLauncher<String> requestPermissionLauncher;

    public TripNotificationManager(Context context, ActivityResultLauncher<String> requestPermissionLauncher) {
        this.context = context;
        this.requestPermissionLauncher = requestPermissionLauncher;
        createNotificationChannel();
    }

    public void checkFirstTimeUser(String city, String startDate) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstTime = sharedPreferences.getBoolean(FIRST_TIME_KEY, true);

        if (isFirstTime) {
            showNotificationExplanationDialog(city, startDate);

            // Mark as not first time anymore
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(FIRST_TIME_KEY, false);
            editor.apply();
        }
    }

    private void showNotificationExplanationDialog(String city, String startDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Trip Reminders");

        // Blue-styled explanation text
        android.widget.TextView messageView = new android.widget.TextView(context);
        messageView.setText("TripCraft can send you helpful reminders about your upcoming trips!\n\n" +
                "• Get notified one week before your trip to start packing\n" +
                "• Receive a final reminder the day before departure\n\n" +
                "Would you like to enable trip reminders?");
        messageView.setPadding(30, 30, 30, 30);
        messageView.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
        messageView.setTextSize(16);

        builder.setView(messageView);

        builder.setPositiveButton("Enable", (dialog, which) -> {
            requestNotificationPermission(city, startDate);
        });

        builder.setNegativeButton("Not Now", (dialog, which) -> {
            Toast.makeText(context, "You can enable notifications later in settings", Toast.LENGTH_SHORT).show();
        });

        builder.setCancelable(false);
        builder.show();
    }

    public void requestNotificationPermission(String city, String startDate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // For older versions, no runtime permission needed
            SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(NOTIFICATION_PREF, true);
            editor.apply();

            if (startDate != null && !startDate.isEmpty()) {
                scheduleNotifications(startDate, city);
            }

            Toast.makeText(context, "Trip reminders enabled!", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Trip Reminders";
            String description = "Notifications for upcoming trips";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleNotifications(String tripDate, String city) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date tripStartDate = dateFormat.parse(tripDate);

            if (tripStartDate != null) {
                scheduleNotification(tripStartDate, 7, ONE_WEEK_NOTIFICATION_ID,
                        "Trip to " + city + " is in one week!",
                        "Time to start planning and packing for your adventure.");

                scheduleNotification(tripStartDate, 1, ONE_DAY_NOTIFICATION_ID,
                        "Your trip to " + city + " is tomorrow!",
                        "Final preparations for your journey to " + city + ".");
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error scheduling notifications", e);
        }
    }

    private void scheduleNotification(Date tripDate, int daysBefore, int notificationId, String title, String message) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tripDate);
        calendar.add(Calendar.DAY_OF_YEAR, -daysBefore);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    // Handle permission result
    public void handlePermissionResult(boolean isGranted, String city, String startDate) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (isGranted) {
            editor.putBoolean(NOTIFICATION_PREF, true);
            editor.apply();

            // Schedule notifications if start date is available
            if (startDate != null && !startDate.isEmpty()) {
                scheduleNotifications(startDate, city);
            }

            Toast.makeText(context, "Trip reminders will be sent before your journey!", Toast.LENGTH_SHORT).show();
        } else {
            editor.putBoolean(NOTIFICATION_PREF, false);
            editor.apply();

            Toast.makeText(context, "Notifications disabled. You can enable them later in settings.", Toast.LENGTH_SHORT).show();
        }
    }
}

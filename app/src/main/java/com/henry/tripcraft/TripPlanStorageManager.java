package com.henry.tripcraft;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages saving, loading, and managing trip plans in Firestore with proper nested structure
 */
public class TripPlanStorageManager {
    private static final String TAG = "TripPlanStorageManager";
    private static final String PREFS_NAME = "TripPlanPrefs";
    private static final int MAX_SLOTS = 5;

    // Collection paths
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TRIPS = "trips";
    private static final String COLLECTION_PLACES = "places";

    private final Context context;
    private final TripNotificationManager notificationManager;
    private final Gson gson;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // In-memory cache for Place objects
    private final LruCache<String, PlaceData> placeCache;
    private final PlacesApiService placesApiService;

    public TripPlanStorageManager(Context context, TripNotificationManager notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
        this.gson = new Gson();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.placeCache = new LruCache<>(100);
        this.placesApiService = new PlacesApiService(context);
    }

    /**
     * Shows dialog to choose a save slot, then handles saving the trip plan
     */
    public void showSaveTripPlanDialog(
            String destination,
            String duration,
            List<List<PlaceData>> activitiesListData,
            String weather,
            String city,
            String startDate) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Please sign in to save plans", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load existing slots
        loadUserSlots(user.getUid(), (slots) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Select a slot to save the plan");

            String[] slotDisplays = new String[MAX_SLOTS];
            for (int i = 0; i < MAX_SLOTS; i++) {
                if (slots.containsKey("slot_" + i)) {
                    TripDocument existingPlan = slots.get("slot_" + i);
                    slotDisplays[i] = "Slot " + (i + 1) + ": " + existingPlan.tripName;
                } else {
                    slotDisplays[i] = "Slot " + (i + 1) + ": Empty Slot";
                }
            }

            builder.setItems(slotDisplays, (dialog, which) -> {
                if (slots.containsKey("slot_" + which)) {
                    confirmOverwrite(which, destination, duration, activitiesListData, weather, city, startDate);
                } else {
                    savePlanToSlot(which, destination, duration, activitiesListData, weather, city, startDate);
                }
            });

            builder.show();
        });
    }

    /**
     * Shows confirmation dialog before overwriting an existing slot
     */
    private void confirmOverwrite(
            int slot,
            String destination,
            String duration,
            List<List<PlaceData>> activitiesListData,
            String weather,
            String city,
            String startDate) {

        new AlertDialog.Builder(context)
                .setTitle("Overwrite Slot")
                .setMessage("Do you want to overwrite this slot?")
                .setPositiveButton("Yes", (dialog, which) ->
                        savePlanToSlot(slot, destination, duration, activitiesListData, weather, city, startDate))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Saves trip plan data to the selected slot using proper Firestore structure
     */
    private void savePlanToSlot(
            int slot,
            String destination,
            String duration,
            List<List<PlaceData>> activitiesListData,
            String weather,
            String city,
            String startDate) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Please sign in to save plans", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate unique trip ID
        String tripId = "slot_" + slot + "_" + UUID.randomUUID().toString().substring(0, 8);

        // Create trip document
        TripDocument tripDoc = new TripDocument();
        tripDoc.tripName = destination;
        tripDoc.startDate = startDate;
        tripDoc.duration = duration;
        tripDoc.weather = weather;
        tripDoc.city = city;
        tripDoc.slotNumber = slot;
        tripDoc.createdAt = System.currentTimeMillis();

        // Calculate end date (simplified - you might want more sophisticated date handling)
        tripDoc.endDate = calculateEndDate(startDate, duration);

        // Reference to the trip document
        String userDocPath = COLLECTION_USERS + "/" + user.getUid();
        String tripDocPath = userDocPath + "/" + COLLECTION_TRIPS + "/" + tripId;

        // First, save the trip document
        db.document(tripDocPath)
                .set(tripDoc)
                .addOnSuccessListener(aVoid -> {
                    // Now save places as subcollection
                    savePlacesToTrip(tripDocPath, activitiesListData, slot);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving trip document", e);
                    Toast.makeText(context, "Failed to save plan", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Saves places to the trip's places subcollection
     */
    private void savePlacesToTrip(String tripDocPath, List<List<PlaceData>> activitiesListData, int slot) {
        if (activitiesListData == null || activitiesListData.isEmpty()) {
            Toast.makeText(context, "Plan saved to Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
            return;
        }

        String placesCollectionPath = tripDocPath + "/" + COLLECTION_PLACES;

        // Create a batch to save all places at once
        int totalPlaces = 0;
        int savedPlaces = 0;

        for (int day = 0; day < activitiesListData.size(); day++) {
            List<PlaceData> dayPlaces = activitiesListData.get(day);
            totalPlaces += dayPlaces.size();

            for (int order = 0; order < dayPlaces.size(); order++) {
                PlaceData place = dayPlaces.get(order);

                // Create place document
                PlaceDocument placeDoc = new PlaceDocument();
                placeDoc.placeId = place.getPlaceId();
                placeDoc.day = day + 1; // Days start from 1
                placeDoc.order = order + 1; // Order starts from 1

                // Use placeId as document ID to avoid duplicates
                String placeDocId = "day" + (day + 1) + "_place" + (order + 1) + "_" + place.getPlaceId().replace("/", "_");

                db.collection(placesCollectionPath)
                        .document(placeDocId)
                        .set(placeDoc)
                        .addOnSuccessListener(aVoid -> {
                            // Cache the place data
                            placeCache.put(place.getPlaceId(), place);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error saving place: " + place.getPlaceId(), e);
                        });
            }
        }

        // Schedule notifications and show success message
        scheduleNotificationsAndShowSuccess(slot);
    }

    private void scheduleNotificationsAndShowSuccess(int slot) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = sharedPreferences.getBoolean("NotificationsEnabled", false);

        // Note: You'll need to pass startDate and city here if notifications are needed
        // For now, just show success message
        Toast.makeText(context, "Plan saved to Slot " + (slot + 1), Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a saved trip plan by loading from the new Firestore structure
     */
    public void showSavedTripPlan(int slot) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Please sign in to view saved plans", Toast.LENGTH_SHORT).show();
            return;
        }

        loadTripFromSlot(user.getUid(), slot, (tripPlan) -> {
            if (tripPlan == null) {
                Toast.makeText(context, "No plan saved in this slot", Toast.LENGTH_SHORT).show();
                return;
            }
            showTripPlanDialog(tripPlan, slot);
        });
    }

    /**
     * Loads a trip from a specific slot using the new structure
     */
    private void loadTripFromSlot(String userId, int slot, TripPlanCallback callback) {
        String userDocPath = COLLECTION_USERS + "/" + userId;

        // Find trip document for this slot
        db.collection(userDocPath + "/" + COLLECTION_TRIPS)
                .whereEqualTo("slotNumber", slot)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onResult(null);
                        return;
                    }

                    DocumentSnapshot tripDoc = querySnapshot.getDocuments().get(0);
                    TripDocument trip = tripDoc.toObject(TripDocument.class);

                    if (trip == null) {
                        callback.onResult(null);
                        return;
                    }

                    // Load places for this trip
                    String tripDocPath = userDocPath + "/" + COLLECTION_TRIPS + "/" + tripDoc.getId();
                    loadPlacesForTrip(tripDocPath, trip, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading trip from slot " + slot, e);
                    callback.onResult(null);
                });
    }

    /**
     * Loads places for a specific trip
     */
    private void loadPlacesForTrip(String tripDocPath, TripDocument trip, TripPlanCallback callback) {
        String placesCollectionPath = tripDocPath + "/" + COLLECTION_PLACES;

        db.collection(placesCollectionPath)
                .orderBy("day")
                .orderBy("order")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Group places by day
                    Map<Integer, List<PlaceDocument>> placesByDay = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        PlaceDocument place = doc.toObject(PlaceDocument.class);

                        if (!placesByDay.containsKey(place.day)) {
                            placesByDay.put(place.day, new ArrayList<>());
                        }
                        placesByDay.get(place.day).add(place);
                    }

                    // Resolve place IDs to PlaceData objects
                    resolvePlacesFromDocuments(placesByDay, trip, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading places for trip", e);
                    callback.onResult(null);
                });
    }

    /**
     * Resolves PlaceDocument objects to PlaceData objects
     */
    private void resolvePlacesFromDocuments(Map<Integer, List<PlaceDocument>> placesByDay, TripDocument trip, TripPlanCallback callback) {
        List<List<PlaceData>> activitiesListData = new ArrayList<>();
        List<CompletableFuture<Void>> dayFutures = new ArrayList<>();

        // Sort days and process them
        List<Integer> sortedDays = new ArrayList<>(placesByDay.keySet());
        sortedDays.sort(Integer::compareTo);

        for (int dayIndex = 0; dayIndex < sortedDays.size(); dayIndex++) {
            int day = sortedDays.get(dayIndex);
            List<PlaceDocument> dayPlaceDocs = placesByDay.get(day);
            List<PlaceData> dayPlaces = new ArrayList<>();
            activitiesListData.add(dayPlaces);

            CompletableFuture<Void> dayFuture = new CompletableFuture<>();
            dayFutures.add(dayFuture);

            resolveDayPlacesFromDocuments(dayPlaceDocs, dayPlaces, dayFuture);
        }

        // Wait for all days to be resolved
        CompletableFuture.allOf(dayFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    TripPlan tripPlan = tripDocumentToTripPlan(trip, activitiesListData);
                    callback.onResult(tripPlan);
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error resolving places", throwable);
                    callback.onResult(null);
                    return null;
                });
    }

    /**
     * Resolves places for a single day from PlaceDocument objects
     */
    private void resolveDayPlacesFromDocuments(List<PlaceDocument> placeDocs, List<PlaceData> dayPlaces, CompletableFuture<Void> dayFuture) {
        if (placeDocs.isEmpty()) {
            dayFuture.complete(null);
            return;
        }

        List<CompletableFuture<PlaceData>> placeFutures = new ArrayList<>();

        for (PlaceDocument placeDoc : placeDocs) {
            CompletableFuture<PlaceData> placeFuture = new CompletableFuture<>();
            placeFutures.add(placeFuture);

            // Check cache first
            PlaceData cachedPlace = placeCache.get(placeDoc.placeId);
            if (cachedPlace != null) {
                placeFuture.complete(cachedPlace);
            } else {
                // Fetch from Places API
                placesApiService.getPlaceDetails(placeDoc.placeId, (placeData) -> {
                    if (placeData != null) {
                        placeCache.put(placeDoc.placeId, placeData);
                        placeFuture.complete(placeData);
                    } else {
                        // Create placeholder
                        PlaceData placeholder = new PlaceData();
                        placeholder.setPlaceId(placeDoc.placeId);
                        placeholder.setName("Place not available");
                        placeFuture.complete(placeholder);
                    }
                });
            }
        }

        // Wait for all places to be resolved
        CompletableFuture.allOf(placeFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    for (CompletableFuture<PlaceData> future : placeFutures) {
                        try {
                            dayPlaces.add(future.get());
                        } catch (Exception e) {
                            Log.e(TAG, "Error getting place data", e);
                        }
                    }
                    dayFuture.complete(null);
                });
    }

    /**
     * Shows the trip plan dialog
     */
    private void showTripPlanDialog(TripPlan tripPlan, int slot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_trip_plan_view, null);

        RecyclerView recyclerView = dialogView.findViewById(R.id.tripPlanRecyclerView);
        DayByDayAdapter adapter = new DayByDayAdapter(tripPlan.activitiesListData, tripPlan.apiKey);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

        builder.setView(dialogView)
                .setTitle("Trip Plan: " + tripPlan.destination)
                .setPositiveButton("Close", null)
                .setNegativeButton("Delete", (dialog, which) -> confirmDelete(slot))
                .show();
    }

    /**
     * Loads user's saved slots
     */
    private void loadUserSlots(String userId, SlotLoadCallback callback) {
        String userDocPath = COLLECTION_USERS + "/" + userId;

        db.collection(userDocPath + "/" + COLLECTION_TRIPS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, TripDocument> slots = new HashMap<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        TripDocument trip = doc.toObject(TripDocument.class);
                        if (trip.slotNumber >= 0 && trip.slotNumber < MAX_SLOTS) {
                            slots.put("slot_" + trip.slotNumber, trip);
                        }
                    }
                    callback.onLoaded(slots);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user slots", e);
                    callback.onLoaded(new HashMap<>());
                });
    }

    /**
     * Gets trip plan from specific slot (async)
     */
    public void getTripPlanFromSlot(int slot, TripPlanCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onResult(null);
            return;
        }
        loadTripFromSlot(user.getUid(), slot, callback);
    }

    /**
     * Deletes a plan from a specific slot
     */
    public void deletePlanFromSlot(int slot) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Please sign in to delete plans", Toast.LENGTH_SHORT).show();
            return;
        }

        loadTripFromSlot(user.getUid(), slot, (tripPlan) -> {
            if (tripPlan != null) {
                // Delete the entire trip document (subcollections will be deleted too)
                String userDocPath = COLLECTION_USERS + "/" + user.getUid();
                db.collection(userDocPath + "/" + COLLECTION_TRIPS)
                        .whereEqualTo("slotNumber", slot)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                querySnapshot.getDocuments().get(0).getReference()
                                        .delete()
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Plan deleted from slot " + slot))
                                        .addOnFailureListener(e -> Log.e(TAG, "Error deleting plan", e));
                            }
                        });
            }
        });
    }

    /**
     * Confirms deletion of a saved plan
     */
    private void confirmDelete(int slot) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Plan")
                .setMessage("Are you sure you want to delete this plan?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    deletePlanFromSlot(slot);
                    Toast.makeText(context, "Plan deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Helper method to calculate end date based on start date and duration
     */
    private String calculateEndDate(String startDate, String duration) {
        // Simple implementation - you might want to use proper date handling
        if (startDate == null || duration == null) return null;

        try {
            int days = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
            // Add days to start date (simplified - you'd use proper date parsing)
            return startDate; // Placeholder - implement proper date calculation
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts TripDocument to TripPlan
     */
    private TripPlan tripDocumentToTripPlan(TripDocument tripDoc, List<List<PlaceData>> activitiesListData) {
        TripPlan tripPlan = new TripPlan();
        tripPlan.destination = tripDoc.tripName;
        tripPlan.duration = tripDoc.duration;
        tripPlan.weather = tripDoc.weather;
        tripPlan.city = tripDoc.city;
        tripPlan.startDate = tripDoc.startDate;
        tripPlan.activitiesListData = activitiesListData;
        return tripPlan;
    }

    /**
     * Clears the in-memory place cache
     */
    public void clearCache() {
        placeCache.evictAll();
    }

    // Legacy method for backward compatibility
    public String getPlanFromSlot(int slot) {
        return "Use getTripPlanFromSlot instead";
    }

    /**
     * Trip document structure for Firestore
     */
    public static class TripDocument {
        public String tripName;
        public String startDate;
        public String endDate;
        public String duration;
        public String weather;
        public String city;
        public int slotNumber;
        public long createdAt;
    }

    /**
     * Place document structure for Firestore subcollection
     */
    public static class PlaceDocument {
        public String placeId;
        public int day;
        public int order;
    }

    /**
     * Original trip plan structure
     */
    public static class TripPlan {
        public String destination;
        public String duration;
        public List<List<PlaceData>> activitiesListData;
        public String weather;
        public String city;
        public String startDate;
        public String apiKey;
    }

    // Callback interfaces
    public interface SlotLoadCallback {
        void onLoaded(Map<String, TripDocument> slots);
    }

    public interface TripPlanCallback {
        void onResult(TripPlan tripPlan);
    }

    public interface PlaceResolutionCallback {
        void onResolved(TripPlan tripPlan);
    }
}
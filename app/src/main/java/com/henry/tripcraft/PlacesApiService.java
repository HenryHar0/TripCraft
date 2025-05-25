package com.henry.tripcraft;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching place details from Google Places API (New)
 */
public class PlacesApiService {
    private static final String TAG = "PlacesApiService";
    private static final String PLACES_API_BASE_URL = "https://places.googleapis.com/v1/places";
    private static final String PHOTO_BASE_URL = "https://places.googleapis.com/v1";

    private final Context context;
    private final RequestQueue requestQueue;
    private final String apiKey;

    public PlacesApiService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
        // Get API key from resources or SharedPreferences
        this.apiKey = getApiKey();
    }

    /**
     * Get API key from app resources or SharedPreferences
     */
    private String getApiKey() {
        return context.getString(
                context.getResources().getIdentifier("google_api_key1", "string", context.getPackageName())
        );
    }

    /**
     * Fetch place details by place ID using Places API (New)
     */
    public void getPlaceDetails(String placeId, PlaceDetailsCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "API key is not available");
            callback.onResult(null);
            return;
        }

        String url = PLACES_API_BASE_URL + "/" + placeId;

        // Create request body with field mask
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("languageCode", "en");
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body", e);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        PlaceData placeData = parsePlaceFromNewApiJson(response);
                        callback.onResult(placeData);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing place details", e);
                        callback.onResult(null);
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching place details: " + error.getMessage(), error);
                    callback.onResult(null);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("X-Goog-Api-Key", apiKey);
                headers.put("X-Goog-FieldMask", "id,displayName,formattedAddress,rating,location,types,userRatingCount,priceLevel,regularOpeningHours,websiteUri,photos");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    /**
     * Parse place data from Places API (New) JSON response
     */
    private PlaceData parsePlaceFromNewApiJson(JSONObject placeJson) throws JSONException {
        String placeId = placeJson.optString("id", "");

        // Display name structure
        String name = "Unknown Place";
        if (placeJson.has("displayName")) {
            JSONObject displayName = placeJson.getJSONObject("displayName");
            name = displayName.optString("text", "Unknown Place");
        }

        String address = placeJson.optString("formattedAddress", "");
        double rating = placeJson.optDouble("rating", 0.0);
        int userRatingsTotal = placeJson.optInt("userRatingCount", 0);
        int priceLevel = placeJson.optInt("priceLevel", -1);

        // Website URL
        String website = placeJson.optString("websiteUri", null);

        // Get location
        LatLng latLng = null;
        if (placeJson.has("location")) {
            JSONObject location = placeJson.getJSONObject("location");
            double lat = location.getDouble("latitude");
            double lng = location.getDouble("longitude");
            latLng = new LatLng(lat, lng);
        }

        // Get place type
        String placeType = "tourist_attraction"; // default
        if (placeJson.has("types")) {
            JSONArray types = placeJson.getJSONArray("types");
            if (types.length() > 0) {
                placeType = types.getString(0);
            }
        }

        // Get opening hours
        String openingHours = "N/A";
        if (placeJson.has("regularOpeningHours")) {
            JSONObject openingHoursJson = placeJson.getJSONObject("regularOpeningHours");
            if (openingHoursJson.has("weekdayDescriptions")) {
                JSONArray weekdayText = openingHoursJson.getJSONArray("weekdayDescriptions");
                StringBuilder hours = new StringBuilder();
                for (int i = 0; i < weekdayText.length(); i++) {
                    if (i > 0) hours.append("\n");
                    hours.append(weekdayText.getString(i));
                }
                openingHours = hours.toString();
            }
        }

        // Create PlaceData object
        PlaceData placeData = new PlaceData(placeId, name, address, (float)rating, latLng,
                placeType, userRatingsTotal, 60); // default 60 min

        placeData.setPriceLevel(priceLevel);
        placeData.setOpeningHours(openingHours);
        placeData.setWebsite(website);

        // Get photo URLs using Places API (New) format
        if (placeJson.has("photos")) {
            JSONArray photos = placeJson.getJSONArray("photos");
            List<String> photoUrls = new ArrayList<>();

            for (int i = 0; i < Math.min(photos.length(), 5); i++) { // Limit to 5 photos
                JSONObject photo = photos.getJSONObject(i);
                if (photo.has("name")) {
                    String photoName = photo.getString("name");
                    // Create photo URL using Places API (New) format
                    String photoUrl = PHOTO_BASE_URL + "/" + photoName + "/media?maxHeightPx=400&maxWidthPx=400&key=" + apiKey;
                    photoUrls.add(photoUrl);
                }
            }

            // Set photo URLs instead of references
            placeData.setPhotoReferences(photoUrls);

            // For backward compatibility, also set the first photo as main image
            if (!photoUrls.isEmpty()) {
                placeData. addPhotoReference(photoUrls.get(0));
            }
        }

        return placeData;
    }

    /**
     * Get photo URL from photo name (Places API New format)
     */
    public String getPhotoUrl(String photoName, int maxWidth, int maxHeight) {
        if (apiKey == null || apiKey.isEmpty() || photoName == null) {
            return null;
        }

        return PHOTO_BASE_URL + "/" + photoName + "/media?maxHeightPx=" + maxHeight +
                "&maxWidthPx=" + maxWidth + "&key=" + apiKey;
    }

    /**
     * Callback interface for place details
     */
    public interface PlaceDetailsCallback {
        void onResult(PlaceData placeData);
    }
}
package com.henry.tripcraft;

import com.google.android.gms.maps.model.LatLng;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlaceData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String placeId;
    private String name;
    private String address;
    private float rating;
    private transient LatLng latLng; // Mark as transient
    private String placeType;
    private int userRatingsTotal;
    private int timeSpent;
    private List<String> photoReferences;
    private boolean userSelected;
    private int priceLevel;
    private String openingHours;
    private float score;
    private String website;

    // Store lat/lng as primitives for serialization
    private double latitude;
    private double longitude;

    public PlaceData(String placeId, String name, String address, float rating,
                     LatLng latLng, String placeType, int userRatingsTotal, int timeSpent) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.latLng = latLng;
        this.placeType = placeType;
        this.userRatingsTotal = userRatingsTotal;
        this.timeSpent = timeSpent;
        this.photoReferences = new ArrayList<>();
        this.userSelected = false;
        this.priceLevel = -1;
        this.openingHours = "N/A";
        this.score = 0f;
        this.website = null;

        // Store lat/lng values for serialization
        if (latLng != null) {
            this.latitude = latLng.latitude;
            this.longitude = latLng.longitude;
        }
    }

    public PlaceData() {
        // Default constructor for placeholder creation
        this.placeId = "";
        this.name = "Unknown Place";
        this.address = "";
        this.rating = 0f;
        this.latLng = null;
        this.placeType = "tourist_attraction";
        this.userRatingsTotal = 0;
        this.timeSpent = 60;
        this.photoReferences = new ArrayList<>();
        this.userSelected = false;
        this.priceLevel = -1;
        this.openingHours = "N/A";
        this.score = 0f;
        this.website = null;
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    // Custom serialization methods
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // LatLng is already stored as lat/lng primitives, so nothing extra needed
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Reconstruct LatLng from stored primitives
        if (latitude != 0.0 || longitude != 0.0) {
            this.latLng = new LatLng(latitude, longitude);
        }
    }

    // Getters
    public String getPlaceId() { return placeId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public float getRating() { return rating; }
    public LatLng getLatLng() { return latLng; }
    public String getPlaceType() { return placeType; }
    public int getUserRatingsTotal() { return userRatingsTotal; }
    public int getTimeSpent() { return timeSpent; }
    public List<String> getPhotoReferences() { return photoReferences; }
    public boolean isUserSelected() { return userSelected; }
    public int getPriceLevel() { return priceLevel; }
    public String getOpeningHours() { return openingHours; }
    public float getScore() { return score; }
    public String getWebsite() { return website; }

    // Setters
    public void setPlaceId(String placeId) { this.placeId = placeId; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setRating(float rating) { this.rating = rating; }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
        // Update the primitive values for serialization
        if (latLng != null) {
            this.latitude = latLng.latitude;
            this.longitude = latLng.longitude;
        } else {
            this.latitude = 0.0;
            this.longitude = 0.0;
        }
    }

    public void setPlaceType(String placeType) { this.placeType = placeType; }
    public void setUserRatingsTotal(int userRatingsTotal) { this.userRatingsTotal = userRatingsTotal; }
    public void setTimeSpent(int timeSpent) { this.timeSpent = timeSpent; }
    public void setUserSelected(boolean userSelected) { this.userSelected = userSelected; }
    public void setPriceLevel(int priceLevel) { this.priceLevel = priceLevel; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public void setScore(float score) { this.score = score; }
    public void setWebsite(String website) { this.website = website; }

    public void addPhotoReference(String photoReference) {
        if (photoReferences == null) {
            photoReferences = new ArrayList<>();
        }
        photoReferences.add(photoReference);
    }

    public void setPhotoReferences(List<String> photoReferences) {
        this.photoReferences = photoReferences;
    }
}
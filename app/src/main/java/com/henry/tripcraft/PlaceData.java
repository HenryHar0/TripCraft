package com.henry.tripcraft;

import com.google.android.gms.maps.model.LatLng;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlaceData implements Serializable {
    private String placeId;
    private String name;
    private String address;
    private float rating;
    private LatLng latLng;
    private String placeType;
    private int userRatingsTotal;
    private int timeSpent;
    private List<String> photoReferences;
    private boolean userSelected;
    private int priceLevel;
    private String openingHours;
    private float score; // Added score field
    private String website; // Added website field

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
        this.score = 0f; // Default score
        this.website = null; // Default website
    }

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
    public float getScore() { return score; } // Getter for score
    public String getWebsite() { return website; } // Getter for website

    public void setPlaceId(String placeId) { this.placeId = placeId; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setRating(float rating) { this.rating = rating; }
    public void setLatLng(LatLng latLng) { this.latLng = latLng; }
    public void setPlaceType(String placeType) { this.placeType = placeType; }
    public void setUserRatingsTotal(int userRatingsTotal) { this.userRatingsTotal = userRatingsTotal; }
    public void setTimeSpent(int timeSpent) { this.timeSpent = timeSpent; }
    public void setUserSelected(boolean userSelected) { this.userSelected = userSelected; }
    public void setPriceLevel(int priceLevel) { this.priceLevel = priceLevel; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public void setScore(float score) { this.score = score; } // Setter for score
    public void setWebsite(String website) { this.website = website; } // Setter for website

    public void addPhotoReference(String photoReference) {
        if (photoReferences == null) {
            photoReferences = new ArrayList<>();
        }
        photoReferences.add(photoReference);
    }

    public void setPhotoReferences(List<String> photoReferences) {
        this.photoReferences = photoReferences;
    }

    // Add this constructor to your PlaceData class

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
    }
}
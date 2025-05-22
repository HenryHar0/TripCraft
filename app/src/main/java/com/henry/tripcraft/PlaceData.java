package com.henry.tripcraft;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class PlaceData {
    private String placeId;
    private String name;
    private String address;
    private float rating;
    private List<String> photoReferences;
    private LatLng latLng;
    private String placeType;
    private int userRatingsTotal;
    private int timeSpent;
    private float score;
    private boolean isUserSelected = false;


    public PlaceData(String placeId, String name, String address, float rating, LatLng latLng, String placeType, int userRatingsTotal, int timeSpent) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.latLng = latLng;
        this.placeType = placeType;
        this.userRatingsTotal = userRatingsTotal;
        this.photoReferences = new ArrayList<>();
        this.timeSpent = timeSpent;
    }


    public String getPlaceId() {
        return placeId;
    }
    public void setScore(float score) {
        this.score = score;
    }
    public void setUserSelected(boolean selected) {
        this.isUserSelected = selected;
    }

    public boolean isUserSelected() {
        Log.d("PlaceData", "isUserSelected() called for " + this.name);
        return isUserSelected;
    }


    public float getScore() {
        return score;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public float getRating() {
        return rating;
    }

    public List<String> getPhotoReferences() {
        return photoReferences;
    }

    public int getUserRatingsTotal() {
        return userRatingsTotal;
    }

    public void setPhotoReferences(List<String> photoReferences) {
        this.photoReferences = photoReferences;
    }

    public void addPhotoReference(String photoReference) {
        if (this.photoReferences == null) {
            this.photoReferences = new ArrayList<>();
        }
        this.photoReferences.add(photoReference);
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public String getPlaceType() {
        return placeType;
    }

    public int getTimeSpent() {
        return timeSpent;
    }
}

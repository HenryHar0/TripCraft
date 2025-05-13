package com.example.tripcraft000;

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

    public PlaceData(String placeId, String name, String address, float rating, LatLng latLng, String placeType) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.latLng = latLng;
        this.placeType = placeType;
        this.photoReferences = new ArrayList<>();
    }

    public String getPlaceId() {
        return placeId;
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
}

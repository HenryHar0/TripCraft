package com.example.tripcraft000;

import java.util.List;

public class GeoNamesResponse {

    public List<City> geonames;

    public static class City {
        public String name;
        public String countryName;
        public double lat;
        public double lng;
        public int geonameId;
    }
}



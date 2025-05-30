package com.henry.tripcraft.models;

import java.util.List;

public class GeoNamesResponse {

    public List<City> geonames;

    public static class City {
        public String name;
        public String countryName;
        public String countryCode;
        public double lat;
        public double lng;
        public int geonameId;
        public int population;
    }
}

package com.henry.tripcraft.models;

public class City {
    private final String name;
    private final String country;
    private final String region;
    private final double latitude;
    private final double longitude;
    private final int population;
    private final String countryCode;

    public City(String name, String country, String region, double latitude, double longitude, int population, String countryCode) {
        this.name = name;
        this.country = country;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.population = population;
        this.countryCode = countryCode;
    }

    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getRegion() { return region; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getPopulation() { return population; }
    public String getCountryCode() { return countryCode; }

    public String getDisplayName() {
        if (region != null && !region.isEmpty() && !region.equals(country)) {
            return name + ", " + region + ", " + country;
        }
        return name + ", " + country;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}

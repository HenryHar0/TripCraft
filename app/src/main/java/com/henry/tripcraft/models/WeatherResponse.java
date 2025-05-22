package com.henry.tripcraft.models;

import java.util.ArrayList;

public class WeatherResponse {
    public Properties properties;

    public static class Properties {
        public ArrayList<Period> periods;
    }

    public static class Period {
        public String name;
        public String detailedForecast;
    }
}

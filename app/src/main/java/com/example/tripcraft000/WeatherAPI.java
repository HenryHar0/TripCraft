package com.example.tripcraft000;

import com.example.tripcraft000.models.PointResponse;
import com.example.tripcraft000.models.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface WeatherAPI {
    @GET
    Call<PointResponse> getPointData(@Url String url);
    @GET
    Call<WeatherResponse> getForecast(@Url String url);
}

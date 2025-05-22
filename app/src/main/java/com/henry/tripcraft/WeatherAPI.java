package com.henry.tripcraft;

import com.henry.tripcraft.models.PointResponse;
import com.henry.tripcraft.models.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface WeatherAPI {
    @GET
    Call<PointResponse> getPointData(@Url String url);
    @GET
    Call<WeatherResponse> getForecast(@Url String url);
}

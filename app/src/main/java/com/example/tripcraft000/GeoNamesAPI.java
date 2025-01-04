package com.example.tripcraft000;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GeoNamesAPI {

    @GET("searchJSON")
    Call<GeoNamesResponse> searchCity(
            @Query("q") String cityName,
            @Query("maxRows") int maxRows,
            @Query("username") String username
    );
}


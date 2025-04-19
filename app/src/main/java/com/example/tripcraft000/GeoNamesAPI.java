package com.example.tripcraft000;

import com.example.tripcraft000.models.GeoNamesResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GeoNamesAPI {
    @GET("searchJSON")
    Call<GeoNamesResponse> searchCity(
            @Query("q") String query,
            @Query("featureCodes") String featureCodes,
            @Query("maxRows") int maxRows,
            @Query("username") String username
    );
}



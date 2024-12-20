package com.example.cmput301f24mikasa;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// Referenced from https://opencagedata.com/dashboard#geocoding by OpenCage, 2024-11-27
public interface GeocodingApi {
    @GET("geocode/v1/json")
    Call<GeocodeResponse> getCoordinates(
            @Query("q") String location,
            @Query("key") String apiKey
    );
}

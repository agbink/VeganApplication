package com.vegan.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;

public interface UserApiService {

    @GET("/api/users/me")
    Call<ApiUser> getMe(@Header("Authorization") String token);

    @PUT("/api/users/me")
    Call<ApiUser> updateProfile(@Header("Authorization") String token,
                                @Body Map<String, String> body);

    @PUT("/api/users/me/password")
    Call<Void> changePassword(@Header("Authorization") String token,
                              @Body Map<String, String> body);

    @DELETE("/api/users/me")
    Call<Void> deleteAccount(@Header("Authorization") String token);
}

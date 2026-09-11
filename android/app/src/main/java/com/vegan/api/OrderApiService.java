package com.vegan.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface OrderApiService {

    @POST("/api/orders")
    Call<ApiOrder> createOrder(@Header("Authorization") String token,
                               @Body OrderCreateRequestBody request);

    @GET("/api/orders")
    Call<List<ApiOrder>> getOrders(@Header("Authorization") String token);

    @GET("/api/orders/{id}")
    Call<ApiOrder> getOrder(@Header("Authorization") String token,
                            @Path("id") long id);
}

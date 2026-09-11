package com.vegan.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface CartApiService {

    @GET("/api/cart")
    Call<List<ApiCartItem>> getCart(@Header("Authorization") String token);

    @POST("/api/cart")
    Call<ApiCartItem> addItem(@Header("Authorization") String token,
                              @Body Map<String, Object> body);

    @PUT("/api/cart/{itemId}")
    Call<ApiCartItem> updateQuantity(@Header("Authorization") String token,
                                     @Path("itemId") long itemId,
                                     @Body Map<String, Integer> body);

    @DELETE("/api/cart/{itemId}")
    Call<Void> deleteItem(@Header("Authorization") String token,
                          @Path("itemId") long itemId);

    @DELETE("/api/cart")
    Call<Void> clearCart(@Header("Authorization") String token);
}

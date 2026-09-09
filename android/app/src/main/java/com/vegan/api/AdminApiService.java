package com.vegan.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AdminApiService {

    @GET("/api/admin/users")
    Call<List<ApiUser>> getUsers(@Header("Authorization") String token);

    @GET("/api/admin/orders")
    Call<List<ApiOrder>> getAllOrders(@Header("Authorization") String token);

    @DELETE("/api/admin/orders/{id}")
    Call<Void> deleteOrder(@Header("Authorization") String token, @Path("id") long id);

    @PATCH("/api/admin/orders/{id}/state")
    Call<ApiOrder> updateOrderState(@Header("Authorization") String token, @Path("id") long id);

    @GET("/api/admin/reviews")
    Call<List<ApiReview>> getAllReviews(@Header("Authorization") String token);

    @DELETE("/api/admin/reviews/{id}")
    Call<Void> deleteReview(@Header("Authorization") String token, @Path("id") long id);

    @POST("/api/products")
    Call<ApiProduct> addProduct(@Header("Authorization") String token, @Body Map<String, Object> body);

    @PUT("/api/products/{id}")
    Call<ApiProduct> updateProduct(@Header("Authorization") String token, @Path("id") long id, @Body Map<String, Object> body);

    @DELETE("/api/products/{id}")
    Call<Void> deleteProduct(@Header("Authorization") String token, @Path("id") long id);
}

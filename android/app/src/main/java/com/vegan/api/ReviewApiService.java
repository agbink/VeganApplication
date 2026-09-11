package com.vegan.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ReviewApiService {

    @GET("/api/reviews")
    Call<List<ApiReview>> getReviews(@Query("productId") long productId);

    @GET("/api/reviews/my")
    Call<List<ApiReview>> getMyReviews(@Header("Authorization") String token);

    @POST("/api/reviews")
    Call<ApiReview> createReview(@Header("Authorization") String token,
                                 @Body Map<String, Object> body);

    @DELETE("/api/reviews/{reviewId}")
    Call<Void> deleteReview(@Header("Authorization") String token,
                            @Path("reviewId") long reviewId);
}

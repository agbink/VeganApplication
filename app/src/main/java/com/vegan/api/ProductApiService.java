package com.vegan.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApiService {

    @GET("/api/products")
    Call<List<ApiProduct>> getProducts(@Query("category") Integer category);

    @GET("/api/products/{id}")
    Call<ApiProduct> getProduct(@Path("id") long id);

    @GET("/api/products/best")
    Call<List<ApiProduct>> getBestSellers();
}

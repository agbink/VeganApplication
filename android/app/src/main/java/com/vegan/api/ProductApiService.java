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

    @GET("/api/products/search")
    Call<List<ApiProduct>> searchProducts(@Query("keyword") String keyword);

    // ===== 페이지네이션(무한 스크롤) 버전 =====

    @GET("/api/products")
    Call<PageResponse<ApiProduct>> getProductsPaged(@Query("category") Integer category,
                                                    @Query("page") int page,
                                                    @Query("size") int size);

    @GET("/api/products/best")
    Call<PageResponse<ApiProduct>> getBestSellersPaged(@Query("page") int page,
                                                       @Query("size") int size);

    @GET("/api/products/search")
    Call<PageResponse<ApiProduct>> searchProductsPaged(@Query("keyword") String keyword,
                                                       @Query("page") int page,
                                                       @Query("size") int size);
}

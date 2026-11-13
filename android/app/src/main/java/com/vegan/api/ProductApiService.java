package com.vegan.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApiService {

    @GET("/api/products/{id}")
    Call<ApiProduct> getProduct(@Path("id") long id);

    // 목록 API는 모두 페이지 단위로만 조회한다 (서버도 page 생략 시 첫 페이지만 반환)

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

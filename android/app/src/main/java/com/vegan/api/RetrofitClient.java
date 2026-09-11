package com.vegan.api;

import com.vegan.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // 기본값은 에뮬레이터용 10.0.2.2이며, 로컬 gradle.properties의
    // VEGAN_API_BASE_URL로 실제 기기/배포 환경 주소를 덮어쓸 수 있습니다.
    private static final String BASE_URL = BuildConfig.VEGAN_API_BASE_URL;

    private static Retrofit retrofit;

    public static Retrofit getInstance() {
        if (retrofit == null) {
            // AuthInterceptor가 로그인 토큰이 있으면 모든 요청에 자동으로 붙여줌
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor())
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ProductApiService getProductApi() {
        return getInstance().create(ProductApiService.class);
    }

    public static OrderApiService getOrderApi() {
        return getInstance().create(OrderApiService.class);
    }

    public static AuthApiService getAuthApi() {
        return getInstance().create(AuthApiService.class);
    }

    public static CartApiService getCartApi() {
        return getInstance().create(CartApiService.class);
    }

    public static ReviewApiService getReviewApi() {
        return getInstance().create(ReviewApiService.class);
    }

    public static UserApiService getUserApi() {
        return getInstance().create(UserApiService.class);
    }

    public static AdminApiService getAdminApi() {
        return getInstance().create(AdminApiService.class);
    }

    public static UploadApiService getUploadApi() {
        return getInstance().create(UploadApiService.class);
    }
}

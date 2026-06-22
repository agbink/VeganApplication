package com.vegan.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // 에뮬레이터에서 PC의 로컬 Spring Boot 서버에 접속할 때는 10.0.2.2 사용
    // 실제 기기로 테스트할 때는 PC와 같은 와이파이에 연결한 뒤, PC의 IP 주소로 변경하세요 (예: 192.168.0.10)
    private static final String BASE_URL = "http://10.0.2.2:8080";

    private static Retrofit retrofit;

    public static Retrofit getInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ProductApiService getProductApi() {
        return getInstance().create(ProductApiService.class);
    }
}

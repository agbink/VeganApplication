package com.vegan.api;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 모든 Retrofit 요청에 자동으로 "Authorization: Bearer {토큰}" 헤더를 붙여줍니다.
 * 로그인 안 한 상태(토큰 없음)면 그냥 헤더 없이 보냅니다.
 */
public class AuthInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String token = TokenManager.getInstance().getToken();

        if (token == null) {
            return chain.proceed(original);
        }

        Request authorized = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(authorized);
    }
}

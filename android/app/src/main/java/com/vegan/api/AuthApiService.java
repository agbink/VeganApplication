package com.vegan.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("/api/auth/register")
    Call<ApiAuthResponse> register(@Body RegisterRequestBody body);

    @POST("/api/auth/login")
    Call<ApiAuthResponse> login(@Body LoginRequestBody body);

    @POST("/api/auth/google")
    Call<ApiAuthResponse> loginWithGoogle(@Body SocialLoginRequestBody body);

    @POST("/api/auth/naver")
    Call<ApiAuthResponse> loginWithNaver(@Body SocialLoginRequestBody body);
}

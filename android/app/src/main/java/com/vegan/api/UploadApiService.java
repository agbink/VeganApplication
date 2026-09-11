package com.vegan.api;

import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadApiService {

    @Multipart
    @POST("/api/upload")
    Call<Map<String, String>> uploadImage(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image
    );
}

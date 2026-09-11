package com.vegan.api;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 갤러리에서 고른 이미지(Uri)를 서버에 업로드하고 URL을 콜백으로 반환합니다.
 *
 * 사용 예:
 *   ImageUploadUtil.upload(context, imageUri, token, url -> {
 *       // url = "http://서버/uploads/abc.jpg"
 *   }, () -> { /* 실패 처리 *\/ });
 */
public class ImageUploadUtil {

    public interface OnSuccess { void onUrl(String url); }
    public interface OnFailure { void onFail(); }

    public static void upload(Context context, Uri imageUri, String token,
                              OnSuccess onSuccess, OnFailure onFailure) {
        try {
            // Uri → 임시 File 변환 (Retrofit Multipart에 File이 필요)
            File file = uriToFile(context, imageUri);

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("image/*"), file);
            MultipartBody.Part part = MultipartBody.Part.createFormData(
                    "image", file.getName(), requestBody);

            RetrofitClient.getUploadApi()
                    .uploadImage(token, part)
                    .enqueue(new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(Call<Map<String, String>> call,
                                               Response<Map<String, String>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                String url = response.body().get("url");
                                if (url != null) onSuccess.onUrl(url);
                                else onFailure.onFail();
                            } else {
                                onFailure.onFail();
                            }
                        }
                        @Override
                        public void onFailure(Call<Map<String, String>> call, Throwable t) {
                            onFailure.onFail();
                        }
                    });
        } catch (Exception e) {
            onFailure.onFail();
        }
    }

    /** Content URI → 캐시 디렉토리의 임시 File */
    private static File uriToFile(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        String ext = getExtension(context, uri);
        File tempFile = File.createTempFile("upload_", ext, context.getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        inputStream.close();
        return tempFile;
    }

    private static String getExtension(Context context, Uri uri) {
        String mime = context.getContentResolver().getType(uri);
        if (mime == null) return ".jpg";
        switch (mime) {
            case "image/png":  return ".png";
            case "image/gif":  return ".gif";
            case "image/webp": return ".webp";
            default:           return ".jpg";
        }
    }
}

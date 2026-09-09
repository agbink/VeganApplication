package com.vegan.api;

// 구글이면 idToken, 네이버면 accessToken을 그대로 token에 담아 보냄
public class SocialLoginRequestBody {
    private String token;

    public SocialLoginRequestBody(String token) {
        this.token = token;
    }
}

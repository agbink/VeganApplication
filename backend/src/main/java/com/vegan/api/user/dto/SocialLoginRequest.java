package com.vegan.api.user.dto;

// Google: idToken / Naver: accessToken - 둘 다 같은 필드명(token)으로 받음
public class SocialLoginRequest {
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

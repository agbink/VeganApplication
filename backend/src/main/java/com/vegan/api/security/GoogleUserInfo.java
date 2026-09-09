package com.vegan.api.security;

// Google tokeninfo 응답에서 뽑아낸 최소 정보
public class GoogleUserInfo {
    private final String email;
    private final String name;

    public GoogleUserInfo(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}

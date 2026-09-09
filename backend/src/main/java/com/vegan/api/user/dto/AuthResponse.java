package com.vegan.api.user.dto;

// 회원가입/로그인 성공 시 클라이언트에게 돌려주는 응답
public class AuthResponse {
    private final String token;
    private final Long userId;
    private final String username;
    private final String email;

    public AuthResponse(String token, Long userId, String username, String email) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}

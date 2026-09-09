package com.vegan.api;

public class ApiAuthResponse {
    private String token;
    private long userId;
    private String username;
    private String email;
    private boolean admin;

    public String getToken() { return token; }
    public long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return admin; }
}

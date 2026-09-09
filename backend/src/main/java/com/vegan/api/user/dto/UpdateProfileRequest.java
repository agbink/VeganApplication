package com.vegan.api.user.dto;

public class UpdateProfileRequest {
    private String username;
    private String phone;
    private String address;

    public String getUsername() { return username; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
}

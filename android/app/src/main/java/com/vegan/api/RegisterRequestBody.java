package com.vegan.api;

public class RegisterRequestBody {
    private String email;
    private String password;
    private String username;
    private String phone;
    private String address;

    public RegisterRequestBody(String email, String password, String username, String phone, String address) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.phone = phone;
        this.address = address;
    }
}

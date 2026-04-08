package com.vegan;

import java.io.Serializable;

public class User implements Serializable {
    private String idToken;
    private String emailId;
    private String password;
    private String username;
    private String phone;
    private String address;



    public User() {}

    public User(String username, String emailId, String phoneNumber) {
        this.username = username;
        this.emailId = emailId;
        this.phone = phoneNumber;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
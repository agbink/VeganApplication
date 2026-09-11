package com.vegan.api.security;

import lombok.Getter;

@Getter
public class NaverUserInfo {
    private final String email;
    private final String name;
    private final String mobile;

    public NaverUserInfo(String email, String name, String mobile) {
        this.email = email;
        this.name = name;
        this.mobile = mobile;
    }
}

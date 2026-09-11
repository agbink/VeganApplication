package com.vegan.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 전체 Spring Security 프레임워크는 안 쓰고, 비밀번호 해시 기능만 가볍게 가져다 씁니다.
// (spring-boot-starter-security를 쓰면 모든 API에 기본 로그인 화면이 걸려버려서 일부러 안 씀)
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

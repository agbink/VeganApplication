package com.vegan.api.user;

import com.vegan.api.user.dto.AuthResponse;
import com.vegan.api.user.dto.LoginRequest;
import com.vegan.api.user.dto.RegisterRequest;
import com.vegan.api.user.dto.SocialLoginRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/register -> 회원가입 (RegisterActivity)
    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // POST /api/auth/login -> 로그인 (LoginActivity)
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // POST /api/auth/google -> 구글 로그인. body.token = Google Sign-In SDK가 발급한 idToken
    @PostMapping("/google")
    public AuthResponse google(@RequestBody SocialLoginRequest request) {
        return authService.loginWithGoogle(request.getToken());
    }

    // POST /api/auth/naver -> 네이버 로그인. body.token = NaverIdLoginSDK가 발급한 accessToken
    @PostMapping("/naver")
    public AuthResponse naver(@RequestBody SocialLoginRequest request) {
        return authService.loginWithNaver(request.getToken());
    }
}

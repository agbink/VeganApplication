package com.vegan.api.user;

import com.vegan.api.admin.AdminAuthorizationService;
import com.vegan.api.security.GoogleTokenVerifier;
import com.vegan.api.security.GoogleUserInfo;
import com.vegan.api.security.JwtTokenProvider;
import com.vegan.api.security.NaverProfileFetcher;
import com.vegan.api.security.NaverUserInfo;
import com.vegan.api.user.dto.AuthResponse;
import com.vegan.api.user.dto.LoginRequest;
import com.vegan.api.user.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final NaverProfileFetcher naverProfileFetcher;
    private final AdminAuthorizationService adminAuthorizationService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider, GoogleTokenVerifier googleTokenVerifier,
                        NaverProfileFetcher naverProfileFetcher,
                        AdminAuthorizationService adminAuthorizationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleTokenVerifier = googleTokenVerifier;
        this.naverProfileFetcher = naverProfileFetcher;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getEmail(),
                hashedPassword,
                request.getUsername(),
                request.getPhone(),
                request.getAddress()
        );
        User saved = userRepository.save(user);

        String token = jwtTokenProvider.createToken(saved.getId(), saved.getEmail());
        return authResponse(token, saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        return authResponse(token, user);
    }

    // 구글 로그인 - idToken은 서버가 직접 구글에 검증을 요청함
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleUserInfo info = googleTokenVerifier.verify(idToken);

        User user = userRepository.findByEmail(info.getEmail())
                .orElseGet(() -> userRepository.save(
                        new User(info.getEmail(), info.getName(), null, AuthProvider.GOOGLE)));

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        return authResponse(token, user);
    }

    // 네이버 로그인 - accessToken을 서버가 직접 네이버 프로필 API로 검증함
    @Transactional
    public AuthResponse loginWithNaver(String accessToken) {
        NaverUserInfo info = naverProfileFetcher.fetch(accessToken);

        User user = userRepository.findByEmail(info.getEmail())
                .orElseGet(() -> userRepository.save(
                        new User(info.getEmail(), info.getName(), info.getMobile(), AuthProvider.NAVER)));

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        return authResponse(token, user);
    }

    private AuthResponse authResponse(String token, User user) {
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(),
                adminAuthorizationService.isAdmin(user));
    }
}

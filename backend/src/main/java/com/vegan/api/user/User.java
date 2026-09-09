package com.vegan.api.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users") // "user"는 mysql 내부 스키마와 헷갈릴 수 있어 복수형 사용
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // 평문 저장 절대 금지 - BCrypt로 해시된 값만 들어옴. 소셜 로그인 계정은 비밀번호가 없을 수 있어 null 허용
    @Column(nullable = true)
    private String password;

    private String username;
    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider; // LOCAL, GOOGLE, NAVER

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (provider == null) {
            provider = AuthProvider.LOCAL;
        }
    }

    protected User() {
        // JPA 기본 생성자
    }

    public User(String email, String password, String username, String phone, String address) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.phone = phone;
        this.address = address;
        this.provider = AuthProvider.LOCAL;
    }

    // 소셜 로그인(Google/Naver)으로 처음 가입하는 경우 - 비밀번호 없음
    public User(String email, String username, String phone, AuthProvider provider) {
        this.email = email;
        this.username = username;
        this.phone = phone;
        this.provider = provider;
    }

    // 회원정보 수정 (MyPageActivity, ChangeActivity에서 사용)
    public void updateProfile(String username, String phone, String address) {
        this.username = username;
        this.phone = phone;
        this.address = address;
    }

    // 비밀번호 변경 - 호출하는 쪽에서 이미 해시된 값을 넘겨줘야 함
    public void changePassword(String newHashedPassword) {
        this.password = newHashedPassword;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AuthProvider getProvider() {
        return provider;
    }
}

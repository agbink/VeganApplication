package com.vegan.api.user;

// 이 계정이 어떤 방식으로 가입했는지 구분 (Firebase Auth가 내부적으로 하던 일)
public enum AuthProvider {
    LOCAL,   // 이메일/비밀번호 직접 가입
    GOOGLE,  // 구글 로그인
    NAVER    // 네이버 로그인
}

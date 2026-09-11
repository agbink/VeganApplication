package com.vegan.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final RestTemplate restTemplate = new RestTemplate();

    // strings.xml의 default_web_client_id와 반드시 동일한 값이어야 함
    @Value("${google.client-id}")
    private String expectedClientId;

    @SuppressWarnings("unchecked")
    public GoogleUserInfo verify(String idToken) {
        Map<String, Object> response;
        try {
            response = restTemplate.getForObject(TOKEN_INFO_URL + idToken, Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "구글 토큰이 유효하지 않습니다.");
        }

        if (response == null || !expectedClientId.equals(response.get("aud"))) {
            // aud가 우리 앱의 클라이언트 ID와 다르면, 다른 앱용으로 발급된 토큰일 수 있어 거부
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "구글 토큰 검증에 실패했습니다.");
        }

        String email = (String) response.get("email");
        String name = (String) response.get("name");
        log.info("구글 토큰 검증 결과 - email: {}, name: {}, 전체응답: {}", email, name, response);
        // name이 없으면 이메일 앞부분(@ 이전)을 닉네임으로 사용
        if (name == null || name.isEmpty()) {
            name = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        }
        return new GoogleUserInfo(email, name);
    }
}

package com.vegan.api.security;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 안드로이드에서 NaverIdLoginSDK로 받은 accessToken을 서버가 직접 검증합니다.
 * (기존엔 안드로이드 앱이 직접 네이버 API를 호출했는데, 보안상 서버에서 하는 게 더 안전합니다)
 *
 * accessToken을 Authorization 헤더에 담아 네이버 프로필 API를 호출하면,
 * 그 토큰이 진짜인지 확인되면서 동시에 사용자 정보(name, email, mobile)도 받아옵니다.
 */
@Component
public class NaverProfileFetcher {

    private static final String PROFILE_URL = "https://openapi.naver.com/v1/nid/me";

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public NaverUserInfo fetch(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(PROFILE_URL, HttpMethod.GET, entity, Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "네이버 토큰이 유효하지 않습니다.");
        }

        Map<String, Object> body = response.getBody();
        if (body == null || !"00".equals(body.get("resultcode"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "네이버 토큰 검증에 실패했습니다.");
        }

        Map<String, Object> profile = (Map<String, Object>) body.get("response");
        String email = (String) profile.get("email");
        String nickname = (String) profile.get("nickname");
        String name = (nickname != null && !nickname.isEmpty()) ? nickname : (String) profile.get("name");
        String mobile = (String) profile.get("mobile");
        return new NaverUserInfo(email, name, mobile);
    }
}

package com.vegan.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 모든 요청에 대해 Authorization 헤더를 확인하고, 유효한 토큰이면
 * request에 "userId" 속성을 심어줍니다 (없으면 그냥 통과 - 로그인 필요 없는 API도 있으니까).
 *
 * Controller에서 로그인이 필요한 API는 이렇게 받으면 됩니다:
 *   @GetMapping("/something")
 *   public X method(@RequestAttribute(required = false) Long userId) {
 *       if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
 *       ...
 *   }
 *
 * 전체 Spring Security 없이 가장 단순한 형태로 만든 필터입니다.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && !header.isBlank()) {
            try {
                Long userId = jwtTokenProvider.getUserId(header);
                request.setAttribute("userId", userId);
            } catch (Exception e) {
                // 토큰이 없거나 잘못됐어도 여기서는 막지 않음.
                // 로그인이 꼭 필요한 API는 Controller에서 userId == null 체크로 막습니다.
            }
        }

        filterChain.doFilter(request, response);
    }
}

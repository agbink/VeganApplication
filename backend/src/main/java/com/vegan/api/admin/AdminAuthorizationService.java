package com.vegan.api.admin;

import com.vegan.api.user.User;
import com.vegan.api.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthorizationService {

    private final UserRepository userRepository;
    private final String adminEmail;

    public AdminAuthorizationService(UserRepository userRepository,
                                     @Value("${app.admin-email:}") String adminEmail) {
        this.userRepository = userRepository;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim();
    }

    public void requireAdmin(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 사용자입니다."));
        if (adminEmail.isBlank() || !adminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
    }

    public boolean isAdmin(User user) {
        return user != null && !adminEmail.isBlank() && adminEmail.equalsIgnoreCase(user.getEmail());
    }
}

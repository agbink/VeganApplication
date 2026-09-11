package com.vegan.api.user;

import com.vegan.api.user.dto.ChangePasswordRequest;
import com.vegan.api.user.dto.UpdateProfileRequest;
import com.vegan.api.security.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // GET /api/users/me - 내 정보 조회
    @GetMapping("/me")
    public User getMe(@RequestHeader("Authorization") String authorization) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        return userService.getMe(userId);
    }

    // PUT /api/users/me - 프로필 수정 (이름, 전화번호, 주소)
    @PutMapping("/me")
    public User updateProfile(@RequestHeader("Authorization") String authorization,
                              @RequestBody UpdateProfileRequest request) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        return userService.updateProfile(userId, request);
    }

    // PUT /api/users/me/password - 비밀번호 변경
    @PutMapping("/me/password")
    public void changePassword(@RequestHeader("Authorization") String authorization,
                               @RequestBody ChangePasswordRequest request) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        userService.changePassword(userId, request);
    }

    // DELETE /api/users/me - 회원 탈퇴
    @DeleteMapping("/me")
    public void deleteAccount(@RequestHeader("Authorization") String authorization) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        userService.deleteAccount(userId);
    }
}

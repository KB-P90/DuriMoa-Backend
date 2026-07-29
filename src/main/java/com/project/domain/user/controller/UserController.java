package com.project.domain.user.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.user.dto.UserProfileDto;
import com.project.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Log4j2
@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(
            @CookieValue(value = "accessToken", required = false) String accessToken,
            @RequestBody(required = false) Long userIdFromBody) {

        Long userId;
        if (accessToken != null) {
            log.warn("프로필 조회 실패 - 존재하지 않는 회원");
            userId = userIdFromBody;

            // userId = getUserIdFromToken(accessToken);
        } else {
            // TODO: 인증(JWT) 완성되면 이 else 블록 삭제
            userId = userIdFromBody;
        }

        UserProfileDto profile = userService.get(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // TODO: 팀원이 작업 중인 JWT 인증 로직 완성되면 그 코드로 교체
    private Long getUserIdFromToken(String token) {
        throw new UnsupportedOperationException("JWT 파싱 미구현");
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> update(@RequestBody UserProfileDto profile) {
        UserProfileDto updated = userService.update(profile);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }


}

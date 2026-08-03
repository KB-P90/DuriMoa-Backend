package com.project.domain.user.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.user.dto.UserProfileDto;
import com.project.domain.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Api(tags = "User", description = "유저 API")
public class UserController {

    private final UserService userService;

    @ApiOperation(value = "마이페이지용 사용자 정보 조회", notes = "현재 쿠키 전달 받는 로직 부재로 body에 사용자 id 값만 전달 부탁드립니다.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(

            // @AuthenticationPrincipal UserDetails userDetail,

            @RequestBody(required = false) Long userIdFromBody) {


        // 추후 시큐리티에서 userid 가져올것
        Long userId = 1l;

//        if (userDetail.getUsername() != null) {
//            log.warn("프로필 조회 실패 - 존재하지 않는 회원");
//
//        }

        // 해당 부분도 추후 변경 예정
        userId = userIdFromBody == null ? userId : userIdFromBody;


        UserProfileDto profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // TODO: 팀원이 작업 중인 JWT 인증 로직 완성되면 그 코드로 교체
    private Long getUserIdFromToken(String token) {
        throw new UnsupportedOperationException("JWT 파싱 미구현");
    }

    @PutMapping("/profile")
    @ApiOperation(value = "사용자 정보 수정")
    public ResponseEntity<ApiResponse<UserProfileDto>> update(@RequestBody UserProfileDto profile) {
        UserProfileDto updated = userService.updateProfile(profile);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }


}

package com.project.domain.user.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.user.dto.PhoneAvailabilityResponseDTO;
import com.project.domain.user.dto.UserProfileDto;
import com.project.domain.user.dto.UserProfileUpdateRequestDto;
import com.project.domain.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Api(tags = "User", description = "유저 API")
public class UserController {

    private final UserService userService;

    @ApiOperation(value = "마이페이지용 사용자 정보 조회")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(
            @ApiIgnore @AuthenticationPrincipal Long userId) {

        UserProfileDto profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회에 성공했습니다.", profile));
    }


    @PutMapping(value = "/profile", consumes = "multipart/form-data")
    @ApiOperation(value = "사용자 정보 수정")
    public ResponseEntity<ApiResponse<UserProfileDto>> update(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ModelAttribute UserProfileUpdateRequestDto request) {

        UserProfileDto updated = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("정보 수정에 성공했습니다.", updated));
    }

    @GetMapping("/phone-availability")
    @ApiOperation(value = "전화번호 중복 확인")
    public ResponseEntity<ApiResponse<PhoneAvailabilityResponseDTO>> checkPhoneAvailability(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @RequestParam String phone) {

        boolean available = userService.isPhoneAvailable(userId, phone);
        return ResponseEntity.ok(ApiResponse.success(
                "전화번호 사용 가능 여부를 조회했습니다.", new PhoneAvailabilityResponseDTO(available)));
    }

    @PatchMapping("/share")
    @ApiOperation(value = "상대방 공유 여부 토글")
    public ResponseEntity<ApiResponse<Boolean>> updateIsShared(
            @ApiIgnore @AuthenticationPrincipal Long userId) {

        boolean result = userService.updateIsShared(userId);
        return ResponseEntity.ok(ApiResponse.success("공유 여부가 변경되었습니다.", result));
    }

    @GetMapping("/invite")
    @ApiOperation(value = "초대 코드 조회")
    public ResponseEntity<ApiResponse<String>> getUserInvite(
            @ApiIgnore @AuthenticationPrincipal Long userId) {

        String inviteCode = userService.getUserInvite(userId);
        return ResponseEntity.ok(ApiResponse.success("조회에 성공했습니다", inviteCode));
    }
}

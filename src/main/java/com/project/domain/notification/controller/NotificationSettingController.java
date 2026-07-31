package com.project.domain.notification.controller;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.notification.dto.NotificationSettingResponseDTO;
import com.project.domain.notification.dto.NotificationSettingUpdateRequestDTO;
import com.project.domain.notification.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "Notification Setting", description = "알림 설정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification-settings")
public class NotificationSettingController {

    private final NotificationService notificationService;

    @ApiOperation(value = "내 알림 설정 조회", notes = "인증 사용자의 알림 설정 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationSettingResponseDTO>>> getSettings(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        List<NotificationSettingResponseDTO> response =
                notificationService.getSettings(authenticatedUserId);
        return ResponseEntity.ok(ApiResponse.success("알림 설정 조회에 성공했습니다.", response));
    }

    @ApiOperation(value = "알림 설정 수정", notes = "인증 사용자의 특정 알림 종류 수신 여부를 저장합니다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateSetting(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @Valid @RequestBody NotificationSettingUpdateRequestDTO request
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        notificationService.updateSetting(authenticatedUserId, request);
        return ResponseEntity.ok(ApiResponse.success("알림 설정을 저장했습니다."));
    }

    private Long resolveAuthenticatedUserId(Long userId, Authentication authentication) {
        if (userId != null && userId > 0) {
            return userId;
        }
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Number) {
                Long authenticationUserId = ((Number) principal).longValue();
                if (authenticationUserId > 0) {
                    return authenticationUserId;
                }
            }
        }
        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}

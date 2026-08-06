package com.project.domain.notification.controller;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.notification.dto.NotificationListResponseDTO;
import com.project.domain.notification.dto.NotificationReadAllResponseDTO;
import com.project.domain.notification.dto.NotificationReadResponseDTO;
import com.project.domain.notification.dto.NotificationUnreadCountResponseDTO;
import com.project.domain.notification.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @ApiOperation(value = "내 알림 목록 조회", notes = "인증 사용자의 알림 목록을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponseDTO>> getNotifications(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "isRead", required = false) Boolean isRead
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        NotificationListResponseDTO response =
                notificationService.getNotifications(authenticatedUserId, page, size, isRead);
        return ResponseEntity.ok(ApiResponse.success("알림 목록 조회에 성공했습니다.", response));
    }

    @ApiOperation(value = "읽지 않은 알림 개수 조회", notes = "인증 사용자의 읽지 않은 알림 개수를 조회합니다.")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponseDTO>> getUnreadCount(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        NotificationUnreadCountResponseDTO response =
                notificationService.getUnreadCount(authenticatedUserId);
        return ResponseEntity.ok(ApiResponse.success("읽지 않은 알림 개수 조회에 성공했습니다.", response));
    }

    @ApiOperation(value = "알림 읽음 처리", notes = "인증 사용자의 특정 알림을 읽음 상태로 변경합니다.")
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NotificationReadResponseDTO>> markAsRead(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication,
            @PathVariable("noticeId") Long noticeId
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        NotificationReadResponseDTO response =
                notificationService.markAsRead(authenticatedUserId, noticeId);
        return ResponseEntity.ok(ApiResponse.success("알림을 삭제했습니다.", response));
    }

    @ApiOperation(value = "전체 알림 읽음 처리", notes = "인증 사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<NotificationReadAllResponseDTO>> markAllAsRead(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiIgnore Authentication authentication
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        NotificationReadAllResponseDTO response =
                notificationService.markAllAsRead(authenticatedUserId);
        return ResponseEntity.ok(ApiResponse.success("모든 알림을 삭제했습니다.", response));
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

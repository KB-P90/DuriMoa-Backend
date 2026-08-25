package com.project.domain.notification.service;

import com.project.domain.notification.domain.NotificationType;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.dto.NotificationDeleteBatchRequestDTO;
import com.project.domain.notification.dto.NotificationDeleteBatchResponseDTO;
import com.project.domain.notification.dto.NotificationDeleteResponseDTO;
import com.project.domain.notification.dto.NotificationListResponseDTO;
import com.project.domain.notification.dto.NotificationReadAllResponseDTO;
import com.project.domain.notification.dto.NotificationReadResponseDTO;
import com.project.domain.notification.dto.NotificationResponseDTO;
import com.project.domain.notification.dto.NotificationSettingBatchUpdateRequestDTO;
import com.project.domain.notification.dto.NotificationSettingResponseDTO;
import com.project.domain.notification.dto.NotificationUnreadCountResponseDTO;
import java.util.List;

public interface NotificationService {

    /**
     * 인증 사용자의 알림 목록을 최신순으로 조회합니다.
     */
    NotificationListResponseDTO getNotifications(
            Long authenticatedUserId,
            Integer page,
            Integer size,
            Boolean isRead
    );

    /**
     * 인증 사용자의 알림 목록을 최신순으로 조회합니다.
     */
    List<NotificationResponseDTO> getNotifications(Long authenticatedUserId);

    /**
     * 인증 사용자의 읽지 않은 알림 개수를 조회합니다.
     */
    NotificationUnreadCountResponseDTO getUnreadCount(Long authenticatedUserId);

    /**
     * 인증 사용자의 특정 알림을 읽음 상태로 변경합니다.
     */
    NotificationReadResponseDTO markAsRead(Long authenticatedUserId, Long notificationId);

    /**
     * 인증 사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.
     */
    NotificationReadAllResponseDTO markAllAsRead(Long authenticatedUserId);

    /**
     * 인증 사용자의 특정 알림을 삭제합니다.
     */
    NotificationDeleteResponseDTO deleteNotification(Long authenticatedUserId, Long notificationId);

    /**
     * 인증 사용자의 선택된 알림들을 삭제합니다.
     */
    NotificationDeleteBatchResponseDTO deleteNotifications(
            Long authenticatedUserId,
            NotificationDeleteBatchRequestDTO request
    );

    /**
     * 인증 사용자의 알림 종류별 수신 설정 목록을 조회합니다.
     */
    List<NotificationSettingResponseDTO> getSettings(Long authenticatedUserId);

    /**
     * 인증 사용자의 알림 종류별 수신 설정을 한 번에 저장하거나 수정합니다.
     */
    void updateSettings(
            Long authenticatedUserId,
            NotificationSettingBatchUpdateRequestDTO request
    );

    /**
     * 다른 도메인에서 발생한 비즈니스 이벤트를 알림으로 저장합니다.
     * 사용자가 해당 알림 종류를 비활성화한 경우 저장하지 않고 null을 반환합니다.
     */
    Long createNotification(NotificationCreateRequestDTO request);

    /**
     * 오늘 이미 동일한 유저·종류·대상으로 발송된 알림이 있는지 확인합니다.
     */
    boolean existsNotificationToday(Long userId, NotificationType type, Long targetId);
}

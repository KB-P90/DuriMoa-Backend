package com.project.domain.notification.service;

import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.dto.NotificationListResponseDTO;
import com.project.domain.notification.dto.NotificationReadAllResponseDTO;
import com.project.domain.notification.dto.NotificationReadResponseDTO;
import com.project.domain.notification.dto.NotificationResponseDTO;
import com.project.domain.notification.dto.NotificationSettingResponseDTO;
import com.project.domain.notification.dto.NotificationSettingUpdateRequestDTO;
import com.project.domain.notification.dto.NotificationUnreadCountResponseDTO;
import java.util.List;

public interface NotificationService {

    /**
     * 인증 사용자의 알림 목록을 최신순으로 조회합니다.
     *
     * @param authenticatedUserId JWT 인증으로 확인된 사용자 ID
     * @return 사용자의 알림 목록
     */
    NotificationListResponseDTO getNotifications(
            Long authenticatedUserId,
            Integer page,
            Integer size,
            Boolean isRead
    );

    List<NotificationResponseDTO> getNotifications(Long authenticatedUserId);

    /**
     * 인증 사용자의 읽지 않은 알림 개수를 조회합니다.
     *
     * @param authenticatedUserId JWT 인증으로 확인된 사용자 ID
     * @return 읽지 않은 알림 개수 응답
     */
    NotificationUnreadCountResponseDTO getUnreadCount(Long authenticatedUserId);

    /**
     * 인증 사용자의 특정 알림을 읽음 상태로 변경합니다.
     *
     * @param authenticatedUserId JWT 인증으로 확인된 사용자 ID
     * @param notificationId 읽음 처리할 알림 ID
     */
    NotificationReadResponseDTO markAsRead(Long authenticatedUserId, Long notificationId);

    /**
     * 인증 사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.
     *
     * @param authenticatedUserId JWT 인증으로 확인된 사용자 ID
     */
    NotificationReadAllResponseDTO markAllAsRead(Long authenticatedUserId);

    /**
     * 인증 사용자의 알림 종류별 수신 설정 목록을 조회합니다.
     *
     * @param authenticatedUserId JWT 인증으로 확인된 사용자 ID
     * @return 알림 설정 목록
     */
    List<NotificationSettingResponseDTO> getSettings(Long authenticatedUserId);

    /**
     * 인증 사용자의 특정 알림 종류 수신 설정을 저장하거나 수정합니다.
     *
     * @param authenticatedUserId JWT 인증으로 확인된 사용자 ID
     * @param request 알림 종류와 수신 활성화 여부
     */
    void updateSetting(
            Long authenticatedUserId,
            NotificationSettingUpdateRequestDTO request
    );

    /**
     * 다른 도메인에서 발생한 비즈니스 이벤트를 알림으로 저장합니다.
     * 사용자가 해당 알림 종류를 비활성화한 경우 저장하지 않고 null을 반환합니다.
     *
     * @param request 알림 수신자, 종류, 제목, 내용
     * @return 저장된 알림 ID, 수신 설정이 꺼져 있으면 null
     */
    Long createNotification(NotificationCreateRequestDTO request);
}

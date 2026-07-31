package com.project.domain.notification.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.notification.domain.NotificationVO;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.dto.NotificationResponseDTO;
import com.project.domain.notification.dto.NotificationSettingResponseDTO;
import com.project.domain.notification.dto.NotificationSettingUpdateRequestDTO;
import com.project.domain.notification.dto.NotificationUnreadCountResponseDTO;
import com.project.domain.notification.mapper.NotificationMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Integer TYPE_MAX_LENGTH = 10;
    private static final Integer TITLE_MAX_LENGTH = 100;

    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotifications(Long authenticatedUserId) {
        validateUserId(authenticatedUserId);
        List<NotificationResponseDTO> notifications =
                notificationMapper.selectNotificationsByUserId(authenticatedUserId);
        return notifications == null ? Collections.emptyList() : notifications;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationUnreadCountResponseDTO getUnreadCount(Long authenticatedUserId) {
        validateUserId(authenticatedUserId);
        Integer unreadCount = notificationMapper.countUnreadNotifications(authenticatedUserId);
        return NotificationUnreadCountResponseDTO.builder()
                .unreadCount(unreadCount == null ? 0 : unreadCount)
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(Long authenticatedUserId, Long notificationId) {
        validateUserId(authenticatedUserId);
        if (notificationId == null || notificationId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "알림 ID가 올바르지 않습니다.");
        }

        Integer updatedCount =
                notificationMapper.updateNotificationRead(authenticatedUserId, notificationId);
        if (updatedCount == null || updatedCount != 1) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "읽음 처리할 알림을 찾을 수 없습니다.");
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long authenticatedUserId) {
        validateUserId(authenticatedUserId);
        notificationMapper.updateAllNotificationsRead(authenticatedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationSettingResponseDTO> getSettings(Long authenticatedUserId) {
        validateUserId(authenticatedUserId);
        List<NotificationSettingResponseDTO> settings =
                notificationMapper.selectNotificationSettingsByUserId(authenticatedUserId);
        return settings == null ? Collections.emptyList() : settings;
    }

    @Override
    @Transactional
    public void updateSetting(
            Long authenticatedUserId,
            NotificationSettingUpdateRequestDTO request
    ) {
        validateUserId(authenticatedUserId);
        validateSettingRequest(request);

        Integer updatedCount = notificationMapper.upsertNotificationSetting(
                authenticatedUserId,
                request.getType().trim(),
                request.getPushEnabled()
        );
        if (updatedCount == null || updatedCount < 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "알림 설정 저장에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public Long createNotification(NotificationCreateRequestDTO request) {
        validateCreateRequest(request);
        Boolean pushEnabled = notificationMapper.selectNotificationSettingEnabled(
                request.getUserId(),
                request.getType().trim()
        );
        if (Boolean.FALSE.equals(pushEnabled)) {
            return null;
        }

        NotificationVO notification = NotificationVO.builder()
                .userId(request.getUserId())
                .type(request.getType().trim())
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .build();
        Integer insertedCount = notificationMapper.insertNotification(notification);
        if (insertedCount == null || insertedCount != 1 || notification.getId() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "알림 저장에 실패했습니다.");
        }
        return notification.getId();
    }

    private void validateCreateRequest(NotificationCreateRequestDTO request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "알림 생성 정보가 필요합니다.");
        }
        validateUserId(request.getUserId());
        validateType(request.getType());
        validateText(request.getTitle(), TITLE_MAX_LENGTH, "알림 제목");
        validateText(request.getContent(), null, "알림 내용");
    }

    private void validateSettingRequest(NotificationSettingUpdateRequestDTO request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "알림 설정 정보가 필요합니다.");
        }
        validateType(request.getType());
        if (request.getPushEnabled() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "푸시 알림 활성화 여부는 필수입니다.");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateType(String type) {
        validateText(type, TYPE_MAX_LENGTH, "알림 종류");
    }

    private void validateText(String value, Integer maxLength, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    fieldName + "는 필수입니다."
            );
        }
        if (maxLength != null && value.trim().length() > maxLength) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    fieldName + "는 " + maxLength + "자 이하여야 합니다."
            );
        }
    }
}

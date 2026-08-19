package com.project.domain.notification.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.util.FcmSender;
import com.project.domain.notification.domain.NotificationType;
import com.project.domain.notification.domain.NotificationVO;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.dto.NotificationDeleteBatchRequestDTO;
import com.project.domain.notification.dto.NotificationDeleteBatchResponseDTO;
import com.project.domain.notification.dto.NotificationDeleteResponseDTO;
import com.project.domain.notification.dto.NotificationListResponseDTO;
import com.project.domain.notification.dto.NotificationReadAllResponseDTO;
import com.project.domain.notification.dto.NotificationReadResponseDTO;
import com.project.domain.notification.dto.NotificationResponseDTO;
import com.project.domain.notification.dto.NotificationSettingResponseDTO;
import com.project.domain.notification.dto.NotificationSettingUpdateRequestDTO;
import com.project.domain.notification.dto.NotificationUnreadCountResponseDTO;
import com.project.domain.notification.mapper.NotificationMapper;
import com.project.domain.notification.sse.NotificationEmitterRegistry;
import com.project.domain.pushdevice.service.PushDeviceService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Integer TITLE_MAX_LENGTH = 100;
    private static final Integer TARGET_TYPE_MAX_LENGTH = 20;
    private static final Integer DEFAULT_PAGE = 0;
    private static final Integer DEFAULT_SIZE = 20;
    private static final Integer MAX_SIZE = 100;

    private final NotificationMapper notificationMapper;
    private final PushDeviceService pushDeviceService;
    private final FcmSender fcmSender;
    private final NotificationEmitterRegistry notificationEmitterRegistry;

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponseDTO getNotifications(
            Long authenticatedUserId,
            Integer page,
            Integer size,
            Boolean isRead
    ) {
        validateUserId(authenticatedUserId);
        Integer resolvedPage = resolvePage(page);
        Integer resolvedSize = resolveSize(size);
        Integer offset = resolvedPage * resolvedSize;

        List<NotificationResponseDTO> notifications = notificationMapper.selectNotifications(
                authenticatedUserId,
                isRead,
                resolvedSize,
                offset
        );
        Integer totalElements = notificationMapper.countNotifications(authenticatedUserId, isRead);
        Integer unreadCount = notificationMapper.countUnreadNotifications(authenticatedUserId);
        int safeTotalElements = totalElements == null ? 0 : totalElements;

        return NotificationListResponseDTO.builder()
                .notices(notifications == null ? Collections.emptyList() : notifications)
                .page(resolvedPage)
                .size(resolvedSize)
                .totalElements(safeTotalElements)
                .hasNext(offset + resolvedSize < safeTotalElements)
                .unreadCount(unreadCount == null ? 0 : unreadCount)
                .build();
    }

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
    public NotificationReadResponseDTO markAsRead(Long authenticatedUserId, Long notificationId) {
        validateUserId(authenticatedUserId);
        validateNotificationId(notificationId);
        validateNotificationExists(authenticatedUserId, notificationId);

        Integer updatedCount = notificationMapper.updateNotificationRead(
                authenticatedUserId,
                notificationId
        );
        if (updatedCount == null || updatedCount != 1) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "읽음 처리할 알림을 찾을 수 없습니다.");
        }
        Integer unreadCount = notificationMapper.countUnreadNotifications(authenticatedUserId);
        return NotificationReadResponseDTO.builder()
                .noticeId(notificationId)
                .isRead(true)
                .unreadCount(unreadCount == null ? 0 : unreadCount)
                .build();
    }

    @Override
    @Transactional
    public NotificationReadAllResponseDTO markAllAsRead(Long authenticatedUserId) {
        validateUserId(authenticatedUserId);
        Integer updatedCount = notificationMapper.updateAllNotificationsRead(authenticatedUserId);
        Integer unreadCount = notificationMapper.countUnreadNotifications(authenticatedUserId);
        return NotificationReadAllResponseDTO.builder()
                .updatedCount(updatedCount == null ? 0 : updatedCount)
                .unreadCount(unreadCount == null ? 0 : unreadCount)
                .build();
    }

    @Override
    @Transactional
    public NotificationDeleteResponseDTO deleteNotification(
            Long authenticatedUserId,
            Long notificationId
    ) {
        validateUserId(authenticatedUserId);
        validateNotificationId(notificationId);
        validateNotificationExists(authenticatedUserId, notificationId);

        Integer deletedCount = notificationMapper.deleteNotification(
                authenticatedUserId,
                notificationId
        );
        if (deletedCount == null || deletedCount != 1) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "삭제할 알림을 찾을 수 없습니다.");
        }
        Integer unreadCount = notificationMapper.countUnreadNotifications(authenticatedUserId);
        return NotificationDeleteResponseDTO.builder()
                .noticeId(notificationId)
                .unreadCount(unreadCount == null ? 0 : unreadCount)
                .build();
    }

    @Override
    @Transactional
    public NotificationDeleteBatchResponseDTO deleteNotifications(
            Long authenticatedUserId,
            NotificationDeleteBatchRequestDTO request
    ) {
        validateUserId(authenticatedUserId);
        List<Long> notificationIds = resolveNotificationIds(request);

        Integer deletedCount = notificationMapper.deleteNotifications(
                authenticatedUserId,
                notificationIds
        );
        Integer unreadCount = notificationMapper.countUnreadNotifications(authenticatedUserId);
        return NotificationDeleteBatchResponseDTO.builder()
                .deletedCount(deletedCount == null ? 0 : deletedCount)
                .unreadCount(unreadCount == null ? 0 : unreadCount)
                .build();
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
                request.getType(),
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
        NotificationType type = request.getType();
        Boolean pushEnabled = notificationMapper.selectNotificationSettingEnabled(
                request.getUserId(),
                type
        );
        if (Boolean.FALSE.equals(pushEnabled)) {
            return null;
        }

        NotificationVO notification = NotificationVO.builder()
                .userId(request.getUserId())
                .type(type)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .targetType(trim(request.getTargetType()))
                .targetId(request.getTargetId())
                .build();
        Integer insertedCount = notificationMapper.insertNotification(notification);
        if (insertedCount == null || insertedCount != 1 || notification.getId() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "알림 저장에 실패했습니다.");
        }

        Map<String, String> data = Map.of(
                "type", type.name(),
                "message", notification.getContent(),
                "occurredAt", LocalDateTime.now().toString()
        );
        List<String> fcmTokens = pushDeviceService.getTokens(request.getUserId());
        fcmTokens.forEach(token -> fcmSender.send(token, notification.getTitle(), notification.getContent(), data));
        notificationEmitterRegistry.sendToUser(request.getUserId(), "notification", data);

        return notification.getId();
    }

    private Integer resolvePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "page는 0 이상이어야 합니다.");
        }
        return page;
    }

    private Integer resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size <= 0 || size > MAX_SIZE) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "size는 1 이상 100 이하여야 합니다."
            );
        }
        return size;
    }

    private List<Long> resolveNotificationIds(NotificationDeleteBatchRequestDTO request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "삭제할 알림 정보가 필요합니다.");
        }
        if (request.getNoticeIds() == null || request.getNoticeIds().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "삭제할 알림 ID 목록은 필수입니다.");
        }
        Set<Long> uniqueNotificationIds = new LinkedHashSet<>(request.getNoticeIds());
        for (Long notificationId : uniqueNotificationIds) {
            validateNotificationId(notificationId);
        }
        return new ArrayList<>(uniqueNotificationIds);
    }

    private void validateCreateRequest(NotificationCreateRequestDTO request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "알림 생성 정보가 필요합니다.");
        }
        validateUserId(request.getUserId());
        validateType(request.getType());
        validateText(request.getTitle(), TITLE_MAX_LENGTH, "알림 제목");
        validateText(request.getContent(), null, "알림 내용");
        if (request.getTargetType() != null) {
            validateText(request.getTargetType(), TARGET_TYPE_MAX_LENGTH, "이동 대상 종류");
        }
        if (request.getTargetId() != null && request.getTargetId() <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이동 대상 ID가 올바르지 않습니다.");
        }
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

    private void validateNotificationId(Long notificationId) {
        if (notificationId == null || notificationId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "알림 ID가 올바르지 않습니다.");
        }
    }

    private void validateNotificationExists(Long userId, Long notificationId) {
        Integer exists = notificationMapper.existsNotification(userId, notificationId);
        if (exists == null || exists != 1) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "알림을 찾을 수 없습니다.");
        }
    }

    private void validateType(NotificationType type) {
        if (type == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "알림 종류는 필수입니다.");
        }
    }

    private void validateText(String value, Integer maxLength, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    fieldName + "은 필수입니다."
            );
        }
        if (maxLength != null && value.trim().length() > maxLength) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    fieldName + "은 " + maxLength + "자 이하여야 합니다."
            );
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

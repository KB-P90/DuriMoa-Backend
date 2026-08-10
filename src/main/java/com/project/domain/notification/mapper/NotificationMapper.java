package com.project.domain.notification.mapper;

import com.project.domain.notification.domain.NotificationVO;
import com.project.domain.notification.dto.NotificationResponseDTO;
import com.project.domain.notification.dto.NotificationSettingResponseDTO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface NotificationMapper {

    List<NotificationResponseDTO> selectNotificationsByUserId(@Param("userId") Long userId);

    List<NotificationResponseDTO> selectNotifications(
            @Param("userId") Long userId,
            @Param("isRead") Boolean isRead,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    Integer countNotifications(
            @Param("userId") Long userId,
            @Param("isRead") Boolean isRead
    );

    Integer countUnreadNotifications(@Param("userId") Long userId);

    Integer existsNotification(
            @Param("userId") Long userId,
            @Param("notificationId") Long notificationId
    );

    Integer updateNotificationRead(
            @Param("userId") Long userId,
            @Param("notificationId") Long notificationId
    );

    Integer updateAllNotificationsRead(@Param("userId") Long userId);

    Integer deleteNotification(
            @Param("userId") Long userId,
            @Param("notificationId") Long notificationId
    );

    Integer deleteNotifications(
            @Param("userId") Long userId,
            @Param("notificationIds") List<Long> notificationIds
    );

    List<NotificationSettingResponseDTO> selectNotificationSettingsByUserId(
            @Param("userId") Long userId
    );

    Boolean selectNotificationSettingEnabled(
            @Param("userId") Long userId,
            @Param("type") String type
    );

    Integer upsertNotificationSetting(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("pushEnabled") Boolean pushEnabled
    );

    Integer insertNotification(NotificationVO notification);
}

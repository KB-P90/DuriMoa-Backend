package com.project.domain.notification.mapper;

import com.project.domain.notification.domain.NotificationVO;
import com.project.domain.notification.dto.NotificationResponseDTO;
import com.project.domain.notification.dto.NotificationSettingResponseDTO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface NotificationMapper {

    List<NotificationResponseDTO> selectNotificationsByUserId(@Param("userId") Long userId);

    Integer countUnreadNotifications(@Param("userId") Long userId);

    Integer updateNotificationRead(
            @Param("userId") Long userId,
            @Param("notificationId") Long notificationId
    );

    Integer updateAllNotificationsRead(@Param("userId") Long userId);

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

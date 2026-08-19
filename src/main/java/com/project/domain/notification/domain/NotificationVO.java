package com.project.domain.notification.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String targetType;
    private Long targetId;
}

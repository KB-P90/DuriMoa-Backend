package com.project.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.domain.notification.domain.NotificationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "알림 설정 응답")
public class NotificationSettingResponseDTO {

    @ApiModelProperty(value = "알림 설정 ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "사용자 ID", example = "1")
    private Long userId;

    @ApiModelProperty(value = "알림 종류", example = "GOAL_CHANGED")
    private NotificationType type;

    @ApiModelProperty(value = "푸시 알림 활성화 여부", example = "true")
    private Boolean pushEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "생성 일시", example = "2026-07-31 10:30:00")
    private LocalDateTime createdAt;
}

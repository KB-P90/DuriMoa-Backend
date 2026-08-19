package com.project.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.domain.notification.domain.NotificationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "알림 응답")
public class NotificationResponseDTO {

    private static final DateTimeFormatter CREATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @ApiModelProperty(value = "알림 ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "사용자 ID", example = "1")
    private Long userId;

    @ApiModelProperty(value = "알림 종류", example = "GOAL_CHANGED")
    private NotificationType type;

    @ApiModelProperty(value = "알림 제목", example = "시안이 수정되었습니다.")
    private String title;

    @ApiModelProperty(value = "알림 내용", example = "상대방이 결혼 시안을 수정했습니다.")
    private String content;

    @ApiModelProperty(value = "읽음 여부", example = "false")
    private Boolean isRead;

    private LocalDateTime createdAt;

    @ApiModelProperty(value = "이동 대상 종류", example = "GOAL")
    private String targetType;

    @ApiModelProperty(value = "이동 대상 ID", example = "1")
    private Long targetId;

    public Long getNoticeId() {
        return id;
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    @ApiModelProperty(value = "생성 일시", example = "2026-07-23T09:30:00+09:00")
    public String getCreatedAt() {
        if (createdAt == null) {
            return null;
        }
        return createdAt.atZone(SEOUL_ZONE).format(CREATED_AT_FORMATTER);
    }

    @JsonIgnore
    public LocalDateTime getCreatedAtValue() {
        return createdAt;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }
}

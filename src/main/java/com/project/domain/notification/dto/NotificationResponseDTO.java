package com.project.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@ApiModel(description = "알림 응답")
public class NotificationResponseDTO {

    @ApiModelProperty(value = "알림 ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "사용자 ID", example = "1")
    private Long userId;

    @ApiModelProperty(value = "알림 종류", example = "GOAL")
    private String type;

    @ApiModelProperty(value = "알림 제목", example = "목표가 변경되었습니다.")
    private String title;

    @ApiModelProperty(value = "알림 내용", example = "결혼 목표 예산이 변경되었습니다.")
    private String content;

    @ApiModelProperty(value = "읽음 여부", example = "false")
    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "생성 일시", example = "2026-07-31 10:30:00")
    private LocalDateTime createdAt;
}

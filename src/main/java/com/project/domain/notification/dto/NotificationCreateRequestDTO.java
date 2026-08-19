package com.project.domain.notification.dto;

import com.project.domain.notification.domain.NotificationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "알림 생성 요청")
public class NotificationCreateRequestDTO {

    @NotNull(message = "사용자 ID는 필수입니다.")
    @ApiModelProperty(value = "알림 수신 사용자 ID", required = true, example = "1")
    private Long userId;

    @NotNull(message = "알림 종류는 필수입니다.")
    @ApiModelProperty(value = "알림 종류", required = true, example = "GOAL_CHANGED")
    private NotificationType type;

    @NotBlank(message = "알림 제목은 필수입니다.")
    @Size(max = 100, message = "알림 제목은 100자 이하여야 합니다.")
    @ApiModelProperty(value = "알림 제목", required = true, example = "시안이 수정되었습니다.")
    private String title;

    @NotBlank(message = "알림 내용은 필수입니다.")
    @ApiModelProperty(value = "알림 내용", required = true, example = "상대방이 결혼 시안을 수정했습니다.")
    private String content;

    @Size(max = 20, message = "이동 대상 종류는 20자 이하여야 합니다.")
    @ApiModelProperty(value = "이동 대상 종류", example = "GOAL")
    private String targetType;

    @ApiModelProperty(value = "이동 대상 ID", example = "1")
    private Long targetId;
}

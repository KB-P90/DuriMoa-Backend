package com.project.domain.notification.dto;

import com.project.domain.notification.domain.NotificationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "알림 설정 수정 요청")
public class NotificationSettingUpdateRequestDTO {

    @NotNull(message = "알림 종류는 필수입니다.")
    @ApiModelProperty(value = "알림 종류", required = true, example = "GOAL_CHANGED")
    private NotificationType type;

    @NotNull(message = "푸시 알림 활성화 여부는 필수입니다.")
    @ApiModelProperty(value = "푸시 알림 활성화 여부", required = true, example = "true")
    private Boolean pushEnabled;
}

package com.project.domain.notification.dto;

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
@ApiModel(description = "알림 설정 수정 요청")
public class NotificationSettingUpdateRequestDTO {

    @NotBlank(message = "알림 종류는 필수입니다.")
    @Size(max = 10, message = "알림 종류는 10자 이하여야 합니다.")
    @ApiModelProperty(value = "알림 종류", required = true, example = "GOAL")
    private String type;

    @NotNull(message = "푸시 알림 활성화 여부는 필수입니다.")
    @ApiModelProperty(value = "푸시 알림 활성화 여부", required = true, example = "true")
    private Boolean pushEnabled;
}

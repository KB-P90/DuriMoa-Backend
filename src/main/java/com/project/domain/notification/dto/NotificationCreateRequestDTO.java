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
@ApiModel(description = "알림 생성 요청")
public class NotificationCreateRequestDTO {

    @NotNull(message = "사용자 ID는 필수입니다.")
    @ApiModelProperty(value = "알림 수신 사용자 ID", required = true, example = "1")
    private Long userId;

    @NotBlank(message = "알림 종류는 필수입니다.")
    @Size(max = 10, message = "알림 종류는 10자 이하여야 합니다.")
    @ApiModelProperty(value = "알림 종류", required = true, example = "GOAL")
    private String type;

    @NotBlank(message = "알림 제목은 필수입니다.")
    @Size(max = 100, message = "알림 제목은 100자 이하여야 합니다.")
    @ApiModelProperty(value = "알림 제목", required = true, example = "목표가 변경되었습니다.")
    private String title;

    @NotBlank(message = "알림 내용은 필수입니다.")
    @ApiModelProperty(value = "알림 내용", required = true, example = "결혼 목표 예산이 변경되었습니다.")
    private String content;
}

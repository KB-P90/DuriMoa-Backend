package com.project.domain.notification.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "알림 설정 일괄 수정 요청")
public class NotificationSettingBatchUpdateRequestDTO {

    @NotEmpty(message = "수정할 알림 설정 목록은 필수입니다.")
    @Valid
    @ApiModelProperty(value = "수정할 알림 설정 목록", required = true)
    private List<NotificationSettingUpdateRequestDTO> settings;
}

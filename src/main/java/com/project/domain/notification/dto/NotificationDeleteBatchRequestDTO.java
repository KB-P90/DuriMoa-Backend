package com.project.domain.notification.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "선택 알림 삭제 요청")
public class NotificationDeleteBatchRequestDTO {

    @NotEmpty(message = "삭제할 알림 ID 목록은 필수입니다.")
    @ApiModelProperty(value = "삭제할 알림 ID 목록", required = true, example = "[1, 2, 3]")
    private List<Long> noticeIds;
}

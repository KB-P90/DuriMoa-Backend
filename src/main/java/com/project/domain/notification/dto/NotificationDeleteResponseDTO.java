package com.project.domain.notification.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "개별 알림 삭제 응답")
public class NotificationDeleteResponseDTO {

    @ApiModelProperty(value = "알림 ID", example = "1")
    private Long noticeId;

    @ApiModelProperty(value = "전체 읽지 않은 알림 개수", example = "0")
    private Integer unreadCount;
}

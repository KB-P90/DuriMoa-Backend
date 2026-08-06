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
@ApiModel(description = "전체 알림 읽음 처리 응답")
public class NotificationReadAllResponseDTO {

    @ApiModelProperty(value = "읽음 처리된 알림 개수", example = "3")
    private Integer updatedCount;

    @ApiModelProperty(value = "전체 읽지 않은 알림 개수", example = "0")
    private Integer unreadCount;
}

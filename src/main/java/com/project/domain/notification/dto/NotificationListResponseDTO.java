package com.project.domain.notification.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "알림 목록 응답")
public class NotificationListResponseDTO {

    @ApiModelProperty(value = "알림 목록")
    private List<NotificationResponseDTO> notices;

    @ApiModelProperty(value = "현재 페이지", example = "0")
    private Integer page;

    @ApiModelProperty(value = "페이지 크기", example = "20")
    private Integer size;

    @ApiModelProperty(value = "필터 적용 후 전체 알림 개수", example = "4")
    private Integer totalElements;

    @ApiModelProperty(value = "다음 페이지 여부", example = "false")
    private Boolean hasNext;

    @ApiModelProperty(value = "전체 읽지 않은 알림 개수", example = "1")
    private Integer unreadCount;
}

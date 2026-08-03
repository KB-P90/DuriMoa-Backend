package com.project.domain.goal.dto;

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
@ApiModel(description = "메인 시안 상태 변경 응답")
public class GoalStatusUpdateResponseDTO {

    @ApiModelProperty(value = "목표 ID", example = "1")
    private Long goalId;

    @ApiModelProperty(value = "목표 상태", allowableValues = "NORMAL,PENDING,CONFIRMED")
    private String status;

    @ApiModelProperty(value = "메인 목표 여부", example = "false")
    private Boolean isMain;

    @ApiModelProperty(value = "현재 사용자의 메인 시안 요청 여부", example = "false")
    private Boolean requestedByMe;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "메인 시안 승인 요청 일시", example = "2026-07-31 14:00:00")
    private LocalDateTime requestedAt;
}

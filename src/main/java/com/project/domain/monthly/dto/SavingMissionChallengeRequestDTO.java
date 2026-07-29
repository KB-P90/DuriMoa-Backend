package com.project.domain.monthly.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "절약 미션 도전 요청 DTO")
public class SavingMissionChallengeRequestDTO {

    @ApiModelProperty(value = "조회 년월 (YYYY-MM)", example = "2026-07", required = true)
    private String yearMonth;
}

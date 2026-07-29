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
@ApiModel(description = "절약 추천 미션 생성 요청 DTO")
public class SavingMissionRecommendationRequestDTO {

    @ApiModelProperty(value = "절약 추천 미션을 생성할 사용자 ID", example = "1", required = true)
    private Long userId;

    @ApiModelProperty(value = "미션을 수행할 대상 연도", example = "2026", required = true)
    private Integer targetYear;

    @ApiModelProperty(value = "미션을 수행할 대상 월 (1~12)", example = "8", required = true)
    private Integer targetMonth;
}

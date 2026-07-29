package com.project.domain.monthly.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "절약 추천 미션 생성 응답 DTO")
public class SavingMissionRecommendationResponseDTO {

    @ApiModelProperty(value = "추천 대상 사용자 ID", example = "1")
    private Long userId;

    @ApiModelProperty(value = "소비 분석 대상 연도", example = "2026")
    private Integer analysisYear;

    @ApiModelProperty(value = "소비 분석 대상 월", example = "7")
    private Integer analysisMonth;

    @ApiModelProperty(value = "추천 미션 수행 대상 연도", example = "2026")
    private Integer targetYear;

    @ApiModelProperty(value = "추천 미션 수행 대상 월", example = "8")
    private Integer targetMonth;

    @ApiModelProperty(value = "생성된 추천 미션 개수", example = "4")
    private Integer missionCount;

    @ApiModelProperty(value = "생성된 추천 미션 목록")
    private List<RecommendedMissionDTO> missions;
}

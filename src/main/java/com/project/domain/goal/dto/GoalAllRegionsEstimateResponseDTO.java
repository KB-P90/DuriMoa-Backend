package com.project.domain.goal.dto;

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
@ApiModel(description = "주요 지역별 균형형 예상 결혼 예산")
public class GoalAllRegionsEstimateResponseDTO {

    @ApiModelProperty(value = "주요 지역별 균형형 예상 금액 목록")
    private List<GoalRegionEstimateResponseDTO> regions;
}

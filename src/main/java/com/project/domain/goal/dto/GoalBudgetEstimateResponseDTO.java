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
@ApiModel(description = "지역별 유형별 예상 결혼 예산")
public class GoalBudgetEstimateResponseDTO {

    @ApiModelProperty(value = "지역", example = "서울(강남)")
    private String region;

    @ApiModelProperty(value = "알뜰형, 균형형, 플렉스형 예상 금액 목록")
    private List<GoalBudgetTypeEstimateResponseDTO> estimates;
}

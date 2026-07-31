package com.project.domain.goal.dto;

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
@ApiModel(description = "결혼 예산 유형별 예상 금액")
public class GoalBudgetTypeEstimateResponseDTO {

    @ApiModelProperty(value = "결혼 예산 유형", example = "알뜰형")
    private String type;

    @ApiModelProperty(value = "예식장, 스튜디오, 드레스, 메이크업 합계", example = "23200000")
    private Long totalAmount;
}

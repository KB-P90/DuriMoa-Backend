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
@ApiModel(description = "지역별 균형형 예상 결혼 예산")
public class GoalRegionEstimateResponseDTO {

    @ApiModelProperty(value = "지역", example = "서울")
    private String region;

    @ApiModelProperty(value = "예식장, 스튜디오, 드레스, 메이크업 중간값 합계", example = "8690000")
    private Long totalAmount;
}

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
@ApiModel(description = "추천 절약 미션 항목 DTO")
public class RecommendedMissionDTO {

    @ApiModelProperty(value = "생성된 절약 미션 ID", example = "101")
    private Long missionId;

    @ApiModelProperty(value = "추천 대상 소비 카테고리 코드", example = "FOOD")
    private String categoryCode;

    @ApiModelProperty(value = "사용자가 수행할 절약 미션 내용", example = "주 3회에서 주 1회로 줄이기")
    private String title;

    @ApiModelProperty(value = "미션 수행 시 예상되는 절감 금액", example = "30000")
    private Long expectedSavingAmount;
}

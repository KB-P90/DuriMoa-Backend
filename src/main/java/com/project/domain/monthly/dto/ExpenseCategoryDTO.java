package com.project.domain.monthly.dto;

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
@ApiModel(description = "카테고리별 지출 정보 DTO")
public class ExpenseCategoryDTO {

    @ApiModelProperty(value = "카테고리 ID", example = "1")
    private Long categoryId;

    @ApiModelProperty(value = "카테고리 코드", example = "FOOD")
    private String categoryCode;

    @ApiModelProperty(value = "지출 금액", example = "620000")
    private Long amount;

    @ApiModelProperty(value = "전월 대비 증감률 (%)", example = "18", required = false)
    private Integer comparisonRate;

    @ApiModelProperty(value = "총 지출 대비 비율 (%)", example = "80")
    private Integer expenseRate;
}

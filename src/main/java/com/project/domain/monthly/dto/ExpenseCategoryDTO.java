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

    @ApiModelProperty(value = "카테고리 명", example = "식비")
    private String categoryName;

    @ApiModelProperty(value = "지출 금액", example = "300000")
    private Long amount;

    @ApiModelProperty(value = "소득 대비 비율 (%)", example = "30")
    private Integer rate;
}

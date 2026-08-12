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
@ApiModel(description = "사용자별 카테고리 지출 정보 DTO")
public class UserExpenseDetailDTO {

    @ApiModelProperty(value = "사용자 ID", example = "1")
    private Long userId;

    @ApiModelProperty(value = "사용자 이름", example = "나")
    private String name;

    @ApiModelProperty(value = "카테고리별 지출 목록")
    private List<ExpenseCategoryDTO> expenseCategories;
}

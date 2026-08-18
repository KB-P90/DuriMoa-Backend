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
@ApiModel(description = "월별 카테고리별 지출 및 피드백 조회 응답 DTO")
public class MonthlyExpenseResponseDTO {

    @ApiModelProperty(value = "조회 연도", example = "2026")
    private Integer year;

    @ApiModelProperty(value = "조회 월", example = "7")
    private Integer month;

    @ApiModelProperty(value = "나의 카테고리별 지출 정보")
    private UserExpenseDetailDTO me;

    @ApiModelProperty(value = "상대방의 카테고리별 지출 정보")
    private UserExpenseDetailDTO partner;

    @ApiModelProperty(value = "월별 피드백 목록")
    private List<ExpenseFeedbackDTO> feedbacks;
}


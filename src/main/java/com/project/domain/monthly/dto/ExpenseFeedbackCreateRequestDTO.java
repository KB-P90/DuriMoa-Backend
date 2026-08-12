package com.project.domain.monthly.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "월별 지출 피드백 생성 요청 DTO")
public class ExpenseFeedbackCreateRequestDTO {

    @NotNull
    @Min(1)
    @ApiModelProperty(value = "피드백 대상 연도", example = "2026", required = true)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    @ApiModelProperty(value = "피드백 대상 월", example = "7", required = true)
    private Integer month;

    @NotBlank
    @Size(max = 100)
    @ApiModelProperty(value = "피드백 내용", example = "우리 다음달에는 좀 더 아껴보자!!", required = true)
    private String content;
}

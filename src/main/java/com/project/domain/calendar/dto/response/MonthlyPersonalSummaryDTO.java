package com.project.domain.calendar.dto.response;

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
@ApiModel(description = "개인 월별 거래 요약 DTO")
public class MonthlyPersonalSummaryDTO {

    @ApiModelProperty(value = "총 수입 금액", example = "3000000")
    private Long income;

    @ApiModelProperty(value = "총 지출 금액", example = "1500000")
    private Long expense;

    @ApiModelProperty(value = "총 저축 금액", example = "500000")
    private Long saving;
}

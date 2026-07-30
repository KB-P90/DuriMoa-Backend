package com.project.domain.calendar.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "결혼 월별 거래 요약 DTO")
public class MonthlyWeddingSummaryDTO {

    @ApiModelProperty(value = "결혼 목표 금액", example = "50000000")
    private Long goalAmount;

    @ApiModelProperty(value = "총 지출 금액", example = "2000000")
    private Long expense;

    @ApiModelProperty(value = "총 저축 금액", example = "1000000")
    private Long saving;
}

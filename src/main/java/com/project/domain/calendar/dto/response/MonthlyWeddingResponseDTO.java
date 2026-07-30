package com.project.domain.calendar.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "결혼 월별 거래 내역 조회 응답 DTO")
public class MonthlyWeddingResponseDTO {

    @ApiModelProperty(value = "결혼 월별 거래 요약")
    private MonthlyWeddingSummaryDTO summary;

    @ApiModelProperty(value = "날짜별 거래 여부 목록")
    private List<CalendarDayDTO> calendar;
}

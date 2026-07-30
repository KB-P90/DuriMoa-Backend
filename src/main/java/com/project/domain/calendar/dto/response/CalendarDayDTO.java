package com.project.domain.calendar.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "날짜별 거래 여부 응답 DTO")
public class CalendarDayDTO {

    @ApiModelProperty(value = "거래 날짜", example = "2026-07-30")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @ApiModelProperty(value = "수입 내역 존재 여부", example = "true")
    private boolean hasIncome;

    @ApiModelProperty(value = "지출 내역 존재 여부", example = "true")
    private boolean hasExpense;

    @ApiModelProperty(value = "저축 내역 존재 여부", example = "true")
    private boolean hasSaving;
}

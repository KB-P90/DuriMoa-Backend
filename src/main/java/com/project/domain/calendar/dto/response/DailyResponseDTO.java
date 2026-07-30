package com.project.domain.calendar.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "일별 거래 내역 조회 응답 DTO")
public class DailyResponseDTO {

    @ApiModelProperty(value = "조회 날짜", example = "2026-07-30")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @ApiModelProperty(value = "해당 날짜 거래 내역 개수", example = "3")
    private int count;

    @ApiModelProperty(value = "거래 내역 목록")
    private List<DailyRecordDTO> records;
}

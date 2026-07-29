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
@ApiModel(description = "월별 절약 미션 조회 응답 DTO")
public class SavingMissionResponseDTO {

    @ApiModelProperty(value = "조회 년월 (YYYY-MM)", example = "2026-07")
    private String yearMonth;

    @ApiModelProperty(value = "총 예상 절감 금액", example = "120000")
    private Long totalExpectedSavingAmount;

    @ApiModelProperty(value = "절약 미션 목록")
    private List<SavingMissionDTO> missions;
}

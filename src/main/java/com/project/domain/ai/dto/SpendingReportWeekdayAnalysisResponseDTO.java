package com.project.domain.ai.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpendingReportWeekdayAnalysisResponseDTO {

    private List<SpendingReportWeekdayResponseDTO> weekdays;
    private Long weekdayDailyAverage;
    private Long weekendDailyAverage;
    private BigDecimal weekendToWeekdayRatio;
    private String peakDayCode;
    private String peakDayName;
    private Long peakDayAverage;
    private String topWeekendCategoryName;
}

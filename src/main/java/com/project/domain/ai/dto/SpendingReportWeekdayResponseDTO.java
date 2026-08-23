package com.project.domain.ai.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpendingReportWeekdayResponseDTO {

    private String code;
    private String name;
    private Long totalAmount;
    private Long averageAmount;
    private Integer calendarDayCount;
    private BigDecimal relativeStrength;
}

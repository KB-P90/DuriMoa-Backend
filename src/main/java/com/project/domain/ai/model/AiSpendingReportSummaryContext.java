package com.project.domain.ai.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiSpendingReportSummaryContext {

    private Long totalExpense;
    private Long averageMonthlyExpense;
    private Integer transactionCount;
    private String topCategoryName;
    private BigDecimal topCategoryRatio;
    private BigDecimal recentTrendRate;
    private String peakDayName;
    private BigDecimal weekendToWeekdayRatio;
    private String topWeekendCategoryName;
}

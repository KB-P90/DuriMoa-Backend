package com.project.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.domain.ai.domain.SpendingTrendUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpendingReportResponseDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate analysisStartDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate analysisEndDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dataStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;

    private Integer analysisDays;
    private Boolean dataSufficient;
    private Long totalExpense;
    private Long averageMonthlyExpense;
    private Integer transactionCount;
    private String topCategoryName;
    private BigDecimal topCategoryRatio;
    private BigDecimal recentTrendRate;
    private SpendingTrendUnit trendUnit;
    private List<SpendingReportTrendResponseDTO> trend;
    private List<SpendingReportCategoryResponseDTO> categories;
    private SpendingReportWeekdayAnalysisResponseDTO weekdayAnalysis;
    private String summary;
    private String warning;
}

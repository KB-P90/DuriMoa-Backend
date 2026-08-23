package com.project.domain.ai.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpendingReportCategoryResponseDTO {

    private String code;
    private String name;
    private Long amount;
    private BigDecimal ratio;
    private Integer transactionCount;
}

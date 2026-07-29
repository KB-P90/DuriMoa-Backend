package com.project.domain.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryExpenseAnalysisDTO {

    private String categoryCode;
    private Long totalAmount;
    private Integer txCount;
}

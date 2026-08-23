package com.project.domain.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyExpenseSummaryCacheDTO {

    private Long monthlyIncomeBase;
    private List<RawCategoryExpenseDTO> categoryExpenses;
}

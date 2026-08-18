package com.project.domain.ai.model;

import com.project.domain.ai.domain.AiBudgetType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiWeddingBudgetSelectionContext {

    private List<AiBudgetType> eligibleBudgetTypes;
    private Long medianMonthlyIncome;
    private Long medianMonthlyExpense;
    private Long medianMonthlySaving;
    private Long sustainableMonthlySaving;
    private Long affordableTotalBudget;
    private Integer monthsUntilWedding;
    private String region;
}

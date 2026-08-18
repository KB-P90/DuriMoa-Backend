package com.project.domain.progress.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyProgressResponseDTO {

    private Integer overallProgressRate;
    private List<MonthlyProgressItemDTO> months;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyProgressItemDTO {

        private String yearMonth;
        private Long expenseAmount;
        private Long cumulativeExpenseAmount;
        private Integer progressRate;
    }
}

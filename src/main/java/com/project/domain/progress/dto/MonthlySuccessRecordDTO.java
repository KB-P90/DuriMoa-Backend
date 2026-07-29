package com.project.domain.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySuccessRecordDTO {

    private String yearMonth;
    private Integer achievementRate;
    private Long expenseAmount;
    private Integer changeRate;
}

package com.project.domain.ai.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiDailySpendingRecord {

    private LocalDate recordDate;
    private Long amount;
    private Integer transactionCount;
}

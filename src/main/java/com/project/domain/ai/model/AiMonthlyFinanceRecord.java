package com.project.domain.ai.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiMonthlyFinanceRecord {

    private String recordMonth;
    private Long income;
    private Long expense;
    private Long saving;
}

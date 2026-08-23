package com.project.domain.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRecordAggregateDTO {
    private LocalDate date;
    private Boolean hasIncome;
    private Boolean hasExpense;
    private Boolean hasSaving;
    private Long income;
    private Long expense;
    private Long saving;
}

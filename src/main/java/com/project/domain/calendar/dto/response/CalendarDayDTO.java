package com.project.domain.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayDTO {
    private LocalDate date;
    private boolean hasIncome;
    private boolean hasExpense;
    private boolean hasSaving;
}

package com.project.domain.calendar.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayDTO {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private boolean hasIncome;
    private boolean hasExpense;
    private boolean hasSaving;
}

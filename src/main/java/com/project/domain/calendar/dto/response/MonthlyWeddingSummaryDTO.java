package com.project.domain.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyWeddingSummaryDTO {
    private Long goalAmount;
    private Long expense;
    private Long saving;
}

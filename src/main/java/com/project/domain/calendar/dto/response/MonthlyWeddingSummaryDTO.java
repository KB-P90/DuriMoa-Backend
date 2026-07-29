package com.project.domain.calendar.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyWeddingSummaryDTO {
    private Long goalAmount;
    private Long expense;
    private Long saving;
}

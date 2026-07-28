package com.project.domain.calendar.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyWeddingResponseDTO {
    private MonthlyWeddingSummaryDTO summary;
    private List<CalendarDayDTO> calendar;
}

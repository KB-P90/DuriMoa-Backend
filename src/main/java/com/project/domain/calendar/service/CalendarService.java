package com.project.domain.calendar.service;

import com.project.domain.calendar.domain.RecordType;
import com.project.domain.calendar.dto.response.MonthlyPersonalResponseDTO;
import com.project.domain.calendar.dto.response.MonthlyWeddingResponseDTO;

import java.time.YearMonth;

public interface CalendarService {
    MonthlyWeddingResponseDTO getMonthlyWedding(YearMonth date, RecordType type);

    MonthlyPersonalResponseDTO getMonthlyPersonal(YearMonth date, RecordType type);
}

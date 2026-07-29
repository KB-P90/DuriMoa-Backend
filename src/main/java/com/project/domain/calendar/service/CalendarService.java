package com.project.domain.calendar.service;

import com.project.domain.calendar.domain.CalendarType;
import com.project.domain.calendar.dto.response.MonthlyPersonalResponseDTO;
import com.project.domain.calendar.dto.response.MonthlyWeddingResponseDTO;

import java.time.YearMonth;

public interface CalendarService {
    Object getMonthly(Long userId, YearMonth month, CalendarType type);
}

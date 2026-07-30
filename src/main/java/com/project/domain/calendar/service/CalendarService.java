package com.project.domain.calendar.service;

import com.project.domain.calendar.domain.CalendarType;
import com.project.domain.calendar.dto.request.RecordCreateRequestDTO;
import com.project.domain.calendar.dto.response.DailyResponseDTO;
import com.project.domain.calendar.dto.response.RecordResponseDTO;

import java.time.LocalDate;
import java.time.YearMonth;

public interface CalendarService {
    Object getMonthly(Long userId, YearMonth month, CalendarType type);

    DailyResponseDTO getDaily(Long userId, LocalDate date, CalendarType type);

    RecordResponseDTO create(Long userId, RecordCreateRequestDTO request);
}

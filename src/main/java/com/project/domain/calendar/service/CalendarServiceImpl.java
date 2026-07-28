package com.project.domain.calendar.service;

import com.project.domain.calendar.domain.RecordType;
import com.project.domain.calendar.dto.response.MonthlyPersonalResponseDTO;
import com.project.domain.calendar.dto.response.MonthlyWeddingResponseDTO;
import com.project.domain.calendar.mapper.CalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Log4j2
public class CalendarServiceImpl implements CalendarService {
    final private CalendarMapper mapper;

    @Override
    public MonthlyWeddingResponseDTO getMonthlyWedding(YearMonth date, RecordType type) {
        return null;
    }

    @Override
    public MonthlyPersonalResponseDTO getMonthlyPersonal(YearMonth date, RecordType type) {
        return null;
    }
}

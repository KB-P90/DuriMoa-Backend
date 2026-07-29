package com.project.domain.calendar.service;

import com.project.domain.calendar.domain.CalendarType;
import com.project.domain.calendar.dto.response.CalendarDayDTO;
import com.project.domain.calendar.dto.response.MonthlyPersonalResponseDTO;
import com.project.domain.calendar.dto.response.MonthlyPersonalSummaryDTO;
import com.project.domain.calendar.dto.response.MonthlyWeddingResponseDTO;
import com.project.domain.calendar.mapper.CalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CalendarServiceImpl implements CalendarService {
    final private CalendarMapper mapper;

    @Override
    public Object getMonthly(Long userId, YearMonth month, CalendarType type) {
        return switch(type) {
            case PERSONAL -> getMonthlyPersonal(userId, month, type);
            case WEDDING -> getMonthlyWedding(userId, month, type);
        };
    }

    private MonthlyPersonalResponseDTO getMonthlyPersonal(Long userId, YearMonth month, CalendarType type) {
        LocalDate start = month.atDay(1);
        LocalDate end = getEndDate(month);

        MonthlyPersonalSummaryDTO summary = mapper.selectMonthlyPersonalSummary(userId, start, end);
        List<CalendarDayDTO> calendar = buildCalendar(start, end, mapper.selectMonthlyPersonalCalendar(userId, start, end));

        return new MonthlyPersonalResponseDTO(summary, calendar);
    }

    private MonthlyWeddingResponseDTO getMonthlyWedding(Long userId, YearMonth month, CalendarType type) {
        return null;
    }

    // 해당 월 마지막 날짜 계산 - 현재 달일 경우 오늘까지 조회
    private LocalDate getEndDate(YearMonth month) {
        LocalDate today = LocalDate.now();
        return month.equals(YearMonth.from(today)) ? today : month.atEndOfMonth();
    }

    // 거래 내역 없어서 비어있는 날짜 있으면 데이터 추가
    private List<CalendarDayDTO> buildCalendar(LocalDate start, LocalDate end, List<CalendarDayDTO> result) {
        Map<LocalDate, CalendarDayDTO> map = result.stream()
                .collect(Collectors.toMap(
                        CalendarDayDTO::getDate,
                        Function.identity()
                ));

        List<CalendarDayDTO> calendar = new ArrayList<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            CalendarDayDTO dto = map.get(d);

            if (dto == null) {
                dto = new CalendarDayDTO();
                dto.setDate(d);
                dto.setHasIncome(false);
                dto.setHasExpense(false);
                dto.setHasSaving(false);
            }

            calendar.add(dto);
        }

        return calendar;
    }
}

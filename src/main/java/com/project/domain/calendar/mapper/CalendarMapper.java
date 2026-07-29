package com.project.domain.calendar.mapper;

import com.project.domain.calendar.dto.response.CalendarDayDTO;
import com.project.domain.calendar.dto.response.MonthlyPersonalSummaryDTO;
import com.project.domain.calendar.dto.response.MonthlyWeddingSummaryDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface CalendarMapper {
    MonthlyPersonalSummaryDTO selectMonthlyPersonalSummary(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    List<CalendarDayDTO> selectMonthlyPersonalCalendar(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

//    MonthlyWeddingSummaryDTO selectMonthlyWeddingSummary(Long userId, LocalDate start, LocalDate end);

//    List<CalendarDayDTO> selectMonthlyWeddingCalendar();
}

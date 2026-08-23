package com.project.domain.calendar.mapper;

import com.project.domain.calendar.domain.ServiceRecord;
import com.project.domain.calendar.dto.response.DailyRecordDTO;
import com.project.domain.calendar.dto.response.MonthlyRecordAggregateDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CalendarMapper {
    List<MonthlyRecordAggregateDTO> selectMonthlyPersonalAggregates(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    Long selectWeddingGoalAmount(@Param("userId") Long userId);

    List<MonthlyRecordAggregateDTO> selectMonthlyWeddingAggregates(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("categories") List<String> categories
    );

    List<DailyRecordDTO> selectDailyPersonal(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    List<DailyRecordDTO> selectDailyWedding(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("categories") List<String> categories
    );

    void create(ServiceRecord record);

    int update(ServiceRecord record);

    int delete(
            @Param("userId") Long userId,
            @Param("recordId") Long recordId);

    ServiceRecord findById(Long recordId);
}

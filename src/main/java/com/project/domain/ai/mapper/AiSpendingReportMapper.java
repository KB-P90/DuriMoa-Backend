package com.project.domain.ai.mapper;

import com.project.domain.ai.model.AiCategorySpendingRecord;
import com.project.domain.ai.model.AiDailySpendingRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiSpendingReportMapper {

    LocalDateTime selectUserCreatedAt(@Param("userId") Long userId);

    List<AiDailySpendingRecord> selectDailySpending(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<AiCategorySpendingRecord> selectCategorySpending(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    AiCategorySpendingRecord selectTopWeekendCategory(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

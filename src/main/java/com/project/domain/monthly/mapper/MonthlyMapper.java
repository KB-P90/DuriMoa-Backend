package com.project.domain.monthly.mapper;

import com.project.domain.monthly.dto.CategoryExpenseAnalysisDTO;
import com.project.domain.monthly.dto.ExpenseCategoryDTO;
import com.project.domain.monthly.dto.SavingMissionDTO;
import com.project.domain.monthly.dto.SavingMissionEntityDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MonthlyMapper {

    List<ExpenseCategoryDTO> selectMonthlyExpenseCategories(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

    List<SavingMissionDTO> selectMonthlySavingMissions(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

    int updateSavingMissionStatusToInProgress(@Param("userId") Long userId, @Param("missionId") Long missionId);

    List<CategoryExpenseAnalysisDTO> selectCategoryExpenseAnalysis(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    int insertSavingMission(SavingMissionEntityDTO mission);
}

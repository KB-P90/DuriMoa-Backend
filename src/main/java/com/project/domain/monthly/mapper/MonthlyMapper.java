package com.project.domain.monthly.mapper;

import com.project.domain.monthly.dto.ExpenseCategoryDTO;
import com.project.domain.monthly.dto.SavingMissionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MonthlyMapper {

    List<ExpenseCategoryDTO> selectMonthlyExpenseCategories(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

    List<SavingMissionDTO> selectMonthlySavingMissions(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

    int updateSavingMissionStatusToInProgress(@Param("userId") Long userId, @Param("missionId") Long missionId);
}

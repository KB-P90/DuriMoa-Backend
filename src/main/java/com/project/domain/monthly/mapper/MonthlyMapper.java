package com.project.domain.monthly.mapper;

import com.project.domain.monthly.dto.CategoryExpenseAnalysisDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackCreateResponseDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackEntityDTO;
import com.project.domain.monthly.dto.RawCategoryExpenseDTO;
import com.project.domain.monthly.dto.SavingMissionDTO;
import com.project.domain.monthly.dto.SavingMissionEntityDTO;
import com.project.domain.monthly.dto.UserSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MonthlyMapper {

    UserSummaryDTO selectUserSummary(@Param("userId") Long userId);

    Long selectConnectedPartnerId(@Param("userId") Long userId);

    List<RawCategoryExpenseDTO> selectCategoryExpensesByUserAndMonth(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

    Long selectMonthlyIncomeBase(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

    List<ExpenseFeedbackDTO> selectMonthlyFeedbacks(@Param("userId") Long userId, @Param("partnerId") Long partnerId, @Param("year") Integer year, @Param("month") Integer month);

    int upsertExpenseFeedback(ExpenseFeedbackEntityDTO feedback);

    ExpenseFeedbackCreateResponseDTO selectExpenseFeedbackById(@Param("feedbackId") Long feedbackId);

    List<SavingMissionDTO> selectMonthlySavingMissions(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

    int updateSavingMissionStatusToInProgress(@Param("userId") Long userId, @Param("missionId") Long missionId);

    List<CategoryExpenseAnalysisDTO> selectCategoryExpenseAnalysis(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    int insertSavingMission(SavingMissionEntityDTO mission);
}


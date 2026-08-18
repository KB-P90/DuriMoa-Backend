package com.project.domain.monthly.service;

import com.project.domain.monthly.dto.CategoryExpenseAnalysisDTO;
import com.project.domain.monthly.dto.CategoryExpenseStatisticsDTO;
import com.project.domain.monthly.dto.SavingMissionEntityDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class MonthlyScheduledSavingMissionTest {

    @Mock
    private MonthlyMapper monthlyMapper;

    @InjectMocks
    private MonthlyServiceImpl monthlyService;

    @Test
    void generateScheduledSavingMissions_SkipsExistingAndGeneratesMissingUser() {
        YearMonth targetMonth = YearMonth.of(2026, 8);
        Mockito.when(monthlyMapper.selectActiveUserIds()).thenReturn(List.of(1L, 2L));
        Mockito.when(monthlyMapper.existsSavingMissionByUserAndMonth(1L, 2026, 8))
                .thenReturn(true);
        Mockito.when(monthlyMapper.existsSavingMissionByUserAndMonth(2L, 2026, 8))
                .thenReturn(false);
        Mockito.when(monthlyMapper.selectCategoryExpenseAnalysis(
                        Mockito.eq(2L), Mockito.any(), Mockito.any()))
                .thenReturn(List.of(CategoryExpenseAnalysisDTO.builder()
                        .categoryCode("FOOD")
                        .totalAmount(600000L)
                        .txCount(4)
                        .build()));
        Mockito.when(monthlyMapper.selectCategoryExpenseStatistics(2026))
                .thenReturn(List.of(CategoryExpenseStatisticsDTO.builder()
                        .categoryCode("FOOD")
                        .averageAmount(500000L)
                        .build()));

        int generatedUserCount = monthlyService.generateScheduledSavingMissions(targetMonth);

        Assertions.assertEquals(1, generatedUserCount);
        Mockito.verify(monthlyMapper, Mockito.times(1))
                .insertSavingMission(Mockito.any(SavingMissionEntityDTO.class));
    }
}

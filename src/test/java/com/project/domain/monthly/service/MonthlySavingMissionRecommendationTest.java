package com.project.domain.monthly.service;

import com.project.domain.monthly.dto.CategoryExpenseAnalysisDTO;
import com.project.domain.monthly.dto.CategoryExpenseStatisticsDTO;
import com.project.domain.monthly.dto.SavingMissionEntityDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationRequestDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonthlySavingMissionRecommendationTest {

    @Mock
    private MonthlyMapper monthlyMapper;

    @Test
    void generateSavingMissionRecommendationsUsesStatisticsForTitleAndSavingAmount() {
        MonthlyServiceImpl monthlyService = new MonthlyServiceImpl(monthlyMapper);
        Mockito.when(monthlyMapper.selectCategoryExpenseAnalysis(
                        Mockito.eq(1L), Mockito.any(), Mockito.any()))
                .thenReturn(List.of(
                        analysis("FOOD", 600000L, 20),
                        analysis("SHOPPING", 70000L, 3),
                        analysis("CULTURE", 200000L, 8)));
        Mockito.when(monthlyMapper.selectCategoryExpenseStatistics(2026))
                .thenReturn(List.of(
                        statistics("FOOD", 450000L),
                        statistics("SHOPPING", 80000L),
                        statistics("CULTURE", 120000L)));

        monthlyService.generateSavingMissionRecommendations(
                SavingMissionRecommendationRequestDTO.builder()
                        .userId(1L)
                        .targetYear(2026)
                        .targetMonth(9)
                        .build());

        ArgumentCaptor<SavingMissionEntityDTO> captor =
                ArgumentCaptor.forClass(SavingMissionEntityDTO.class);
        Mockito.verify(monthlyMapper, Mockito.times(2)).insertSavingMission(captor.capture());
        List<SavingMissionEntityDTO> missions = captor.getAllValues();

        Assertions.assertEquals("FOOD", missions.get(0).getCategoryCode());
        Assertions.assertEquals("월 20회에서 15회로 줄이면", missions.get(0).getTitle());
        Assertions.assertEquals(150000L, missions.get(0).getExpectedSavingAmount());
        Assertions.assertEquals("CULTURE", missions.get(1).getCategoryCode());
        Assertions.assertEquals("월 8회에서 4회로 줄이면", missions.get(1).getTitle());
        Assertions.assertEquals(80000L, missions.get(1).getExpectedSavingAmount());
    }

    @Test
    void generateSavingMissionRecommendationsCreatesNoMissionAtOrBelowStatistics() {
        MonthlyServiceImpl monthlyService = new MonthlyServiceImpl(monthlyMapper);
        Mockito.when(monthlyMapper.selectCategoryExpenseAnalysis(
                        Mockito.eq(1L), Mockito.any(), Mockito.any()))
                .thenReturn(List.of(analysis("ETC", 100000L, 5)));
        Mockito.when(monthlyMapper.selectCategoryExpenseStatistics(2026))
                .thenReturn(List.of(statistics("ETC", 100000L)));

        monthlyService.generateSavingMissionRecommendations(
                SavingMissionRecommendationRequestDTO.builder()
                        .userId(1L)
                        .targetYear(2026)
                        .targetMonth(9)
                        .build());

        Mockito.verify(monthlyMapper, Mockito.never())
                .insertSavingMission(Mockito.any(SavingMissionEntityDTO.class));
    }

    private CategoryExpenseAnalysisDTO analysis(String categoryCode, Long amount, Integer txCount) {
        return CategoryExpenseAnalysisDTO.builder()
                .categoryCode(categoryCode)
                .totalAmount(amount)
                .txCount(txCount)
                .build();
    }

    private CategoryExpenseStatisticsDTO statistics(String categoryCode, Long averageAmount) {
        return CategoryExpenseStatisticsDTO.builder()
                .categoryCode(categoryCode)
                .averageAmount(averageAmount)
                .build();
    }
}

package com.project.domain.ai.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.project.domain.ai.domain.AiAnalysisScope;
import com.project.domain.ai.domain.AiBudgetType;
import com.project.domain.ai.dto.AiBudgetTypeModelResponseDTO;
import com.project.domain.ai.dto.WeddingBudgetRecommendationRequestDTO;
import com.project.domain.ai.dto.WeddingBudgetRecommendationResponseDTO;
import com.project.domain.ai.mapper.AiRecommendationMapper;
import com.project.domain.ai.model.AiAnalysisMemberRecord;
import com.project.domain.ai.model.AiMonthlyFinanceRecord;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;
import com.project.domain.goal.service.GoalStatisticsService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeddingBudgetRecommendationServiceImplTest {

    @Mock
    private AiRecommendationMapper aiRecommendationMapper;

    @Mock
    private GoalStatisticsService goalStatisticsService;

    @Mock
    private AiService aiService;

    private WeddingBudgetRecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WeddingBudgetRecommendationServiceImpl(
                aiRecommendationMapper,
                goalStatisticsService,
                aiService
        );
    }

    @Test
    void analysisPeriodUsesPreviousThreeCompletedMonths() {
        WeddingBudgetRecommendationServiceImpl.AnalysisPeriod period =
                service.resolveAnalysisPeriod(LocalDate.of(2026, 8, 18));

        Assertions.assertEquals(LocalDate.of(2026, 5, 1), period.getStartDate());
        Assertions.assertEquals(LocalDate.of(2026, 7, 31), period.getEndDate());
    }

    @Test
    void recommendsBalancedBudgetFromConnectedCoupleFinances() {
        when(aiRecommendationMapper.selectAnalysisMembers(1L)).thenReturn(List.of(
                new AiAnalysisMemberRecord(1L, 5_000_000L),
                new AiAnalysisMemberRecord(2L, 5_000_000L)
        ));
        when(aiRecommendationMapper.selectMonthlyFinances(anyList(), any(), any()))
                .thenReturn(List.of(
                        new AiMonthlyFinanceRecord("2026-05", 8_100_000L, 4_020_000L, 2_150_000L),
                        new AiMonthlyFinanceRecord("2026-06", 8_000_000L, 4_090_000L, 2_150_000L),
                        new AiMonthlyFinanceRecord("2026-07", 7_800_000L, 4_130_000L, 2_050_000L)
                ));
        stubStatistics();
        when(aiService.selectWeddingBudgetType(any())).thenReturn(
                new AiBudgetTypeModelResponseDTO(
                        AiBudgetType.BALANCED,
                        "두 사람의 저축 여력이 안정적이어서 균형형이 적합해요."
                )
        );

        WeddingBudgetRecommendationResponseDTO result = service.recommend(
                1L,
                new WeddingBudgetRecommendationRequestDTO(
                        LocalDate.now().plusMonths(10),
                        "서울(강남)"
                )
        );

        Assertions.assertEquals(AiAnalysisScope.COUPLE, result.getAnalysisScope());
        Assertions.assertEquals(2, result.getAnalyzedMemberCount());
        Assertions.assertEquals(3, result.getAnalysisMonths());
        Assertions.assertTrue(result.getDataSufficient());
        Assertions.assertEquals(2_150_000L, result.getSustainableMonthlySaving());
        Assertions.assertEquals("balanced", result.getBudgetType());
        Assertions.assertEquals(15_000_000L, result.getItems().getVenue());
        Assertions.assertEquals(1_440_000L, result.getItems().getReserve());
        Assertions.assertEquals(22_140_000L, result.getTotalBudget());
        Assertions.assertEquals("AI 추천 시안", result.getName());
        Assertions.assertEquals(5, result.getReasonDetails().size());
        Assertions.assertEquals("분석 대상", result.getReasonDetails().get(0).getTitle());
        Assertions.assertTrue(
                result.getReasonDetails().get(1).getDescription().contains("2,150,000원")
        );
        Assertions.assertTrue(
                result.getReasonDetails().get(4).getDescription().contains("7%")
        );
    }

    private void stubStatistics() {
        when(goalStatisticsService.findGoalStatistics(eq("서울(강남)"), eq("예식장")))
                .thenReturn(statistic("예식장", 10_000_000L, 15_000_000L, 20_000_000L));
        when(goalStatisticsService.findGoalStatistics(eq("서울(강남)"), eq("스튜디오")))
                .thenReturn(statistic("스튜디오", 1_500_000L, 2_500_000L, 4_000_000L));
        when(goalStatisticsService.findGoalStatistics(eq("서울(강남)"), eq("드레스")))
                .thenReturn(statistic("드레스", 1_200_000L, 2_000_000L, 3_500_000L));
        when(goalStatisticsService.findGoalStatistics(eq("서울(강남)"), eq("메이크업")))
                .thenReturn(statistic("메이크업", 800_000L, 1_200_000L, 1_800_000L));
    }

    private GoalStatisticsResponseDTO statistic(
            String category,
            Long lower,
            Long median,
            Long upper
    ) {
        return GoalStatisticsResponseDTO.builder()
                .region("서울(강남)")
                .category(category)
                .lower25(lower)
                .median(median)
                .upper25(upper)
                .build();
    }
}

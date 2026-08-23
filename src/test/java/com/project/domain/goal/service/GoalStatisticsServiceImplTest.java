package com.project.domain.goal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.goal.domain.GoalStatisticsVO;
import com.project.domain.goal.dto.GoalAllRegionsEstimateResponseDTO;
import com.project.domain.goal.dto.GoalBudgetEstimateResponseDTO;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;
import com.project.domain.goal.mapper.GoalStatisticsMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GoalStatisticsServiceImplTest {

    @Test
    void findGoalStatisticsReturnsMappedResponseForJuneBaseline() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);
        when(mapper.selectGoalStatistics("서울(강남)", "예식장"))
                .thenReturn(venueStatistics());

        GoalStatisticsResponseDTO response =
                service.findGoalStatistics(" 서울(강남) ", " 예식장 ", 6);

        assertEquals("서울(강남)", response.getRegion());
        assertEquals(13_000_000L, response.getUpper10());
        assertEquals(7_500_000L, response.getMedian());
        verify(mapper).selectGoalStatistics("서울(강남)", "예식장");
    }

    @Test
    void findGoalStatisticsAppliesVenueWeightForOctober() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);
        when(mapper.selectGoalStatistics("서울(강남)", "예식장"))
                .thenReturn(venueStatistics());

        GoalStatisticsResponseDTO response =
                service.findGoalStatistics("서울(강남)", "예식장", 10);

        assertEquals(11_260_000L, response.getUpper10());
        assertEquals(7_970_000L, response.getUpper25());
        assertEquals(6_490_000L, response.getMedian());
        assertEquals(3_810_000L, response.getLower25());
        assertEquals(1_840_000L, response.getLower10());
    }

    @Test
    void findGoalStatisticsRejectsBlankRegion() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findGoalStatistics(" ", "예식장", 6)
        );
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    void findGoalStatisticsRejectsWeddingMonthOutsideRange() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findGoalStatistics("서울(강남)", "예식장", 13)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        assertEquals("예식 예정 월은 1부터 12 사이여야 합니다.", exception.getMessage());
        verify(mapper, never()).selectGoalStatistics(anyString(), anyString());
    }

    @Test
    void findGoalStatisticsThrowsWhenDataDoesNotExist() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);
        when(mapper.selectGoalStatistics("없는지역", "예식장")).thenReturn(null);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findGoalStatistics("없는지역", "예식장", 6)
        );
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findGoalStatisticsReturnsZeroAmountsForUnsupportedCategory() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);

        GoalStatisticsResponseDTO response =
                service.findGoalStatistics(" 서울(강남) ", " 신혼여행 ", 10);

        assertEquals("서울(강남)", response.getRegion());
        assertEquals("신혼여행", response.getCategory());
        assertEquals(0L, response.getUpper10());
        assertEquals(0L, response.getUpper25());
        assertEquals(0L, response.getMedian());
        assertEquals(0L, response.getLower25());
        assertEquals(0L, response.getLower10());
        verify(mapper, never()).selectGoalStatistics(anyString(), anyString());
    }

    @Test
    void findGoalBudgetEstimateReturnsJuneBaselineTotals() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);
        List<String> categories = estimateCategories();
        when(mapper.selectGoalStatisticsForEstimate("서울(강남)", categories))
                .thenReturn(estimateStatistics());

        GoalBudgetEstimateResponseDTO response =
                service.findGoalBudgetEstimate(" 서울(강남) ", 6);

        assertEquals("서울(강남)", response.getRegion());
        assertEquals(3, response.getEstimates().size());
        assertEquals("알뜰형", response.getEstimates().get(0).getType());
        assertEquals(23_200_000L, response.getEstimates().get(0).getTotalAmount());
        assertEquals(28_500_000L, response.getEstimates().get(1).getTotalAmount());
        assertEquals(35_000_000L, response.getEstimates().get(2).getTotalAmount());
        verify(mapper).selectGoalStatisticsForEstimate("서울(강남)", categories);
    }

    @Test
    void findGoalBudgetEstimateAppliesSeparateOctoberWeights() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);
        List<String> categories = estimateCategories();
        when(mapper.selectGoalStatisticsForEstimate("서울(강남)", categories))
                .thenReturn(estimateStatistics());

        GoalBudgetEstimateResponseDTO response =
                service.findGoalBudgetEstimate("서울(강남)", 10);

        assertEquals(20_970_000L, response.getEstimates().get(0).getTotalAmount());
        assertEquals(25_770_000L, response.getEstimates().get(1).getTotalAmount());
        assertEquals(31_660_000L, response.getEstimates().get(2).getTotalAmount());
    }

    @Test
    void findGoalBudgetEstimateRejectsBlankRegion() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findGoalBudgetEstimate(" ", 6)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    void findGoalBudgetEstimateThrowsWhenCategoryIsMissing() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);
        List<String> categories = estimateCategories();
        when(mapper.selectGoalStatisticsForEstimate("서울(강남)", categories))
                .thenReturn(Collections.singletonList(
                        statistics("예식장", 18_000_000L, 22_000_000L, 27_000_000L)
                ));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findGoalBudgetEstimate("서울(강남)", 6)
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals(
                "예상 예산 산정에 필요한 통계 정보가 부족합니다.",
                exception.getMessage()
        );
    }

    @Test
    void findAllRegionsGoalBudgetEstimateReturnsUnadjustedRepresentativeValues() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);
        List<String> regions = estimateRegions();
        List<String> categories = estimateCategories();
        List<GoalStatisticsVO> statistics = regions.stream()
                .flatMap(region -> categories.stream()
                        .map(category -> statistics(region, category, 1L, 100L, 1_000L)))
                .collect(Collectors.toList());
        when(mapper.selectGoalStatisticsForAllRegionsEstimate(regions, categories))
                .thenReturn(statistics);

        GoalAllRegionsEstimateResponseDTO response =
                service.findAllRegionsGoalBudgetEstimate();

        assertEquals(7, response.getRegions().size());
        assertEquals("서울", response.getRegions().get(0).getRegion());
        assertEquals(400L, response.getRegions().get(0).getTotalAmount());
        assertEquals("부산", response.getRegions().get(1).getRegion());
        assertEquals("경기도", response.getRegions().get(6).getRegion());
        verify(mapper).selectGoalStatisticsForAllRegionsEstimate(regions, categories);
    }

    @Test
    void findAllRegionsGoalBudgetEstimateThrowsWhenRegionIsIncomplete() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = createService(mapper);
        List<String> regions = estimateRegions();
        List<String> categories = estimateCategories();
        List<GoalStatisticsVO> statistics = regions.stream()
                .flatMap(region -> categories.stream()
                        .filter(category -> !("부산".equals(region)
                                && "메이크업".equals(category)))
                        .map(category -> statistics(region, category, 1L, 100L, 1_000L)))
                .collect(Collectors.toList());
        when(mapper.selectGoalStatisticsForAllRegionsEstimate(regions, categories))
                .thenReturn(statistics);

        CustomException exception = assertThrows(
                CustomException.class,
                service::findAllRegionsGoalBudgetEstimate
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("예상 예산 산정에 필요한 통계 정보가 부족합니다.", exception.getMessage());
    }

    private GoalStatisticsService createService(GoalStatisticsMapper mapper) {
        return new GoalStatisticsServiceImpl(
                mapper,
                new WeddingMonthlyCostWeightCalculator()
        );
    }

    private GoalStatisticsVO venueStatistics() {
        return GoalStatisticsVO.builder()
                .region("서울(강남)")
                .category("예식장")
                .high10(13_000_000L)
                .high25(9_200_000L)
                .mid50(7_500_000L)
                .low25(4_400_000L)
                .low10(2_120_000L)
                .build();
    }

    private List<GoalStatisticsVO> estimateStatistics() {
        return Arrays.asList(
                statistics("예식장", 18_000_000L, 22_000_000L, 27_000_000L),
                statistics("스튜디오", 2_800_000L, 3_500_000L, 4_300_000L),
                statistics("드레스", 1_400_000L, 1_800_000L, 2_200_000L),
                statistics("메이크업", 1_000_000L, 1_200_000L, 1_500_000L)
        );
    }

    private List<String> estimateCategories() {
        return Arrays.asList("예식장", "스튜디오", "드레스", "메이크업");
    }

    private List<String> estimateRegions() {
        return Arrays.asList(
                "서울(강남외)", "부산", "대구", "인천", "광주", "대전", "경기도"
        );
    }

    private GoalStatisticsVO statistics(
            String category,
            Long lower25,
            Long median,
            Long upper25
    ) {
        return GoalStatisticsVO.builder()
                .region("서울(강남)")
                .category(category)
                .low25(lower25)
                .mid50(median)
                .high25(upper25)
                .build();
    }

    private GoalStatisticsVO statistics(
            String region,
            String category,
            Long lower25,
            Long median,
            Long upper25
    ) {
        return GoalStatisticsVO.builder()
                .region(region)
                .category(category)
                .low25(lower25)
                .mid50(median)
                .high25(upper25)
                .build();
    }
}

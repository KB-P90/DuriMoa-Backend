package com.project.domain.goal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.goal.domain.GoalStatisticsVO;
import com.project.domain.goal.dto.GoalBudgetEstimateResponseDTO;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;
import com.project.domain.goal.mapper.GoalStatisticsMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoalStatisticsServiceImplTest {

    @Test
    void findGoalStatisticsReturnsMappedResponse() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = new GoalStatisticsServiceImpl(mapper);
        when(mapper.selectGoalStatistics("서울(강남)", "예식장"))
                .thenReturn(GoalStatisticsVO.builder()
                        .region("서울(강남)")
                        .category("예식장")
                        .high10(13_000_000L)
                        .high25(9_200_000L)
                        .mid50(7_500_000L)
                        .low25(4_400_000L)
                        .low10(2_120_000L)
                        .build());

        GoalStatisticsResponseDTO response =
                service.findGoalStatistics(" 서울(강남) ", " 예식장 ");

        assertEquals("서울(강남)", response.getRegion());
        assertEquals(13_000_000L, response.getUpper10());
        assertEquals(7_500_000L, response.getMedian());
        verify(mapper).selectGoalStatistics("서울(강남)", "예식장");
    }

    @Test
    void findGoalStatisticsRejectsBlankRegion() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = new GoalStatisticsServiceImpl(mapper);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findGoalStatistics(" ", "예식장")
        );
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    void findGoalStatisticsThrowsWhenDataDoesNotExist() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = new GoalStatisticsServiceImpl(mapper);
        when(mapper.selectGoalStatistics("없는지역", "예식장")).thenReturn(null);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findGoalStatistics("없는지역", "예식장")
        );
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findGoalBudgetEstimateReturnsThreeTypeTotals() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = new GoalStatisticsServiceImpl(mapper);
        List<String> categories =
                Arrays.asList("예식장", "스튜디오", "드레스", "메이크업");
        when(mapper.selectGoalStatisticsForEstimate("서울(강남)", categories))
                .thenReturn(Arrays.asList(
                        statistics("예식장", 18_000_000L, 22_000_000L, 27_000_000L),
                        statistics("스튜디오", 2_800_000L, 3_500_000L, 4_300_000L),
                        statistics("드레스", 1_400_000L, 1_800_000L, 2_200_000L),
                        statistics("메이크업", 1_000_000L, 1_200_000L, 1_500_000L)
                ));

        GoalBudgetEstimateResponseDTO response =
                service.findGoalBudgetEstimate(" 서울(강남) ");

        assertEquals("서울(강남)", response.getRegion());
        assertEquals(3, response.getEstimates().size());
        assertEquals("알뜰형", response.getEstimates().get(0).getType());
        assertEquals(23_200_000L, response.getEstimates().get(0).getTotalAmount());
        assertEquals(28_500_000L, response.getEstimates().get(1).getTotalAmount());
        assertEquals(35_000_000L, response.getEstimates().get(2).getTotalAmount());
        verify(mapper).selectGoalStatisticsForEstimate("서울(강남)", categories);
    }

    @Test
    void findGoalBudgetEstimateRejectsBlankRegion() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = new GoalStatisticsServiceImpl(mapper);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findGoalBudgetEstimate(" ")
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    void findGoalBudgetEstimateThrowsWhenCategoryIsMissing() {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsService service = new GoalStatisticsServiceImpl(mapper);
        List<String> categories =
                Arrays.asList("예식장", "스튜디오", "드레스", "메이크업");
        when(mapper.selectGoalStatisticsForEstimate("서울(강남)", categories))
                .thenReturn(Collections.singletonList(
                        statistics("예식장", 18_000_000L, 22_000_000L, 27_000_000L)
                ));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findGoalBudgetEstimate("서울(강남)")
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals(
                "예상 예산 산정에 필요한 통계 정보가 부족합니다.",
                exception.getMessage()
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
}

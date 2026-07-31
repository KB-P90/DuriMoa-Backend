package com.project.domain.goal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.goal.domain.GoalStatisticsVO;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;
import com.project.domain.goal.mapper.GoalStatisticsMapper;
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
}

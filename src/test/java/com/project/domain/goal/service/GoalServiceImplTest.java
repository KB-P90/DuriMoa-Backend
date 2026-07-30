package com.project.domain.goal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.service.CoupleService;
import com.project.domain.goal.domain.GoalItemVO;
import com.project.domain.goal.domain.GoalVO;
import com.project.domain.goal.dto.GoalCreateRequestDTO;
import com.project.domain.goal.dto.GoalCreateResponseDTO;
import com.project.domain.goal.dto.GoalItemCreateRequestDTO;
import com.project.domain.goal.mapper.GoalMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GoalServiceImplTest {

    @Test
    void createGoalStoresGoalAndItemsForConnectedCouple() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalCreateRequestDTO request = validRequest();

        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.insertGoal(any(GoalVO.class))).thenAnswer(invocation -> {
            GoalVO goal = invocation.getArgument(0);
            goal.setId(100L);
            return 1;
        });
        when(goalMapper.insertGoalItems(eq(100L), anyList())).thenReturn(2);

        GoalCreateResponseDTO response = service.createGoal(1L, request);

        assertEquals(100L, response.getGoalId());
        assertEquals("NORMAL", response.getStatus());
        ArgumentCaptor<GoalVO> goalCaptor = ArgumentCaptor.forClass(GoalVO.class);
        verify(goalMapper).insertGoal(goalCaptor.capture());
        assertEquals(10L, goalCaptor.getValue().getCoupleId());
        assertEquals(35_000_000L, goalCaptor.getValue().getTotalBudget());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GoalItemVO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(goalMapper).insertGoalItems(eq(100L), itemsCaptor.capture());
        assertEquals(2, itemsCaptor.getValue().size());
        assertEquals("예식장", itemsCaptor.getValue().get(0).getCategory());
    }

    @Test
    void createGoalRejectsUnauthenticatedUser() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createGoal(null, validRequest())
        );

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(coupleService, never()).getConnectedCoupleByUserId(any());
    }

    @Test
    void createGoalRejectsRatioTotalOtherThanOneHundred() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalCreateRequestDTO request = GoalCreateRequestDTO.builder()
                .name("결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .totalBudget(35_000_000L)
                .groomRatio(60L)
                .brideRatio(30L)
                .items(validItems())
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createGoal(1L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(goalMapper, never()).insertGoal(any());
    }

    @Test
    void createGoalRejectsDuplicateCategories() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalItemCreateRequestDTO duplicate = item("예식장", 17_000_000L, true);
        GoalCreateRequestDTO request = GoalCreateRequestDTO.builder()
                .name("결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .totalBudget(35_000_000L)
                .groomRatio(50L)
                .brideRatio(50L)
                .items(List.of(item("예식장", 18_000_000L, true), duplicate))
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createGoal(1L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(goalMapper, never()).insertGoal(any());
    }

    @Test
    void createGoalRejectsBudgetDifferentFromIncludedItemSum() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalCreateRequestDTO request = GoalCreateRequestDTO.builder()
                .name("결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .totalBudget(40_000_000L)
                .groomRatio(50L)
                .brideRatio(50L)
                .items(validItems())
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createGoal(1L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(goalMapper, never()).insertGoal(any());
    }

    @Test
    void createGoalRejectsPastWeddingDate() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalCreateRequestDTO request = GoalCreateRequestDTO.builder()
                .name("결혼 예산")
                .weddingDate(LocalDate.of(2020, 1, 1))
                .region("서울(강남)")
                .totalBudget(35_000_000L)
                .groomRatio(50L)
                .brideRatio(50L)
                .items(validItems())
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createGoal(1L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(goalMapper, never()).insertGoal(any());
    }

    private GoalCreateRequestDTO validRequest() {
        return GoalCreateRequestDTO.builder()
                .name("균형형 결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .totalBudget(35_000_000L)
                .groomRatio(50L)
                .brideRatio(50L)
                .items(validItems())
                .build();
    }

    private List<GoalItemCreateRequestDTO> validItems() {
        return List.of(
                item("예식장", 18_000_000L, true),
                item("스튜디오", 17_000_000L, true)
        );
    }

    private GoalItemCreateRequestDTO item(
            String category,
            Long targetAmount,
            Boolean included
    ) {
        return GoalItemCreateRequestDTO.builder()
                .category(category)
                .targetAmount(targetAmount)
                .included(included)
                .type("균형형")
                .build();
    }
}

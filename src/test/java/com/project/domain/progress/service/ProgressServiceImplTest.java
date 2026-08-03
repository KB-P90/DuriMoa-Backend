package com.project.domain.progress.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.project.common.exception.CustomException;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.service.CoupleService;
import com.project.domain.progress.dto.ActiveGoalDTO;
import com.project.domain.progress.dto.CategoryExpenseDTO;
import com.project.domain.progress.dto.GoalItemSourceDTO;
import com.project.domain.progress.dto.MemberSourceDTO;
import com.project.domain.progress.dto.MonthlyExpenseDTO;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.mapper.ProgressMapper;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressServiceImplTest {

    private static final Long REQUEST_USER_ID = 10L;
    private static final Long PARTNER_USER_ID = 20L;
    private static final Long COUPLE_ID = 1L;
    private static final Long GOAL_ID = 100L;

    @Mock
    private CoupleService coupleService;

    @Mock
    private ProgressMapper progressMapper;

    @InjectMocks
    private ProgressServiceImpl progressService;

    @Test
    void returnsProgressForConnectedCouple() {
        YearMonth startMonth = YearMonth.now().minusMonths(2);
        CoupleDTO couple = new CoupleDTO(COUPLE_ID, REQUEST_USER_ID, PARTNER_USER_ID, "CONNECTED");
        ActiveGoalDTO activeGoal = ActiveGoalDTO.builder()
                .goalId(GOAL_ID)
                .targetAmount(50_000_000L)
                .groomRatio(50L)
                .brideRatio(50L)
                .createdAt(startMonth.atDay(1).atStartOfDay())
                .build();

        when(coupleService.getConnectedCoupleByUserId(REQUEST_USER_ID)).thenReturn(couple);
        when(progressMapper.findActiveGoal(COUPLE_ID)).thenReturn(activeGoal);
        when(progressMapper.findTotalWeddingExpense(
                eq(List.of(REQUEST_USER_ID, PARTNER_USER_ID)),
                anyList()
        )).thenReturn(22_700_000L);
        when(progressMapper.findIncludedGoalItems(GOAL_ID)).thenReturn(List.of(
                new GoalItemSourceDTO(1L, "예식장", 10_800_000L, true),
                new GoalItemSourceDTO(2L, "드레스", 5_200_000L, false)
        ));
        when(progressMapper.findCategoryWeddingExpenses(
                eq(List.of(REQUEST_USER_ID, PARTNER_USER_ID)),
                anyList()
        )).thenReturn(List.of(new CategoryExpenseDTO("WEDDINGHALL", 10_500_000L)));
        when(progressMapper.findMembers(REQUEST_USER_ID, PARTNER_USER_ID)).thenReturn(List.of(
                new MemberSourceDTO(REQUEST_USER_ID, "김민준", "G", 14_670_000L, true),
                new MemberSourceDTO(PARTNER_USER_ID, "이서연", "B", 16_520_000L, false)
        ));
        when(progressMapper.findMonthlyWeddingExpenses(
                eq(List.of(REQUEST_USER_ID, PARTNER_USER_ID)),
                anyList(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(
                new MonthlyExpenseDTO(startMonth.toString(), 3_500_000L),
                new MonthlyExpenseDTO(startMonth.plusMonths(1).toString(), 7_200_000L),
                new MonthlyExpenseDTO(startMonth.plusMonths(2).toString(), 12_000_000L)
        ));

        ProgressResponseDTO result = progressService.getProgress(REQUEST_USER_ID);

        assertEquals(50_000_000L, result.getOverall().getTargetAmount());
        assertEquals(22_700_000L, result.getOverall().getCurrentAmount());
        assertEquals(45, result.getOverall().getProgressRate());
        assertEquals(1, result.getOverall().getCompletedItemCount());
        assertEquals(2, result.getOverall().getTotalItemCount());
        assertEquals("예식장", result.getOverall().getItems().get(0).getCategory());
        assertEquals(10_500_000L, result.getOverall().getItems().get(0).getCurrentAmount());
        assertEquals(97, result.getOverall().getItems().get(0).getProgressRate());
        assertNull(result.getOverall().getItems().get(1).getProgressRate());

        assertEquals(REQUEST_USER_ID, result.getPersonal().getMembers().get(0).getUserId());
        assertEquals(true, result.getPersonal().getMembers().get(0).getAssetShared());
        assertEquals(14_670_000L, result.getPersonal().getMembers().get(0).getAssetAmount());
        assertEquals(58.7, result.getPersonal().getMembers().get(0).getProgressRate());
        assertEquals(PARTNER_USER_ID, result.getPersonal().getMembers().get(1).getUserId());
        assertEquals(false, result.getPersonal().getMembers().get(1).getAssetShared());
        assertNull(result.getPersonal().getMembers().get(1).getAssetAmount());
        assertNull(result.getPersonal().getMembers().get(1).getCurrentAmount());
        assertEquals(66.1, result.getPersonal().getMembers().get(1).getProgressRate());

        assertEquals(45, result.getMonthlyProgress().getOverallProgressRate());
        assertEquals(3, result.getMonthlyProgress().getMonths().size());
        assertEquals(YearMonth.now().toString(),
                result.getMonthlyProgress().getMonths().get(0).getYearMonth());
        assertEquals(22_700_000L,
                result.getMonthlyProgress().getMonths().get(0).getCumulativeExpenseAmount());
    }

    @Test
    void returnsNullProgressRateWhenTargetAmountIsZero() {
        YearMonth startMonth = YearMonth.now();
        CoupleDTO couple = new CoupleDTO(COUPLE_ID, REQUEST_USER_ID, PARTNER_USER_ID, "CONNECTED");
        ActiveGoalDTO activeGoal = ActiveGoalDTO.builder()
                .goalId(GOAL_ID)
                .targetAmount(0L)
                .groomRatio(50L)
                .brideRatio(50L)
                .createdAt(startMonth.atDay(1).atStartOfDay())
                .build();

        when(coupleService.getConnectedCoupleByUserId(REQUEST_USER_ID)).thenReturn(couple);
        when(progressMapper.findActiveGoal(COUPLE_ID)).thenReturn(activeGoal);
        when(progressMapper.findTotalWeddingExpense(anyList(), anyList())).thenReturn(1_000_000L);
        when(progressMapper.findIncludedGoalItems(GOAL_ID)).thenReturn(List.of());
        when(progressMapper.findCategoryWeddingExpenses(anyList(), anyList())).thenReturn(List.of());
        when(progressMapper.findMembers(REQUEST_USER_ID, PARTNER_USER_ID)).thenReturn(List.of(
                new MemberSourceDTO(REQUEST_USER_ID, "김민준", "G", 1_000_000L, true)
        ));
        when(progressMapper.findMonthlyWeddingExpenses(
                anyList(),
                anyList(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        ProgressResponseDTO result = progressService.getProgress(REQUEST_USER_ID);

        assertNull(result.getOverall().getProgressRate());
        assertNull(result.getPersonal().getMembers().get(0).getTargetAmount());
        assertNull(result.getPersonal().getMembers().get(0).getProgressRate());
        assertNull(result.getMonthlyProgress().getMonths().get(0).getProgressRate());
    }

    @Test
    void keepsCumulativeProgressRateWhenMonthHasNoExpense() {
        YearMonth startMonth = YearMonth.now().minusMonths(1);
        CoupleDTO couple = new CoupleDTO(COUPLE_ID, REQUEST_USER_ID, PARTNER_USER_ID, "CONNECTED");
        ActiveGoalDTO activeGoal = ActiveGoalDTO.builder()
                .goalId(GOAL_ID)
                .targetAmount(10_000_000L)
                .groomRatio(50L)
                .brideRatio(50L)
                .createdAt(startMonth.atDay(1).atStartOfDay())
                .build();

        when(coupleService.getConnectedCoupleByUserId(REQUEST_USER_ID)).thenReturn(couple);
        when(progressMapper.findActiveGoal(COUPLE_ID)).thenReturn(activeGoal);
        when(progressMapper.findTotalWeddingExpense(anyList(), anyList())).thenReturn(2_000_000L);
        when(progressMapper.findIncludedGoalItems(GOAL_ID)).thenReturn(List.of());
        when(progressMapper.findCategoryWeddingExpenses(anyList(), anyList())).thenReturn(List.of());
        when(progressMapper.findMembers(REQUEST_USER_ID, PARTNER_USER_ID)).thenReturn(List.of());
        when(progressMapper.findMonthlyWeddingExpenses(
                anyList(),
                anyList(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(new MonthlyExpenseDTO(startMonth.toString(), 2_000_000L)));

        ProgressResponseDTO result = progressService.getProgress(REQUEST_USER_ID);

        assertEquals(0, result.getMonthlyProgress().getMonths().get(0).getExpenseAmount());
        assertEquals(2_000_000L,
                result.getMonthlyProgress().getMonths().get(0).getCumulativeExpenseAmount());
        assertEquals(20, result.getMonthlyProgress().getMonths().get(0).getProgressRate());
    }

    @Test
    void throwsExceptionWhenActiveGoalIsMissing() {
        CoupleDTO couple = new CoupleDTO(COUPLE_ID, REQUEST_USER_ID, PARTNER_USER_ID, "CONNECTED");
        when(coupleService.getConnectedCoupleByUserId(REQUEST_USER_ID)).thenReturn(couple);
        when(progressMapper.findActiveGoal(COUPLE_ID)).thenReturn(null);

        assertThrows(CustomException.class, () -> progressService.getProgress(REQUEST_USER_ID));

        verify(progressMapper).findActiveGoal(COUPLE_ID);
        verifyNoMoreInteractions(progressMapper);
    }
}

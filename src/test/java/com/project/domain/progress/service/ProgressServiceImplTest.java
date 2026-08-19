package com.project.domain.progress.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.exception.CustomException;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.service.CoupleService;
import com.project.domain.progress.dto.ActiveGoalDTO;
import com.project.domain.progress.dto.CategoryExpenseDTO;
import com.project.domain.progress.dto.GoalItemSourceDTO;
import com.project.domain.progress.dto.MemberSourceDTO;
import com.project.domain.progress.dto.MonthlyExpenseDTO;
import com.project.domain.progress.dto.MonthlyProgressResponseDTO;
import com.project.domain.progress.dto.ProgressItemCompletionRequestDTO;
import com.project.domain.progress.dto.ProgressItemCompletionResponseDTO;
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

    private static final Long USER_ID = 10L;
    private static final Long PARTNER_ID = 20L;
    private static final Long COUPLE_ID = 1L;
    private static final Long GOAL_ID = 100L;

    @Mock
    private CoupleService coupleService;

    @Mock
    private ProgressMapper progressMapper;

    @InjectMocks
    private ProgressServiceImpl progressService;

    @Test
    void getsOverallAndPersonalProgressWithoutMonthlyQuery() {
        stubConnectedCoupleAndGoal();
        when(progressMapper.findIncludedGoalItems(GOAL_ID)).thenReturn(List.of(
                new GoalItemSourceDTO(1L, "WEDDINGHALL", 10_800_000L, true)
        ));
        when(progressMapper.findCategoryWeddingExpenses(anyList(), anyList())).thenReturn(List.of(
                new CategoryExpenseDTO("WEDDINGHALL", 10_500_000L)
        ));
        // assetAmount(실제 잔액)와 savingAmount(모은 돈)를 의도적으로 다른 값으로 둬서 두 값이 분리되어 쓰이는지 검증한다.
        when(progressMapper.findMembers(eq(USER_ID), eq(PARTNER_ID), anyList())).thenReturn(List.of(
                new MemberSourceDTO(USER_ID, "User", "G", "https://example.com/groom.jpg",
                        false, 20_000_000L, 5_000_000L, 14_670_000L),
                new MemberSourceDTO(PARTNER_ID, "Partner", "B", null,
                        false, 16_520_000L, 0L, 16_520_000L)
        ));

        ProgressResponseDTO result = progressService.getProgress(USER_ID);

        assertEquals(312, result.getOverall().getProgressRate());
        assertEquals(1, result.getOverall().getCompletedItemCount());
        assertEquals(USER_ID, result.getPersonal().getMembers().get(0).getUserId());
        assertEquals("G", result.getPersonal().getMembers().get(0).getRole());
        assertEquals("https://example.com/groom.jpg",
                result.getPersonal().getMembers().get(0).getProfileImage());
        assertFalse(result.getPersonal().getMembers().get(0).getAssetShared());
        assertEquals(20_000_000L, result.getPersonal().getMembers().get(0).getAssetAmount());
        assertEquals(14_670_000L, result.getPersonal().getMembers().get(0).getCurrentAmount());
        assertEquals("B", result.getPersonal().getMembers().get(1).getRole());
        assertNull(result.getPersonal().getMembers().get(1).getProfileImage());
        assertFalse(result.getPersonal().getMembers().get(1).getAssetShared());
        assertNull(result.getPersonal().getMembers().get(1).getAssetAmount());
        assertNull(result.getPersonal().getMembers().get(1).getCurrentAmount());
        verify(progressMapper, never()).findMonthlyWeddingSaving(
                anyList(), anyList(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getsMonthlyProgressFromEarliestSavingMonthIncludingWeddingFundBaseline() {
        YearMonth goalMonth = YearMonth.now();
        YearMonth firstSavingMonth = goalMonth.minusMonths(2);
        stubConnectedCoupleAndGoal(goalMonth);
        // weddingFund 합계 500,000 + 저축 거래 합계 2,000,000 = 모은 돈 2,500,000
        when(progressMapper.findMembers(eq(USER_ID), eq(PARTNER_ID), anyList())).thenReturn(List.of(
                new MemberSourceDTO(USER_ID, "User", "G", null, false, 0L, 300_000L, 1_300_000L),
                new MemberSourceDTO(PARTNER_ID, "Partner", "B", null, false, 0L, 200_000L, 1_200_000L)
        ));
        when(progressMapper.findFirstWeddingSavingDate(anyList(), anyList()))
                .thenReturn(firstSavingMonth.atDay(15));
        when(progressMapper.findMonthlyWeddingSaving(
                anyList(), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new MonthlyExpenseDTO(firstSavingMonth.toString(), 2_000_000L)));

        MonthlyProgressResponseDTO result = progressService.getMonthlyProgress(USER_ID);

        assertEquals(25, result.getOverallProgressRate());
        assertEquals(3, result.getMonths().size());
        assertEquals(YearMonth.now().toString(), result.getMonths().get(0).getYearMonth());
        assertEquals(2_500_000L, result.getMonths().get(0).getCumulativeExpenseAmount());
        assertEquals(2_000_000L, result.getMonths().get(2).getExpenseAmount());
        verify(progressMapper).findMonthlyWeddingSaving(
                anyList(), anyList(), eq(firstSavingMonth.atDay(1).atStartOfDay()), any(LocalDateTime.class));
    }

    @Test
    void updatesOnlyAnItemInTheConnectedCouplesActiveGoal() {
        when(coupleService.getConnectedCoupleByUserId(USER_ID)).thenReturn(connectedCouple());
        when(progressMapper.findActiveGoalIdByGoalItemId(1L, COUPLE_ID)).thenReturn(GOAL_ID);
        when(progressMapper.countCompletedIncludedGoalItems(GOAL_ID)).thenReturn(3);
        when(progressMapper.countIncludedGoalItems(GOAL_ID)).thenReturn(5);

        ProgressItemCompletionResponseDTO result = progressService.updateItemCompletion(
                USER_ID, 1L, new ProgressItemCompletionRequestDTO(true));

        assertTrue(result.getCompleted());
        assertEquals(3, result.getCompletedItemCount());
        assertEquals(5, result.getTotalItemCount());
        verify(progressMapper).updateGoalItemCompletion(1L, GOAL_ID, true);
    }

    @Test
    void rejectsItemOutsideTheActiveGoal() {
        when(coupleService.getConnectedCoupleByUserId(USER_ID)).thenReturn(connectedCouple());
        when(progressMapper.findActiveGoalIdByGoalItemId(1L, COUPLE_ID)).thenReturn(null);

        assertThrows(CustomException.class, () -> progressService.updateItemCompletion(
                USER_ID, 1L, new ProgressItemCompletionRequestDTO(true)));
        verify(progressMapper, never()).updateGoalItemCompletion(any(), any(), any());
    }

    private void stubConnectedCoupleAndGoal() {
        stubConnectedCoupleAndGoal(YearMonth.now().minusMonths(1));
    }

    private void stubConnectedCoupleAndGoal(YearMonth goalMonth) {
        when(coupleService.getConnectedCoupleByUserId(USER_ID)).thenReturn(connectedCouple());
        when(progressMapper.findActiveGoal(COUPLE_ID)).thenReturn(ActiveGoalDTO.builder()
                .goalId(GOAL_ID)
                .targetAmount(10_000_000L)
                .groomRatio(50L)
                .brideRatio(50L)
                .createdAt(goalMonth.atDay(1).atStartOfDay())
                .build());
    }

    private CoupleDTO connectedCouple() {
        return new CoupleDTO(COUPLE_ID, USER_ID, PARTNER_ID, "CONNECTED");
    }
}

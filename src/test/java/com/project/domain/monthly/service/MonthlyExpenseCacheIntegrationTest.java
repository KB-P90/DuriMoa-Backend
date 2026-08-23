package com.project.domain.monthly.service;

import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.dto.MonthlyExpenseSummaryCacheDTO;
import com.project.domain.monthly.dto.RawCategoryExpenseDTO;
import com.project.domain.monthly.dto.UserSummaryDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MonthlyExpenseCacheIntegrationTest {

    @Mock
    private MonthlyMapper monthlyMapper;

    @Mock
    private MonthlyExpenseCacheService monthlyExpenseCacheService;

    @Test
    void getMonthlyCategoryExpensesUsesCachedSummaryOnCacheHit() {
        Long userId = 1L;
        YearMonth yearMonth = YearMonth.of(2026, 8);
        Mockito.when(monthlyMapper.selectUserSummary(userId)).thenReturn(userSummary(userId));
        Mockito.when(monthlyMapper.selectConnectedPartnerId(userId)).thenReturn(null);
        Mockito.when(monthlyMapper.selectMonthlyFeedbacks(userId, null, 2026, 8))
                .thenReturn(Collections.emptyList());
        Mockito.when(monthlyExpenseCacheService.find(userId, yearMonth))
                .thenReturn(Optional.of(MonthlyExpenseSummaryCacheDTO.builder()
                        .monthlyIncomeBase(2_000_000L)
                        .categoryExpenses(List.of(rawExpense("FOOD", 200_000L)))
                        .build()));

        MonthlyServiceImpl service = new MonthlyServiceImpl(
                monthlyMapper,
                monthlyExpenseCacheService);
        MonthlyExpenseResponseDTO response = service.getMonthlyCategoryExpenses(userId, 2026, 8);

        Assertions.assertEquals(1, response.getMe().getExpenseCategories().size());
        Assertions.assertEquals(10, response.getMe().getExpenseCategories().get(0).getRate());
        Mockito.verify(monthlyMapper, Mockito.never())
                .selectCategoryExpensesByUserAndMonth(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.verify(monthlyMapper, Mockito.never())
                .selectMonthlyIncomeBase(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void getMonthlyCategoryExpensesCachesDatabaseSummaryOnCacheMiss() {
        Long userId = 1L;
        YearMonth yearMonth = YearMonth.of(2026, 8);
        List<RawCategoryExpenseDTO> expenses = List.of(rawExpense("LIVING", 300_000L));
        Mockito.when(monthlyMapper.selectUserSummary(userId)).thenReturn(userSummary(userId));
        Mockito.when(monthlyMapper.selectConnectedPartnerId(userId)).thenReturn(null);
        Mockito.when(monthlyMapper.selectMonthlyFeedbacks(userId, null, 2026, 8))
                .thenReturn(Collections.emptyList());
        Mockito.when(monthlyExpenseCacheService.find(userId, yearMonth))
                .thenReturn(Optional.empty());
        Mockito.when(monthlyMapper.selectCategoryExpensesByUserAndMonth(userId, 2026, 8))
                .thenReturn(expenses);
        Mockito.when(monthlyMapper.selectMonthlyIncomeBase(userId, 2026, 8))
                .thenReturn(3_000_000L);

        MonthlyServiceImpl service = new MonthlyServiceImpl(
                monthlyMapper,
                monthlyExpenseCacheService);
        service.getMonthlyCategoryExpenses(userId, 2026, 8);

        ArgumentCaptor<MonthlyExpenseSummaryCacheDTO> summaryCaptor =
                ArgumentCaptor.forClass(MonthlyExpenseSummaryCacheDTO.class);
        Mockito.verify(monthlyExpenseCacheService).save(
                Mockito.eq(userId),
                Mockito.eq(yearMonth),
                summaryCaptor.capture());
        Assertions.assertEquals(3_000_000L, summaryCaptor.getValue().getMonthlyIncomeBase());
        Assertions.assertEquals(expenses, summaryCaptor.getValue().getCategoryExpenses());
    }

    private UserSummaryDTO userSummary(Long userId) {
        return UserSummaryDTO.builder()
                .userId(userId)
                .name("테스트 사용자")
                .build();
    }

    private RawCategoryExpenseDTO rawExpense(String categoryCode, Long amount) {
        return RawCategoryExpenseDTO.builder()
                .categoryCode(categoryCode)
                .amount(amount)
                .build();
    }
}

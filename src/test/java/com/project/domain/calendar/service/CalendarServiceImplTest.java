package com.project.domain.calendar.service;

import com.project.domain.calendar.domain.CalendarType;
import com.project.domain.calendar.dto.response.MonthlyPersonalResponseDTO;
import com.project.domain.calendar.dto.response.MonthlyRecordAggregateDTO;
import com.project.domain.calendar.dto.response.MonthlyWeddingResponseDTO;
import com.project.domain.calendar.mapper.CalendarMapper;
import com.project.domain.transaction.service.TransactionSyncAsyncService;
import com.project.domain.transaction.service.TransactionSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CalendarServiceImplTest {

    @Mock
    private CalendarMapper mapper;

    @Mock
    private TransactionSyncService transactionSyncService;

    @Mock
    private TransactionSyncAsyncService transactionSyncAsyncService;

    @InjectMocks
    private CalendarServiceImpl calendarService;

    @Test
    void getMonthlyPersonalUsesSingleAggregateQueryAndBuildsSummaryAndCalendar() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 7);
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate activeDate = LocalDate.of(2026, 7, 15);

        Mockito.when(transactionSyncService.needSync(userId)).thenReturn(false);
        Mockito.when(mapper.selectMonthlyPersonalAggregates(userId, start, month.atEndOfMonth()))
                .thenReturn(List.of(aggregate(activeDate, true, true, false,
                        3_000_000L, 1_500_000L, 500_000L)));

        MonthlyPersonalResponseDTO response = (MonthlyPersonalResponseDTO) calendarService
                .getMonthly(userId, month, CalendarType.PERSONAL);

        Assertions.assertEquals(3_000_000L, response.getSummary().getIncome());
        Assertions.assertEquals(1_500_000L, response.getSummary().getExpense());
        Assertions.assertEquals(500_000L, response.getSummary().getSaving());
        Assertions.assertEquals(31, response.getCalendar().size());
        Assertions.assertTrue(response.getCalendar().get(14).isHasIncome());
        Assertions.assertTrue(response.getCalendar().get(14).isHasExpense());
        Assertions.assertFalse(response.getCalendar().get(14).isHasSaving());
        Assertions.assertFalse(response.getCalendar().get(0).isHasIncome());

        Mockito.verify(mapper).selectMonthlyPersonalAggregates(userId, start, month.atEndOfMonth());
        Mockito.verify(mapper, Mockito.never()).selectMonthlyWeddingAggregates(
                Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.anyList());
    }

    @Test
    void getMonthlyWeddingUsesSingleAggregateQueryAndIncludesGoalAmount() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 2);
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate activeDate = LocalDate.of(2026, 2, 28);

        Mockito.when(transactionSyncService.needSync(userId)).thenReturn(false);
        Mockito.when(mapper.selectWeddingGoalAmount(userId)).thenReturn(50_000_000L);
        Mockito.when(mapper.selectMonthlyWeddingAggregates(
                        Mockito.eq(userId), Mockito.eq(start), Mockito.eq(month.atEndOfMonth()), Mockito.anyList()))
                .thenReturn(List.of(aggregate(activeDate, false, true, true,
                        null, 2_000_000L, 1_000_000L)));

        MonthlyWeddingResponseDTO response = (MonthlyWeddingResponseDTO) calendarService
                .getMonthly(userId, month, CalendarType.WEDDING);

        Assertions.assertEquals(50_000_000L, response.getSummary().getGoalAmount());
        Assertions.assertEquals(2_000_000L, response.getSummary().getExpense());
        Assertions.assertEquals(1_000_000L, response.getSummary().getSaving());
        Assertions.assertEquals(28, response.getCalendar().size());
        Assertions.assertEquals(activeDate, response.getCalendar().get(27).getDate());
        Assertions.assertFalse(response.getCalendar().get(27).isHasIncome());
        Assertions.assertTrue(response.getCalendar().get(27).isHasExpense());
        Assertions.assertTrue(response.getCalendar().get(27).isHasSaving());

        Mockito.verify(mapper).selectWeddingGoalAmount(userId);
        Mockito.verify(mapper).selectMonthlyWeddingAggregates(
                Mockito.eq(userId), Mockito.eq(start), Mockito.eq(month.atEndOfMonth()), Mockito.anyList());
        Mockito.verify(mapper, Mockito.never()).selectMonthlyPersonalAggregates(
                Mockito.anyLong(), Mockito.any(), Mockito.any());
    }

    @Test
    void getMonthlyStartsAsyncSyncWhenUserNeedsSync() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 7);

        Mockito.when(transactionSyncService.needSync(userId)).thenReturn(true);
        Mockito.when(transactionSyncAsyncService.tryStarySync(userId)).thenReturn(true);
        Mockito.when(mapper.selectMonthlyPersonalAggregates(
                userId, month.atDay(1), month.atEndOfMonth())).thenReturn(List.of());

        calendarService.getMonthly(userId, month, CalendarType.PERSONAL);

        Mockito.verify(transactionSyncAsyncService).syncUserTransactionsAsync(userId);
    }

    private MonthlyRecordAggregateDTO aggregate(
            LocalDate date,
            Boolean hasIncome,
            Boolean hasExpense,
            Boolean hasSaving,
            Long income,
            Long expense,
            Long saving
    ) {
        return MonthlyRecordAggregateDTO.builder()
                .date(date)
                .hasIncome(hasIncome)
                .hasExpense(hasExpense)
                .hasSaving(hasSaving)
                .income(income)
                .expense(expense)
                .saving(saving)
                .build();
    }
}

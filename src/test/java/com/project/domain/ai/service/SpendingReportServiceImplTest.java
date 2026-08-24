package com.project.domain.ai.service;

import com.project.domain.ai.dto.AiSpendingReportSummaryModelResponseDTO;
import com.project.domain.ai.dto.SpendingReportResponseDTO;
import com.project.domain.ai.mapper.AiSpendingReportMapper;
import com.project.domain.ai.model.AiCategorySpendingRecord;
import com.project.domain.ai.model.AiDailySpendingRecord;
import com.project.domain.ai.model.AiSpendingReportSummaryContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpendingReportServiceImplTest {

    @Mock
    private AiSpendingReportMapper spendingReportMapper;

    @Mock
    private AiService aiService;

    @Test
    void generatesPersonalReportFromSignupDateThroughRequestDate() {
        Long userId = 7L;
        LocalDateTime requestedAt = LocalDateTime.of(2026, 1, 31, 15, 20);
        Mockito.when(spendingReportMapper.selectUserCreatedAt(userId))
                .thenReturn(LocalDateTime.of(2026, 1, 1, 9, 0));
        Mockito.when(spendingReportMapper.selectDailySpending(
                        userId,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(
                        new AiDailySpendingRecord(LocalDate.of(2026, 1, 3), 120_000L, 3),
                        new AiDailySpendingRecord(LocalDate.of(2026, 1, 10), 180_000L, 3)
                ));
        Mockito.when(spendingReportMapper.selectCategorySpending(
                        userId,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(
                        new AiCategorySpendingRecord("FOOD", 210_000L, 4),
                        new AiCategorySpendingRecord("CULTURE", 90_000L, 2)
                ));
        Mockito.when(spendingReportMapper.selectTopWeekendCategory(
                        userId,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)))
                .thenReturn(new AiCategorySpendingRecord("FOOD", 210_000L, 4));
        Mockito.when(aiService.summarizeSpendingReport(
                        Mockito.any(AiSpendingReportSummaryContext.class)))
                .thenReturn(new AiSpendingReportSummaryModelResponseDTO(
                        "가입 이후 식비 비중이 가장 높아요. 주말에 지출이 집중되는 경향이 보여요."
                ));

        SpendingReportServiceImpl service = new SpendingReportServiceImpl(
                spendingReportMapper,
                aiService
        );
        SpendingReportResponseDTO result = service.generate(userId, requestedAt);

        Assertions.assertEquals(LocalDate.of(2026, 1, 1), result.getAnalysisStartDate());
        Assertions.assertEquals(LocalDate.of(2026, 1, 31), result.getAnalysisEndDate());
        Assertions.assertEquals(LocalDate.of(2026, 1, 3), result.getDataStartDate());
        Assertions.assertEquals(300_000L, result.getTotalExpense());
        Assertions.assertEquals(6, result.getTransactionCount());
        Assertions.assertTrue(result.getDataSufficient());
        Assertions.assertEquals("식비", result.getTopCategoryName());
        Assertions.assertEquals(new BigDecimal("70.0"), result.getTopCategoryRatio());
        Assertions.assertEquals(7, result.getWeekdayAnalysis().getWeekdays().size());

        ArgumentCaptor<AiSpendingReportSummaryContext> contextCaptor =
                ArgumentCaptor.forClass(AiSpendingReportSummaryContext.class);
        Mockito.verify(aiService).summarizeSpendingReport(contextCaptor.capture());
        Assertions.assertEquals(300_000L, contextCaptor.getValue().getTotalExpense());
        Assertions.assertEquals("식비", contextCaptor.getValue().getTopCategoryName());
    }

    @Test
    void returnsDataWarningAndSkipsAiSummaryWhenRecordsAreInsufficient() {
        Long userId = 9L;
        LocalDateTime requestedAt = LocalDateTime.of(2026, 2, 10, 10, 0);
        Mockito.when(spendingReportMapper.selectUserCreatedAt(userId))
                .thenReturn(LocalDateTime.of(2026, 2, 1, 9, 0));
        Mockito.when(spendingReportMapper.selectDailySpending(
                        Mockito.eq(userId), Mockito.any(LocalDate.class), Mockito.any(LocalDate.class)))
                .thenReturn(List.of());
        Mockito.when(spendingReportMapper.selectCategorySpending(
                        Mockito.eq(userId), Mockito.any(LocalDate.class), Mockito.any(LocalDate.class)))
                .thenReturn(List.of());

        SpendingReportServiceImpl service = new SpendingReportServiceImpl(
                spendingReportMapper,
                aiService
        );
        SpendingReportResponseDTO result = service.generate(userId, requestedAt);

        Assertions.assertFalse(result.getDataSufficient());
        Assertions.assertEquals(0L, result.getTotalExpense());
        Assertions.assertNotNull(result.getWarning());
        Assertions.assertTrue(result.getSummary().contains("지출 내역이 아직 없어요"));
        Mockito.verifyNoInteractions(aiService);
    }
}

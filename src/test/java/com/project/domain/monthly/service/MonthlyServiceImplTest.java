package com.project.domain.monthly.service;

import com.project.domain.monthly.dto.ExpenseCategoryDTO;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class MonthlyServiceImplTest {

    @Mock
    private MonthlyMapper monthlyMapper;

    @InjectMocks
    private MonthlyServiceImpl monthlyService;

    @Test
    @DisplayName("월별 카테고리 지출 조회 시 총 소비 금액(totalAmount)이 합산되어 올바르게 반환된다.")
    void getMonthlyCategoryExpenses_CalculatesTotalAmount() {
        Long userId = 1L;
        Integer year = 2026;
        Integer month = 7;

        List<ExpenseCategoryDTO> mockCategories = List.of(
                ExpenseCategoryDTO.builder()
                        .categoryId(1L)
                        .categoryCode("SHOPPING")
                        .amount(200000L)
                        .comparisonRate(10)
                        .expenseRate(44)
                        .build(),
                ExpenseCategoryDTO.builder()
                        .categoryId(2L)
                        .categoryCode("FOOD")
                        .amount(150000L)
                        .comparisonRate(10)
                        .expenseRate(33)
                        .build(),
                ExpenseCategoryDTO.builder()
                        .categoryId(3L)
                        .categoryCode("TRANSPORT")
                        .amount(70000L)
                        .comparisonRate(10)
                        .expenseRate(16)
                        .build(),
                ExpenseCategoryDTO.builder()
                        .categoryId(4L)
                        .categoryCode("CAFE")
                        .amount(30000L)
                        .comparisonRate(10)
                        .expenseRate(7)
                        .build()
        );

        Mockito.when(monthlyMapper.selectMonthlyExpenseCategories(userId, year, month))
                .thenReturn(mockCategories);

        MonthlyExpenseResponseDTO response = monthlyService.getMonthlyCategoryExpenses(userId, year, month);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(2026, response.getYear());
        Assertions.assertEquals(7, response.getMonth());
        Assertions.assertEquals(450000L, response.getTotalAmount());
        Assertions.assertEquals(4, response.getExpenseCategories().size());
    }
}

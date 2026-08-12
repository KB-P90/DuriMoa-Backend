package com.project.domain.monthly.service;

import com.project.domain.monthly.dto.ExpenseCategoryDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackDTO;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.dto.RawCategoryExpenseDTO;
import com.project.domain.monthly.dto.UserSummaryDTO;
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
    @DisplayName("월별 카테고리 지출 조회 시 본인 및 상대방의 카테고리별 지출과 소득 대비 비율이 계산되고 피드백이 조회된다.")
    void getMonthlyCategoryExpenses_CalculatesIncomeWeightedRates() {
        Long userId = 1L;
        Long partnerId = 2L;
        Integer year = 2026;
        Integer month = 7;

        UserSummaryDTO meSummary = UserSummaryDTO.builder()
                .userId(userId)
                .name("나")
                .build();

        UserSummaryDTO partnerSummary = UserSummaryDTO.builder()
                .userId(partnerId)
                .name("상대방")
                .build();

        List<RawCategoryExpenseDTO> meExpenses = List.of(
                RawCategoryExpenseDTO.builder().categoryCode("FOOD").amount(300000L).build(),
                RawCategoryExpenseDTO.builder().categoryCode("LIVING").amount(200000L).build(),
                RawCategoryExpenseDTO.builder().categoryCode("SHOPPING").amount(4000000L).build(),
                RawCategoryExpenseDTO.builder().categoryCode("WEDDINGHALL").amount(1500000L).build()
        );

        List<RawCategoryExpenseDTO> partnerExpenses = List.of(
                RawCategoryExpenseDTO.builder().categoryCode("FOOD").amount(300000L).build(),
                RawCategoryExpenseDTO.builder().categoryCode("LIVING").amount(300000L).build()
        );

        List<ExpenseFeedbackDTO> feedbacks = List.of(
                ExpenseFeedbackDTO.builder().feedbackId(1L).writerUserId(partnerId).writerName("상대방").receiverId(partnerId).content("저번달보다 쇼핑을 줄였네").build()
        );

        Mockito.when(monthlyMapper.selectUserSummary(userId)).thenReturn(meSummary);
        Mockito.when(monthlyMapper.selectConnectedPartnerId(userId)).thenReturn(partnerId);
        Mockito.when(monthlyMapper.selectUserSummary(partnerId)).thenReturn(partnerSummary);
        Mockito.when(monthlyMapper.selectCategoryExpensesByUserAndMonth(userId, year, month)).thenReturn(meExpenses);
        Mockito.when(monthlyMapper.selectCategoryExpensesByUserAndMonth(partnerId, year, month)).thenReturn(partnerExpenses);
        Mockito.when(monthlyMapper.selectMonthlyIncomeBase(userId, year, month)).thenReturn(3000000L);
        Mockito.when(monthlyMapper.selectMonthlyIncomeBase(partnerId, year, month)).thenReturn(5000000L);
        Mockito.when(monthlyMapper.selectMonthlyFeedbacks(userId, partnerId, year, month)).thenReturn(feedbacks);

        MonthlyExpenseResponseDTO response = monthlyService.getMonthlyCategoryExpenses(userId, year, month);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(2026, response.getYear());
        Assertions.assertEquals(7, response.getMonth());

        Assertions.assertNotNull(response.getMe());
        Assertions.assertEquals("나", response.getMe().getName());
        List<ExpenseCategoryDTO> meCategories = response.getMe().getExpenseCategories();
        Assertions.assertEquals(3, meCategories.size());
        Assertions.assertEquals(10, meCategories.get(0).getRate());
        Assertions.assertEquals(100, meCategories.get(2).getRate());
        Assertions.assertTrue(meCategories.stream()
                .noneMatch(category -> "WEDDINGHALL".equals(category.getCategoryCode())));

        Assertions.assertNotNull(response.getPartner());
        Assertions.assertEquals("상대방", response.getPartner().getName());
        List<ExpenseCategoryDTO> partnerCategories = response.getPartner().getExpenseCategories();
        Assertions.assertEquals(2, partnerCategories.size());
        Assertions.assertEquals(6, partnerCategories.get(0).getRate());

        Assertions.assertEquals(1, response.getFeedbacks().size());
        Assertions.assertEquals("저번달보다 쇼핑을 줄였네", response.getFeedbacks().get(0).getContent());
    }
}

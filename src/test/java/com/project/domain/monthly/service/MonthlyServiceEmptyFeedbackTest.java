package com.project.domain.monthly.service;

import com.project.domain.monthly.dto.ExpenseFeedbackDTO;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
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

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class MonthlyServiceEmptyFeedbackTest {

    @Mock
    private MonthlyMapper monthlyMapper;

    @InjectMocks
    private MonthlyServiceImpl monthlyService;

    @Test
    @DisplayName("월별 피드백이 없으면 본인과 상대방의 빈 피드백을 반환한다.")
    void getMonthlyCategoryExpenses_ReturnsEmptyFeedbackForBothUsers() {
        Long userId = 1L;
        Long partnerId = 2L;
        Integer year = 2026;
        Integer month = 7;

        Mockito.when(monthlyMapper.selectUserSummary(userId)).thenReturn(UserSummaryDTO.builder()
                .userId(userId)
                .name("나")
                .build());
        Mockito.when(monthlyMapper.selectConnectedPartnerId(userId)).thenReturn(partnerId);
        Mockito.when(monthlyMapper.selectUserSummary(partnerId)).thenReturn(UserSummaryDTO.builder()
                .userId(partnerId)
                .name("상대방")
                .build());
        Mockito.when(monthlyMapper.selectCategoryExpensesByUserAndMonth(userId, year, month))
                .thenReturn(Collections.emptyList());
        Mockito.when(monthlyMapper.selectCategoryExpensesByUserAndMonth(partnerId, year, month))
                .thenReturn(Collections.emptyList());
        Mockito.when(monthlyMapper.selectMonthlyFeedbacks(userId, partnerId, year, month))
                .thenReturn(Collections.emptyList());

        MonthlyExpenseResponseDTO response = monthlyService.getMonthlyCategoryExpenses(
                userId, year, month);

        Assertions.assertEquals(2, response.getFeedbacks().size());
        assertEmptyFeedback(response.getFeedbacks().get(0), partnerId, "상대방");
        assertEmptyFeedback(response.getFeedbacks().get(1), userId, "나");
    }

    private void assertEmptyFeedback(
            ExpenseFeedbackDTO feedback, Long userId, String userName) {
        Assertions.assertNull(feedback.getFeedbackId());
        Assertions.assertEquals(userId, feedback.getWriterUserId());
        Assertions.assertEquals(userName, feedback.getWriterName());
        Assertions.assertEquals(userId, feedback.getReceiverId());
        Assertions.assertEquals("", feedback.getContent());
    }
}

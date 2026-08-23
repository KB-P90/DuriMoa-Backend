package com.project.domain.monthly.service;

import com.project.common.exception.CustomException;
import com.project.domain.monthly.dto.ExpenseFeedbackCreateRequestDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackCreateResponseDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackEntityDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonthlyServiceFeedbackTest {

    @Mock
    private MonthlyMapper monthlyMapper;

    @Mock
    private MonthlyExpenseCacheService monthlyExpenseCacheService;

    @InjectMocks
    private MonthlyServiceImpl monthlyService;

    @Test
    @DisplayName("월별 피드백을 생성하거나 기존 피드백을 수정한다.")
    void createOrUpdateExpenseFeedback_UpsertsFeedback() {
        Long userId = 1L;
        Long partnerId = 2L;
        ExpenseFeedbackCreateRequestDTO request = ExpenseFeedbackCreateRequestDTO.builder()
                .year(2026)
                .month(7)
                .content("우리 다음달에는 좀 더 아껴보자!!")
                .build();
        ExpenseFeedbackCreateResponseDTO expectedResponse =
                ExpenseFeedbackCreateResponseDTO.builder()
                        .feedbackId(3L)
                        .writerUserId(userId)
                        .writerName("나")
                        .writerProfileImageUrl("https://example.com/profile/user1.jpg")
                        .receiverUserId(partnerId)
                        .receiverName("상대방")
                        .year(2026)
                        .month(7)
                        .content(request.getContent())
                        .build();

        Mockito.when(monthlyMapper.selectConnectedPartnerId(userId)).thenReturn(partnerId);
        Mockito.doAnswer(invocation -> {
            ExpenseFeedbackEntityDTO feedback = invocation.getArgument(0);
            feedback.setFeedbackId(3L);
            return 1;
        }).when(monthlyMapper).upsertExpenseFeedback(Mockito.any(ExpenseFeedbackEntityDTO.class));
        Mockito.when(monthlyMapper.selectExpenseFeedbackById(3L)).thenReturn(expectedResponse);

        ExpenseFeedbackCreateResponseDTO response =
                monthlyService.createOrUpdateExpenseFeedback(userId, request);

        Assertions.assertEquals(3L, response.getFeedbackId());
        Assertions.assertEquals(userId, response.getWriterUserId());
        Assertions.assertEquals("https://example.com/profile/user1.jpg",
                response.getWriterProfileImageUrl());
        Assertions.assertEquals(partnerId, response.getReceiverUserId());
        Assertions.assertEquals(request.getContent(), response.getContent());

        ArgumentCaptor<ExpenseFeedbackEntityDTO> captor =
                ArgumentCaptor.forClass(ExpenseFeedbackEntityDTO.class);
        Mockito.verify(monthlyMapper).upsertExpenseFeedback(captor.capture());
        Assertions.assertEquals(userId, captor.getValue().getWriterUserId());
        Assertions.assertEquals(partnerId, captor.getValue().getReceiverUserId());
        Assertions.assertEquals(2026, captor.getValue().getYear());
        Assertions.assertEquals(7, captor.getValue().getMonth());
    }

    @Test
    @DisplayName("연결된 파트너가 없으면 피드백을 생성하지 않는다.")
    void createOrUpdateExpenseFeedback_ThrowsWhenPartnerDoesNotExist() {
        Long userId = 1L;
        ExpenseFeedbackCreateRequestDTO request = ExpenseFeedbackCreateRequestDTO.builder()
                .year(2026)
                .month(7)
                .content("피드백")
                .build();

        Mockito.when(monthlyMapper.selectConnectedPartnerId(userId)).thenReturn(null);

        Assertions.assertThrows(CustomException.class,
                () -> monthlyService.createOrUpdateExpenseFeedback(userId, request));
        Mockito.verify(monthlyMapper, Mockito.never())
                .upsertExpenseFeedback(Mockito.any(ExpenseFeedbackEntityDTO.class));
    }
}

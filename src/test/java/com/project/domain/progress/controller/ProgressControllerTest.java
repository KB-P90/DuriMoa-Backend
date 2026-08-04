package com.project.domain.progress.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.GoalItemProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.MemberProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.MonthlyProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.MonthlyProgressItemDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.OverallProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.PersonalProgressDTO;
import com.project.domain.progress.service.ProgressService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProgressControllerTest {

    private ProgressService progressService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        progressService = org.mockito.Mockito.mock(ProgressService.class);
        ProgressController progressController = new ProgressController(progressService);
        mockMvc = MockMvcBuilders.standaloneSetup(progressController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsProgressInApiResponse() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(10L, null, List.of())
        );
        when(progressService.getProgress(eq(10L))).thenReturn(progressResponse());

        MvcResult result = mockMvc.perform(get("/api/progress"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(body.get("success").asBoolean());
        assertEquals("현황 조회에 성공했습니다.", body.get("message").asText());
        assertEquals(50_000_000L, body.at("/data/overall/targetAmount").asLong());
        assertEquals(62, body.at("/data/overall/progressRate").asInt());
        assertEquals("예식장", body.at("/data/overall/items/0/category").asText());
        assertFalse(body.at("/data/personal/members/1/assetShared").asBoolean());
        assertTrue(body.at("/data/personal/members/1/assetAmount").isNull());
        assertEquals(62, body.at("/data/monthlyProgress/overallProgressRate").asInt());
        assertEquals(31_190_000L,
                body.at("/data/monthlyProgress/months/0/cumulativeExpenseAmount").asLong());
    }

    @Test
    void returnsApiResponseWhenActiveGoalIsMissing() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(10L, null, List.of())
        );
        when(progressService.getProgress(eq(10L))).thenThrow(new CustomException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "활성 결혼 목표를 찾을 수 없습니다."
        ));

        MvcResult result = mockMvc.perform(get("/api/progress"))
                .andExpect(status().isNotFound())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse(body.get("success").asBoolean());
        assertEquals("활성 결혼 목표를 찾을 수 없습니다.", body.get("message").asText());
        assertFalse(body.has("data"));
    }

    private ProgressResponseDTO progressResponse() {
        return ProgressResponseDTO.builder()
                .overall(OverallProgressDTO.builder()
                        .targetAmount(50_000_000L)
                        .currentAmount(31_190_000L)
                        .progressRate(62)
                        .completedItemCount(1)
                        .totalItemCount(2)
                        .items(List.of(GoalItemProgressDTO.builder()
                                .goalItemId(1L)
                                .category("예식장")
                                .targetAmount(10_800_000L)
                                .currentAmount(10_500_000L)
                                .progressRate(97)
                                .completed(true)
                                .build()))
                        .build())
                .personal(PersonalProgressDTO.builder()
                        .members(List.of(
                                MemberProgressDTO.builder()
                                        .userId(10L)
                                        .name("김민준")
                                        .assetShared(true)
                                        .assetAmount(14_670_000L)
                                        .targetAmount(25_000_000L)
                                        .currentAmount(14_670_000L)
                                        .progressRate(58.7)
                                        .build(),
                                MemberProgressDTO.builder()
                                        .userId(20L)
                                        .name("이서연")
                                        .assetShared(false)
                                        .assetAmount(null)
                                        .targetAmount(25_000_000L)
                                        .currentAmount(null)
                                        .progressRate(66.1)
                                        .build()
                        ))
                        .build())
                .monthlyProgress(MonthlyProgressDTO.builder()
                        .overallProgressRate(62)
                        .months(List.of(MonthlyProgressItemDTO.builder()
                                .yearMonth("2026-07")
                                .expenseAmount(12_000_000L)
                                .cumulativeExpenseAmount(31_190_000L)
                                .progressRate(62)
                                .build()))
                        .build())
                .build();
    }
}

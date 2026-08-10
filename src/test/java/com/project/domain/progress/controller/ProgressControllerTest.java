package com.project.domain.progress.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.domain.progress.dto.MonthlyProgressResponseDTO;
import com.project.domain.progress.dto.MonthlyProgressResponseDTO.MonthlyProgressItemDTO;
import com.project.domain.progress.dto.ProgressItemCompletionResponseDTO;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.MemberProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.OverallProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.PersonalProgressDTO;
import com.project.domain.progress.service.ProgressService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
        mockMvc = MockMvcBuilders.standaloneSetup(new ProgressController(progressService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(10L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsOverallAndPersonalProgressOnly() throws Exception {
        when(progressService.getProgress(eq(10L))).thenReturn(progressResponse());

        MvcResult result = mockMvc.perform(get("/api/progress"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(body.get("success").asBoolean());
        assertEquals(62, body.at("/data/overall/progressRate").asInt());
        assertFalse(body.at("/data/personal/members/1/assetShared").asBoolean());
        assertTrue(body.at("/data/monthlyProgress").isMissingNode());
    }

    @Test
    void returnsMonthlyProgressSeparately() throws Exception {
        when(progressService.getMonthlyProgress(eq(10L))).thenReturn(
                MonthlyProgressResponseDTO.builder()
                        .overallProgressRate(62)
                        .months(List.of(MonthlyProgressItemDTO.builder()
                                .yearMonth("2026-07")
                                .expenseAmount(12_000_000L)
                                .cumulativeExpenseAmount(31_190_000L)
                                .progressRate(62)
                                .build()))
                        .build());

        MvcResult result = mockMvc.perform(get("/api/progress/monthly"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertEquals(62, body.at("/data/overallProgressRate").asInt());
        assertEquals(31_190_000L, body.at("/data/months/0/cumulativeExpenseAmount").asLong());
    }

    @Test
    void updatesChecklistCompletion() throws Exception {
        when(progressService.updateItemCompletion(eq(10L), eq(1L), any())).thenReturn(
                ProgressItemCompletionResponseDTO.builder()
                        .goalItemId(1L)
                        .completed(true)
                        .completedItemCount(3)
                        .totalItemCount(5)
                        .build());

        MvcResult result = mockMvc.perform(patch("/api/progress/items/1/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(body.at("/data/completed").asBoolean());
        assertEquals(3, body.at("/data/completedItemCount").asInt());
        assertEquals(5, body.at("/data/totalItemCount").asInt());
    }

    private ProgressResponseDTO progressResponse() {
        return ProgressResponseDTO.builder()
                .overall(OverallProgressDTO.builder()
                        .targetAmount(50_000_000L)
                        .currentAmount(31_190_000L)
                        .progressRate(62)
                        .completedItemCount(1)
                        .totalItemCount(2)
                        .items(List.of())
                        .build())
                .personal(PersonalProgressDTO.builder()
                        .members(List.of(
                                MemberProgressDTO.builder().userId(10L).assetShared(true).build(),
                                MemberProgressDTO.builder().userId(20L).assetShared(false).build()))
                        .build())
                .build();
    }
}

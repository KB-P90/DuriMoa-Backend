package com.project.domain.monthly.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.common.response.ApiResponse;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationRequestDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationResponseDTO;
import com.project.domain.monthly.dto.SavingMissionResponseDTO;
import com.project.domain.monthly.service.MonthlyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MonthlyControllerTest {

    private MonthlyService monthlyService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        monthlyService = Mockito.mock(MonthlyService.class);
        MonthlyController monthlyController = new MonthlyController(monthlyService);
        mockMvc = MockMvcBuilders.standaloneSetup(monthlyController)
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
    @DisplayName("인증 정보가 있을 때 카테고리별 지출 조회가 성공한다.")
    void getMonthlyCategoryExpenses_Success_WithAuth() throws Exception {
        Long userId = 1L;
        MonthlyExpenseResponseDTO responseDTO = MonthlyExpenseResponseDTO.builder()
                .year(2026)
                .month(7)
                .totalAmount(3200000L)
                .expenseCategories(Collections.emptyList())
                .build();

        Mockito.when(monthlyService.getMonthlyCategoryExpenses(eq(userId), eq(2026), eq(7)))
                .thenReturn(responseDTO);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MvcResult result = mockMvc.perform(get("/api/expense/monthly")
                        .param("year", "2026")
                        .param("month", "7"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertTrue(response.get("success").asBoolean());
        Assertions.assertEquals("월별 카테고리별 지출 목록을 조회했습니다.", response.get("message").asText());
        Mockito.verify(monthlyService).getMonthlyCategoryExpenses(eq(userId), eq(2026), eq(7));
    }

    @Test
    @DisplayName("인증 정보가 없을 때 카테고리별 지출 조회가 401 에러를 반환한다.")
    void getMonthlyCategoryExpenses_Unauthorized_WithoutAuth() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/expense/monthly")
                        .param("year", "2026")
                        .param("month", "7"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertFalse(response.get("success").asBoolean());
        Assertions.assertEquals("인증이 필요합니다.", response.get("message").asText());
    }

    @Test
    @DisplayName("인증 정보가 있을 때 월별 절약 미션 조회가 성공한다.")
    void getMonthlySavingMissions_Success_WithAuth() throws Exception {
        Long userId = 1L;
        SavingMissionResponseDTO responseDTO = SavingMissionResponseDTO.builder()
                .yearMonth("2026-07")
                .totalExpectedSavingAmount(50000L)
                .missions(Collections.emptyList())
                .build();

        Mockito.when(monthlyService.getMonthlySavingMissions(eq(userId), eq(2026), eq(7)))
                .thenReturn(responseDTO);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MvcResult result = mockMvc.perform(get("/api/expense/saving-missions")
                        .param("year", "2026")
                        .param("month", "7"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertTrue(response.get("success").asBoolean());
        Assertions.assertEquals("월별 절약 미션 목록을 조회했습니다.", response.get("message").asText());
        Mockito.verify(monthlyService).getMonthlySavingMissions(eq(userId), eq(2026), eq(7));
    }

    @Test
    @DisplayName("인증 정보가 있을 때 절약 미션 도전 시작이 성공한다.")
    void startSavingMission_Success_WithAuth() throws Exception {
        Long userId = 1L;
        Long missionId = 101L;

        Mockito.doNothing().when(monthlyService).startSavingMission(eq(userId), eq(missionId));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MvcResult result = mockMvc.perform(post("/api/expense/saving-missions/{missionId}", missionId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertTrue(response.get("success").asBoolean());
        Assertions.assertEquals("절약 미션 도전을 시작했습니다.", response.get("message").asText());
        Mockito.verify(monthlyService).startSavingMission(eq(userId), eq(missionId));
    }

    @Test
    @DisplayName("인증 정보가 있을 때 절약 추천 미션 생성이 성공한다.")
    void generateSavingMissionRecommendations_Success_WithAuth() throws Exception {
        Long userId = 1L;
        SavingMissionRecommendationRequestDTO requestDTO = SavingMissionRecommendationRequestDTO.builder()
                .targetYear(2026)
                .targetMonth(8)
                .build();

        SavingMissionRecommendationResponseDTO responseDTO = SavingMissionRecommendationResponseDTO.builder()
                .userId(userId)
                .targetYear(2026)
                .targetMonth(8)
                .missionCount(0)
                .missions(Collections.emptyList())
                .build();

        Mockito.when(monthlyService.generateSavingMissionRecommendations(any(SavingMissionRecommendationRequestDTO.class)))
                .thenReturn(ResponseEntity.ok(ApiResponse.success("절약 추천 미션이 생성되었습니다.", responseDTO)));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MvcResult result = mockMvc.perform(post("/api/expense/saving-missions/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertTrue(response.get("success").asBoolean());
        Assertions.assertEquals("절약 추천 미션이 생성되었습니다.", response.get("message").asText());
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

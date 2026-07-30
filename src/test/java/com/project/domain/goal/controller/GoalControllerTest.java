package com.project.domain.goal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.domain.goal.dto.GoalCreateResponseDTO;
import com.project.domain.goal.dto.GoalItemResponseDTO;
import com.project.domain.goal.dto.GoalResponseDTO;
import com.project.domain.goal.service.GoalService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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

class GoalControllerTest {

    private GoalService goalService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        goalService = org.mockito.Mockito.mock(GoalService.class);
        GoalController controller = new GoalController(goalService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
    void createGoalReturnsCommonSuccessResponse() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList())
        );
        when(goalService.createGoal(eq(1L), any()))
                .thenReturn(GoalCreateResponseDTO.builder()
                        .goalId(100L)
                        .status("NORMAL")
                        .createdAt(LocalDateTime.of(2026, 7, 30, 10, 30))
                        .build());

        MvcResult result = mockMvc.perform(post("/api/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("결혼 목표가 저장되었습니다.", response.get("message").asText());
        assertEquals(100L, response.at("/data/goalId").asLong());
        assertEquals("NORMAL", response.at("/data/status").asText());
        assertEquals("2026-07-30 10:30:00", response.at("/data/createdAt").asText());
        verify(goalService).createGoal(eq(1L), any());
    }

    @Test
    void createGoalReturnsBadRequestForEmptyItems() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList())
        );
        String json = validJson().replace(
                "\"items\":[{\"category\":\"예식장\",\"targetAmount\":35000000,"
                        + "\"included\":true,\"type\":\"균형형\"}]",
                "\"items\":[]"
        );

        MvcResult result = mockMvc.perform(post("/api/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andReturn();
        JsonNode response = readResponse(result);

        assertFalse(response.get("success").asBoolean());
        assertEquals("목표 항목은 최소 1개 이상이어야 합니다.", response.get("message").asText());
    }

    @Test
    void createGoalUsesAuthenticationWhenPrincipalArgumentIsMissing() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());
        GoalCreateResponseDTO expected = GoalCreateResponseDTO.builder()
                .goalId(100L)
                .status("NORMAL")
                .createdAt(LocalDateTime.of(2026, 7, 30, 10, 30))
                .build();
        when(goalService.createGoal(eq(1L), any())).thenReturn(expected);
        GoalController controller = new GoalController(goalService);

        GoalCreateResponseDTO response = controller.createGoal(
                null,
                authentication,
                com.project.domain.goal.dto.GoalCreateRequestDTO.builder().build()
        ).getBody().getData();

        assertEquals(100L, response.getGoalId());
        verify(goalService).createGoal(eq(1L), any());
    }

    @Test
    void getGoalsReturnsGoalListWithoutCoupleId() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList())
        );
        when(goalService.getGoals(1L)).thenReturn(List.of(goalResponse()));

        MvcResult result = mockMvc.perform(get("/api/goal"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("결혼 목표 목록 조회에 성공했습니다.", response.get("message").asText());
        assertEquals(1, response.get("data").size());
        assertEquals(1L, response.at("/data/0/goalId").asLong());
        assertTrue(response.at("/data/0/coupleId").isMissingNode());
        assertEquals("예식장", response.at("/data/0/items/0/category").asText());
        verify(goalService).getGoals(1L);
    }

    @Test
    void getGoalsReturnsEmptyList() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList())
        );
        when(goalService.getGoals(1L)).thenReturn(Collections.emptyList());

        MvcResult result = mockMvc.perform(get("/api/goal"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").isArray());
        assertEquals(0, response.get("data").size());
    }

    private String validJson() {
        return "{"
                + "\"name\":\"균형형 결혼 예산\","
                + "\"weddingDate\":\"2099-10-09\","
                + "\"region\":\"서울(강남)\","
                + "\"totalBudget\":35000000,"
                + "\"groomRatio\":50,"
                + "\"brideRatio\":50,"
                + "\"items\":[{"
                + "\"category\":\"예식장\","
                + "\"targetAmount\":35000000,"
                + "\"included\":true,"
                + "\"type\":\"균형형\""
                + "}]"
                + "}";
    }

    private GoalResponseDTO goalResponse() {
        return GoalResponseDTO.builder()
                .goalId(1L)
                .name("균형형 결혼 예산")
                .weddingDate(LocalDate.of(2027, 10, 9))
                .region("서울(강남)")
                .totalBudget(23_200_000L)
                .groomRatio(50L)
                .brideRatio(50L)
                .status("NORMAL")
                .isMain(false)
                .createdAt(LocalDateTime.of(2026, 7, 30, 10, 30))
                .items(List.of(GoalItemResponseDTO.builder()
                        .goalItemId(1L)
                        .category("예식장")
                        .targetAmount(18_000_000L)
                        .included(true)
                        .completed(false)
                        .type("균형형")
                        .build()))
                .build();
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }
}

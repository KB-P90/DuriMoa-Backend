package com.project.domain.goal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.domain.goal.dto.GoalCreateItemResponseDTO;
import com.project.domain.goal.dto.GoalCreateResponseDTO;
import com.project.domain.goal.dto.GoalItemResponseDTO;
import com.project.domain.goal.dto.GoalResponseDTO;
import com.project.domain.goal.dto.GoalStatusUpdateResponseDTO;
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
                        .name("균형형 결혼 예산")
                        .weddingDate(LocalDate.of(2099, 10, 9))
                        .region("서울(강남)")
                        .totalBudget(35_000_000L)
                        .groomRatio(50L)
                        .brideRatio(50L)
                        .status("NORMAL")
                        .createdAt(LocalDateTime.of(2026, 7, 30, 10, 30))
                        .items(List.of(GoalCreateItemResponseDTO.builder()
                                .category("예식장")
                                .targetAmount(35_000_000L)
                                .included(true)
                                .completed(false)
                                .type("균형형")
                                .build()))
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
        assertEquals("균형형 결혼 예산", response.at("/data/name").asText());
        assertEquals(35_000_000L, response.at("/data/totalBudget").asLong());
        assertEquals("NORMAL", response.at("/data/status").asText());
        assertEquals("2026-07-30 10:30:00", response.at("/data/createdAt").asText());
        assertEquals(false, response.at("/data/items/0/completed").asBoolean());
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

    @Test
    void requestMainGoalReturnsPendingStatus() throws Exception {
        authenticate(1L);
        when(goalService.requestMainGoal(1L, 2L)).thenReturn(
                statusResponse(2L, "PENDING", false, true)
        );

        MvcResult result = mockMvc.perform(patch("/api/goal/2/request"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("메인 시안 승인 요청을 전송했습니다.", response.get("message").asText());
        assertEquals("PENDING", response.at("/data/status").asText());
        assertTrue(response.at("/data/requestedByMe").asBoolean());
        verify(goalService).requestMainGoal(1L, 2L);
    }

    @Test
    void cancelMainGoalRequestReturnsNormalStatus() throws Exception {
        authenticate(1L);
        when(goalService.cancelMainGoalRequest(1L, 2L)).thenReturn(
                statusResponse(2L, "NORMAL", false, false)
        );

        MvcResult result = mockMvc.perform(patch("/api/goal/2/cancel"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals("메인 시안 승인 요청을 취소했습니다.", response.get("message").asText());
        assertEquals("NORMAL", response.at("/data/status").asText());
        verify(goalService).cancelMainGoalRequest(1L, 2L);
    }

    @Test
    void decideMainGoalRequestReturnsConfirmedStatus() throws Exception {
        authenticate(2L);
        when(goalService.decideMainGoalRequest(eq(2L), eq(2L), any())).thenReturn(
                statusResponse(2L, "CONFIRMED", true, false)
        );

        MvcResult result = mockMvc.perform(patch("/api/goal/2/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals("메인 시안 승인 요청을 수락했습니다.", response.get("message").asText());
        assertEquals("CONFIRMED", response.at("/data/status").asText());
        assertTrue(response.at("/data/isMain").asBoolean());
        verify(goalService).decideMainGoalRequest(eq(2L), eq(2L), any());
    }

    @Test
    void decideMainGoalRequestRejectsBlankDecision() throws Exception {
        authenticate(2L);

        MvcResult result = mockMvc.perform(patch("/api/goal/2/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        JsonNode response = readResponse(result);

        assertFalse(response.get("success").asBoolean());
        assertEquals("승인 여부는 필수입니다.", response.get("message").asText());
    }

    private String validJson() {
        return "{"
                + "\"name\":\"균형형 결혼 예산\","
                + "\"weddingDate\":\"2099-10-09\","
                + "\"region\":\"서울(강남)\","
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

    private void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.emptyList()
                )
        );
    }

    private GoalStatusUpdateResponseDTO statusResponse(
            Long goalId,
            String status,
            Boolean isMain,
            Boolean requestedByMe
    ) {
        return GoalStatusUpdateResponseDTO.builder()
                .goalId(goalId)
                .status(status)
                .isMain(isMain)
                .requestedByMe(requestedByMe)
                .build();
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

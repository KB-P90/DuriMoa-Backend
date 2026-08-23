package com.project.domain.goal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.domain.goal.dto.GoalAllRegionsEstimateResponseDTO;
import com.project.domain.goal.dto.GoalBudgetEstimateResponseDTO;
import com.project.domain.goal.dto.GoalBudgetTypeEstimateResponseDTO;
import com.project.domain.goal.dto.GoalRegionEstimateResponseDTO;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;
import com.project.domain.goal.service.GoalStatisticsService;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GoalStatisticsControllerTest {

    private GoalStatisticsService goalStatisticsService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        goalStatisticsService = Mockito.mock(GoalStatisticsService.class);
        GoalStatisticsController controller =
                new GoalStatisticsController(goalStatisticsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getGoalStatisticsReturnsCommonResponse() throws Exception {
        GoalStatisticsResponseDTO statistics = GoalStatisticsResponseDTO.builder()
                .region("서울(강남)")
                .category("예식장")
                .upper10(11_260_000L)
                .upper25(7_970_000L)
                .median(6_490_000L)
                .lower25(3_810_000L)
                .lower10(1_840_000L)
                .build();
        when(goalStatisticsService.findGoalStatistics("서울(강남)", "예식장", 10))
                .thenReturn(statistics);

        MvcResult result = mockMvc.perform(get("/api/goal/stat")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("region", "서울(강남)")
                        .param("category", "예식장")
                        .param("weddingMonth", "10"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals(true, response.get("success").asBoolean());
        assertEquals("결혼 통계 정보 조회에 성공했습니다.", response.get("message").asText());
        assertEquals(11_260_000L, response.at("/data/upper10").asLong());
        assertEquals(true, response.at("/data/unit").isMissingNode());
    }

    @Test
    void getGoalStatisticsReturnsBadRequestForMissingRegion() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/goal/stat")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("category", "예식장")
                        .param("weddingMonth", "10"))
                .andExpect(status().isBadRequest())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals(false, response.get("success").asBoolean());
        assertEquals("필수 요청 파라미터가 누락되었습니다.", response.get("message").asText());
    }

    @Test
    void getGoalStatisticsReturnsBadRequestForMissingWeddingMonth() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/goal/stat")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("region", "서울(강남)")
                        .param("category", "예식장"))
                .andExpect(status().isBadRequest())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals(false, response.get("success").asBoolean());
        assertEquals("필수 요청 파라미터가 누락되었습니다.", response.get("message").asText());
    }

    @Test
    void getGoalBudgetEstimateReturnsCommonResponse() throws Exception {
        GoalBudgetEstimateResponseDTO estimate = GoalBudgetEstimateResponseDTO.builder()
                .region("서울(강남)")
                .estimates(Arrays.asList(
                        GoalBudgetTypeEstimateResponseDTO.builder()
                                .type("알뜰형")
                                .totalAmount(20_970_000L)
                                .build(),
                        GoalBudgetTypeEstimateResponseDTO.builder()
                                .type("균형형")
                                .totalAmount(25_770_000L)
                                .build(),
                        GoalBudgetTypeEstimateResponseDTO.builder()
                                .type("플렉스형")
                                .totalAmount(31_660_000L)
                                .build()
                ))
                .build();
        when(goalStatisticsService.findGoalBudgetEstimate("서울(강남)", 10))
                .thenReturn(estimate);

        MvcResult result = mockMvc.perform(get("/api/goal/stat/estimate")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("region", "서울(강남)")
                        .param("weddingMonth", "10"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals(true, response.get("success").asBoolean());
        assertEquals(
                "유형별 예상 결혼 예산 조회에 성공했습니다.",
                response.get("message").asText()
        );
        assertEquals("알뜰형", response.at("/data/estimates/0/type").asText());
        assertEquals(20_970_000L, response.at("/data/estimates/0/totalAmount").asLong());
        assertEquals(31_660_000L, response.at("/data/estimates/2/totalAmount").asLong());
    }

    @Test
    void getAllRegionsGoalBudgetEstimateReturnsUnadjustedCommonResponse() throws Exception {
        GoalAllRegionsEstimateResponseDTO estimate = GoalAllRegionsEstimateResponseDTO.builder()
                .regions(Arrays.asList(
                        GoalRegionEstimateResponseDTO.builder()
                                .region("서울")
                                .totalAmount(8_690_000L)
                                .build(),
                        GoalRegionEstimateResponseDTO.builder()
                                .region("부산")
                                .totalAmount(5_300_000L)
                                .build()
                ))
                .build();
        when(goalStatisticsService.findAllRegionsGoalBudgetEstimate()).thenReturn(estimate);

        MvcResult result = mockMvc.perform(get("/api/goal/stat/all-regions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals(true, response.get("success").asBoolean());
        assertEquals(
                "주요 지역 예상 결혼 예산 조회에 성공했습니다.",
                response.get("message").asText()
        );
        assertEquals("서울", response.at("/data/regions/0/region").asText());
        assertEquals(8_690_000L, response.at("/data/regions/0/totalAmount").asLong());
        assertEquals("부산", response.at("/data/regions/1/region").asText());
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }
}

package com.project.domain.home.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.domain.home.dto.HomeResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.SetupChecklistResponseDTO;
import com.project.domain.home.service.HomeService;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    private HomeService homeService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // 홈 Controller 단위 테스트에 필요한 MockMvc를 구성합니다.
    @BeforeEach
    void setUp() {
        homeService = Mockito.mock(HomeService.class);
        HomeController homeController = new HomeController(homeService);
        mockMvc = MockMvcBuilders.standaloneSetup(homeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        SecurityContextHolder.clearContext();
    }

    // 테스트 사이에 인증 컨텍스트가 남지 않도록 정리합니다.
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 인증 principal이 있으면 홈 조회 성공 응답을 반환합니다.
    @Test
    void getHomeReturnsSuccessResponseWithAuthentication() throws Exception {
        Mockito.when(homeService.getHome(1L)).thenReturn(homeResponse());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MvcResult result = mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertTrue(response.get("success").asBoolean());
        Assertions.assertEquals("홈 화면 조회에 성공했습니다.", response.get("message").asText());
        Assertions.assertEquals(5, response.at("/data/setup_checklist/total_count").asInt());
        Assertions.assertEquals(1, response.at("/data/setup_checklist/current_step").asInt());
        Mockito.verify(homeService).getHome(1L);
    }

    // 인증 정보가 없으면 홈 조회를 거부합니다.
    @Test
    void getHomeReturnsUnauthorizedWithoutAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/home"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertFalse(response.get("success").asBoolean());
        Assertions.assertEquals("인증이 필요합니다.", response.get("message").asText());
        Assertions.assertTrue(response.get("data").isNull());
    }

    // 홈 Controller 테스트용 최소 응답을 만듭니다.
    private HomeResponseDTO homeResponse() {
        return HomeResponseDTO.builder()
                .setupChecklist(SetupChecklistResponseDTO.builder()
                        .totalCount(5)
                        .completedCount(0)
                        .currentStep(1)
                        .steps(Collections.emptyList())
                        .build())
                .build();
    }

    // MockMvc 응답 본문을 JSON으로 읽습니다.
    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

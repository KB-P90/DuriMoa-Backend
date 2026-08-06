package com.project.domain.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.domain.onboarding.controller.OnboardingController;
import com.project.domain.onboarding.dto.OnboardingQueryRequestDTO;
import com.project.domain.onboarding.dto.OnboardingQueryResponseDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterRequestDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterResponseDTO;
import com.project.domain.onboarding.dto.OnboardingSelectRequestDTO;
import com.project.domain.onboarding.dto.OnboardingSelectResponseDTO;
import com.project.domain.onboarding.service.OnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OnboardingService onboardingService;

    @InjectMocks
    private OnboardingController onboardingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(onboardingController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(Long.class) && parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return 1L;
                    }
                })
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("1. 계좌/카드 등록 및 추가 API 컨트롤러 테스트")
    void registerOrAddAccount_ControllerTest() throws Exception {
        OnboardingRegisterRequestDTO request = OnboardingRegisterRequestDTO.builder()
                .company("국민은행")
                .id("test_user")
                .password("password123!")
                .build();

        OnboardingRegisterResponseDTO response = OnboardingRegisterResponseDTO.builder()
                .connectedId("demo_connected_id")
                .actionType("CREATED")
                .businessType("BK")
                .organization("0004")
                .company("국민은행")
                .build();

        when(onboardingService.registerOrAddAccount(any(), any())).thenReturn(response);

        MvcResult result = mockMvc.perform(post("/api/onboarding/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(body);
        assertTrue(body.contains("demo_connected_id"));
        assertTrue(body.contains("국민은행"));
    }

    @Test
    @DisplayName("2. 계좌/카드 목록 조회 API 컨트롤러 테스트")
    void getAccountsOrCards_ControllerTest() throws Exception {
        OnboardingQueryResponseDTO response = OnboardingQueryResponseDTO.builder()
                .itemList(Collections.singletonList("계좌데이터"))
                .build();

        when(onboardingService.getAccountsOrCards(any(), any())).thenReturn(response);

        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/onboarding/accounts")
                        .param("company", "국민은행"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(body);
        assertTrue(body.contains("계좌데이터"));
    }

    @Test
    @DisplayName("3. 계좌/카드 선택 및 거래 내역 저장 API 컨트롤러 테스트")
    void selectAccountsOrCards_ControllerTest() throws Exception {
        OnboardingSelectRequestDTO request = OnboardingSelectRequestDTO.builder()
                .company("국민은행")
                .selectedNumbers(Collections.singletonList("12345678901234"))
                .startDate("20260701")
                .endDate("20260731")
                .build();

        OnboardingSelectResponseDTO response = OnboardingSelectResponseDTO.builder()
                .businessType("BK")
                .organization("0004")
                .totalSavedRecords(15)
                .processedNumbers(Collections.singletonList("12345678901234"))
                .build();

        when(onboardingService.selectAccountsOrCards(any(), any())).thenReturn(response);

        MvcResult result = mockMvc.perform(post("/api/onboarding/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertNotNull(body);
        assertTrue(body.contains("15"));
    }
}

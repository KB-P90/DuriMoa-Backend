package com.project.domain.codef;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.response.ApiResponse;
import com.project.domain.codef.controller.CodefController;
import com.project.domain.codef.dto.CodefTokenResponseDTO;
import com.project.domain.codef.service.CodefService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CodefControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CodefService codefService;

    @InjectMocks
    private CodefController codefController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(codefController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("CODEF 토큰 발급 API 요청 컨트롤러 테스트")
    void getAccessToken_ControllerTest() throws Exception {
        // given
        CodefTokenResponseDTO mockResponse = CodefTokenResponseDTO.builder()
                .accessToken("mock_access_token")
                .tokenType("Bearer")
                .expiresIn(604800L)
                .scope("read")
                .build();

        when(codefService.getAccessToken()).thenReturn(mockResponse);

        // when & then
        MvcResult mvcResult = mockMvc.perform(post("/api/codef/token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        assertNotNull(responseContent);
        assertTrue(responseContent.contains("mock_access_token"));
    }

    @Test
    @DisplayName("CODEF 은행 거래내역 조회 API 요청 컨트롤러 테스트")
    void getBankTransactionList_ControllerTest() throws Exception {
        // given
        com.project.domain.codef.dto.CodefBankTransactionResponseDTO bankData = com.project.domain.codef.dto.CodefBankTransactionResponseDTO.builder()
                .items(java.util.Collections.emptyList())
                .build();
        com.project.domain.codef.dto.CodefApiResponseDTO<com.project.domain.codef.dto.CodefBankTransactionResponseDTO> mockResponse =
                com.project.domain.codef.dto.CodefApiResponseDTO.<com.project.domain.codef.dto.CodefBankTransactionResponseDTO>builder()
                        .result(com.project.domain.codef.dto.CodefResultDTO.builder().code("CF-00000").message("성공").build())
                        .data(bankData)
                        .build();

        when(codefService.getBankTransactionList(any())).thenReturn(mockResponse);

        com.project.domain.codef.dto.CodefBankTransactionListRequestDTO requestDTO = com.project.domain.codef.dto.CodefBankTransactionListRequestDTO.builder()
                .connectedId("demo_connected_id")
                .account("1234567890")
                .build();

        // when & then
        MvcResult mvcResult = mockMvc.perform(post("/api/codef/bank/transaction-list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        assertNotNull(responseContent);
        assertTrue(responseContent.contains("CF-00000"));
    }

    @Test
    @DisplayName("CODEF 카드 승인내역 조회 API 요청 컨트롤러 테스트")
    void getCardApprovalList_ControllerTest() throws Exception {
        // given
        com.project.domain.codef.dto.CodefCardApprovalResponseDTO cardData = com.project.domain.codef.dto.CodefCardApprovalResponseDTO.builder()
                .items(java.util.Collections.emptyList())
                .build();
        com.project.domain.codef.dto.CodefApiResponseDTO<com.project.domain.codef.dto.CodefCardApprovalResponseDTO> mockResponse =
                com.project.domain.codef.dto.CodefApiResponseDTO.<com.project.domain.codef.dto.CodefCardApprovalResponseDTO>builder()
                        .result(com.project.domain.codef.dto.CodefResultDTO.builder().code("CF-00000").message("성공").build())
                        .data(cardData)
                        .build();

        when(codefService.getCardApprovalList(any())).thenReturn(mockResponse);

        com.project.domain.codef.dto.CodefCardApprovalListRequestDTO requestDTO = com.project.domain.codef.dto.CodefCardApprovalListRequestDTO.builder()
                .connectedId("demo_connected_id")
                .build();

        // when & then
        MvcResult mvcResult = mockMvc.perform(post("/api/codef/card/approval-list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        assertNotNull(responseContent);
        assertTrue(responseContent.contains("CF-00000"));
    }

    @Test
    @DisplayName("CODEF Connected ID 생성 API 요청 컨트롤러 테스트")
    void createConnectedId_ControllerTest() throws Exception {
        // given
        com.project.domain.codef.dto.CodefConnectedIdResponseDTO idResponse = com.project.domain.codef.dto.CodefConnectedIdResponseDTO.builder()
                .connectedId("mock_connected_id")
                .build();

        com.project.domain.codef.dto.CodefApiResponseDTO<com.project.domain.codef.dto.CodefConnectedIdResponseDTO> mockResponse =
                com.project.domain.codef.dto.CodefApiResponseDTO.<com.project.domain.codef.dto.CodefConnectedIdResponseDTO>builder()
                        .result(com.project.domain.codef.dto.CodefResultDTO.builder().code("CF-00000").message("성공").build())
                        .data(idResponse)
                        .build();

        when(codefService.createConnectedId(any())).thenReturn(mockResponse);

        com.project.domain.codef.dto.CodefConnectedIdRequestDTO requestDTO = com.project.domain.codef.dto.CodefConnectedIdRequestDTO.builder()
                .accountList(java.util.Collections.singletonList(
                        com.project.domain.codef.dto.CodefAccountItemDTO.builder()
                                .countryCode("KR")
                                .businessType("BK")
                                .clientType("P")
                                .organization("0004")
                                .loginType("1")
                                .id("test_id")
                                .password("test_pw")
                                .build()
                ))
                .build();

        // when & then
        MvcResult mvcResult = mockMvc.perform(post("/api/codef/connected-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        assertNotNull(responseContent);
        assertTrue(responseContent.contains("mock_connected_id"));
    }
}

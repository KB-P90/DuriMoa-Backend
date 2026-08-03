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

        when(codefService.getAccessToken(any())).thenReturn(mockResponse);

        // when & then
        MvcResult mvcResult = mockMvc.perform(post("/api/codef/token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        assertNotNull(responseContent);
        assertTrue(responseContent.contains("mock_access_token"));
    }
}

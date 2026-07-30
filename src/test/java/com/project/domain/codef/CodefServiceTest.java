package com.project.domain.codef;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.config.CodefConfig;
import com.project.domain.codef.dto.CodefTokenRequestDTO;
import com.project.domain.codef.dto.CodefTokenResponseDTO;
import com.project.domain.codef.service.CodefServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodefServiceTest {

    @Mock
    private CodefConfig codefConfig;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CodefServiceImpl codefService;

    @BeforeEach
    void setUp() {
        when(codefConfig.getClientId()).thenReturn("test_client_id");
        when(codefConfig.getClientSecret()).thenReturn("test_client_secret");
        when(codefConfig.getOauthUrl()).thenReturn("https://oauth.codef.io/oauth/token");
    }

    @Test
    @DisplayName("CODEF OAuth2.0 Access Token 발급 성공 테스트")
    void getAccessToken_Success() {
        // given
        CodefTokenResponseDTO mockResponse = CodefTokenResponseDTO.builder()
                .accessToken("mock_access_token_12345")
                .tokenType("Bearer")
                .expiresIn(604800L)
                .scope("read")
                .build();

        when(restTemplate.postForEntity(
                eq("https://oauth.codef.io/oauth/token"),
                any(HttpEntity.class),
                eq(CodefTokenResponseDTO.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // when
        CodefTokenRequestDTO requestDTO = new CodefTokenRequestDTO();
        CodefTokenResponseDTO result = codefService.getAccessToken(requestDTO);

        // then
        assertNotNull(result);
        assertEquals("mock_access_token_12345", result.getAccessToken());
        assertEquals("Bearer", result.getTokenType());
    }
}

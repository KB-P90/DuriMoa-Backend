package com.project.domain.codef;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.config.CodefConfig;
import com.project.domain.codef.dto.CodefTokenDTO;
import com.project.domain.codef.mapper.CodefTokenMapper;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodefServiceTest {

    @Mock
    private CodefConfig codefConfig;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CodefTokenMapper codefTokenMapper;

    @InjectMocks
    private CodefServiceImpl codefService;

    @Test
    @DisplayName("DB에 저장된 유효한 CODEF Access Token 재사용 테스트")
    void getAccessToken_FromDB_Success() {
        // given
        CodefTokenDTO savedToken = CodefTokenDTO.builder()
                .id(1L)
                .accessToken("db_access_token_12345")
                .tokenType("Bearer")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .build();

        when(codefTokenMapper.selectLatestToken()).thenReturn(savedToken);

        // when
        CodefTokenResponseDTO result = codefService.getAccessToken();

        // then
        assertNotNull(result);
        assertEquals("db_access_token_12345", result.getAccessToken());
        assertEquals("Bearer", result.getTokenType());
    }

    @Test
    @DisplayName("CODEF OAuth2.0 Access Token 신규 발급 및 DB 저장 테스트")
    void getAccessToken_NewIssuance_Success() {
        // given
        when(codefConfig.getClientId()).thenReturn("test_client_id");
        when(codefConfig.getClientSecret()).thenReturn("test_client_secret");
        when(codefConfig.getOauthUrl()).thenReturn("https://oauth.codef.io/oauth/token");
        when(codefTokenMapper.selectLatestToken()).thenReturn(null);

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
        CodefTokenResponseDTO result = codefService.getAccessToken();

        // then
        assertNotNull(result);
        assertEquals("mock_access_token_12345", result.getAccessToken());
        assertEquals("Bearer", result.getTokenType());

        verify(codefTokenMapper).upsertToken(eq("mock_access_token_12345"), eq("Bearer"), any(LocalDateTime.class));
    }
}

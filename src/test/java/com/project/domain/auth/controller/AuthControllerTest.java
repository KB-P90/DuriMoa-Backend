package com.project.domain.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.common.security.RefreshTokenCookieProvider;
import com.project.domain.auth.dto.LoginResponseDTO;
import com.project.domain.auth.dto.LoginResultDTO;
import com.project.domain.auth.dto.LoginUserResponseDTO;
import com.project.domain.auth.dto.KakaoLoginResponseDTO;
import com.project.domain.auth.dto.KakaoLoginResultDTO;
import com.project.domain.auth.dto.TokenRefreshResponseDTO;
import com.project.domain.auth.dto.TokenRefreshResultDTO;
import com.project.domain.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RefreshTokenCookieProvider refreshTokenCookieProvider;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService, refreshTokenCookieProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void loginReturnsConfirmedResponseFormat() throws Exception {
        LoginUserResponseDTO user =
                new LoginUserResponseDTO(1L, "01012345678", "홍길동", "G", null, "LOCAL");
        when(authService.login(any())).thenReturn(new LoginResultDTO(
                new LoginResponseDTO("Bearer accessToken", user),
                "refreshToken"));
        when(refreshTokenCookieProvider.createCookie("refreshToken")).thenReturn(
                ResponseCookie.from("refreshToken", "refreshToken")
                        .httpOnly(true)
                        .path("/api/auth")
                        .build());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("로그인이 완료되었습니다.", response.get("message").asText());
        assertEquals("Bearer accessToken", response.at("/data/accessToken").asText());
        assertEquals(1L, response.at("/data/user/userId").asLong());
        assertEquals("01012345678", response.at("/data/user/phone").asText());
        assertEquals("LOCAL", response.at("/data/user/provider").asText());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(
                new CustomException(
                        ErrorCode.UNAUTHORIZED,
                        "휴대폰 번호 또는 비밀번호가 일치하지 않습니다."));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals(false, response.get("success").asBoolean());
        assertEquals(
                "휴대폰 번호 또는 비밀번호가 일치하지 않습니다.",
                response.get("message").asText());
        assertTrue(response.get("data").isNull());
    }

    @Test
    void loginReturnsBadRequestForInvalidInput() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"123\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals(false, response.get("success").asBoolean());
        assertTrue(response.get("data").isNull());
    }

    @Test
    void refreshReturnsNewAccessTokenAndRefreshTokenCookie() throws Exception {
        when(authService.refresh("refreshToken")).thenReturn(new TokenRefreshResultDTO(
                new TokenRefreshResponseDTO("Bearer newAccessToken"),
                "newRefreshToken"));
        when(refreshTokenCookieProvider.createCookie("newRefreshToken")).thenReturn(
                ResponseCookie.from("refreshToken", "newRefreshToken")
                        .httpOnly(true)
                        .path("/api/auth")
                        .build());

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new javax.servlet.http.Cookie("refreshToken", "refreshToken")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertEquals(
                "Bearer newAccessToken",
                response.at("/data/accessToken").asText());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    void kakaoLoginReturnsSignupTokenForNewUser() throws Exception {
        KakaoLoginResponseDTO response = KakaoLoginResponseDTO.signupRequired(
                "signupToken",
                "카카오 사용자",
                "https://example.com/profile.png");
        when(authService.loginWithKakao(any()))
                .thenReturn(new KakaoLoginResultDTO(response, null));

        MvcResult result = mockMvc.perform(post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"authorizationCode\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = readResponse(result);

        assertTrue(body.at("/data/signupRequired").asBoolean());
        assertEquals("signupToken", body.at("/data/signupToken").asText());
        assertEquals("카카오 사용자", body.at("/data/name").asText());
        assertEquals(null, result.getResponse().getHeader("Set-Cookie"));
    }

    @Test
    void kakaoLoginSetsRefreshTokenCookieForExistingUser() throws Exception {
        LoginUserResponseDTO user = new LoginUserResponseDTO(
                7L, null, "카카오 사용자", "B",
                "https://example.com/profile.png", "KAKAO");
        KakaoLoginResponseDTO response = KakaoLoginResponseDTO.loggedIn(
                "Bearer accessToken",
                user);
        when(authService.loginWithKakao(any()))
                .thenReturn(new KakaoLoginResultDTO(response, "refreshToken"));
        when(refreshTokenCookieProvider.createCookie("refreshToken")).thenReturn(
                ResponseCookie.from("refreshToken", "refreshToken")
                        .httpOnly(true)
                        .path("/api/auth")
                        .build());

        MvcResult result = mockMvc.perform(post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"authorizationCode\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = readResponse(result);

        assertEquals(false, body.at("/data/signupRequired").asBoolean());
        assertEquals("Bearer accessToken", body.at("/data/accessToken").asText());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    void kakaoSignupReturnsServiceTokensAndRefreshCookie() throws Exception {
        LoginUserResponseDTO user = new LoginUserResponseDTO(
                8L, null, "카카오 사용자", "G",
                "https://example.com/profile.png", "KAKAO");
        when(authService.signupWithKakao(any())).thenReturn(new LoginResultDTO(
                new LoginResponseDTO("Bearer accessToken", user),
                "refreshToken"));
        when(refreshTokenCookieProvider.createCookie("refreshToken")).thenReturn(
                ResponseCookie.from("refreshToken", "refreshToken")
                        .httpOnly(true)
                        .path("/api/auth")
                        .build());

        MvcResult result = mockMvc.perform(post("/api/auth/kakao/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"signupToken\":\"signupToken\","
                                + "\"role\":\"G\","
                                + "\"serviceTermsAgreed\":true,"
                                + "\"privacyTermsAgreed\":true,"
                                + "\"financeTermsAgreed\":true"
                                + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = readResponse(result);

        assertEquals("Bearer accessToken", body.at("/data/accessToken").asText());
        assertEquals("KAKAO", body.at("/data/user/provider").asText());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    void logoutExpiresRefreshTokenCookie() throws Exception {
        when(refreshTokenCookieProvider.deleteCookie()).thenReturn(
                ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .path("/api/auth")
                        .maxAge(0L)
                        .build());

        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(new javax.servlet.http.Cookie("refreshToken", "refreshToken")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("로그아웃이 완료되었습니다.", response.get("message").asText());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("Max-Age=0"));
        org.mockito.Mockito.verify(authService).logout("refreshToken");
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

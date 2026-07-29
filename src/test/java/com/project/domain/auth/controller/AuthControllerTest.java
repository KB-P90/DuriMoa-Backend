package com.project.domain.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.auth.dto.LoginResponseDTO;
import com.project.domain.auth.dto.LoginUserResponseDTO;
import com.project.domain.auth.exception.AuthExceptionHandler;
import com.project.domain.auth.exception.InvalidLoginCredentialsException;
import com.project.domain.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void loginReturnsConfirmedResponseFormat() throws Exception {
        LoginUserResponseDTO user =
                new LoginUserResponseDTO(1L, "01012345678", "홍길동", "G", null, "LOCAL");
        when(authService.login(any())).thenReturn(new LoginResponseDTO("accessToken", user));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("로그인이 완료되었습니다.", response.get("message").asText());
        assertEquals("accessToken", response.at("/data/accessToken").asText());
        assertEquals(1L, response.at("/data/user/userId").asLong());
        assertEquals("01012345678", response.at("/data/user/phone").asText());
        assertEquals("LOCAL", response.at("/data/user/provider").asText());
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidLoginCredentialsException());

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

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

package com.project.domain.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.user.dto.UserProfileDto;
import com.project.domain.user.dto.UserProfileUpdateRequestDto;
import com.project.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final Long AUTHENTICATED_USER_ID = 3L;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(AUTHENTICATED_USER_ID, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 8. 요청 id가 다른 사용자여도 인증 사용자 프로필만 수정됨
    // UserProfileUpdateRequestDto에는 id 필드 자체가 없으므로, 클라이언트가 임의의 id를
    // 함께 보내도 서비스에는 항상 인증된 userId가 별도 인자로 전달되는지 검증한다.
    @Test
    void updateUsesAuthenticatedUserIdRegardlessOfClientSuppliedId() throws Exception {
        UserProfileDto response = UserProfileDto.builder().id(AUTHENTICATED_USER_ID).name("홍길동").build();
        when(userService.updateProfile(eq(AUTHENTICATED_USER_ID), any(UserProfileUpdateRequestDto.class)))
                .thenReturn(response);

        MockMultipartFile image = new MockMultipartFile("profileImage", "", "image/png", new byte[0]);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/users/profile")
                        .file(image)
                        .param("id", "999")
                        .param("name", "홍길동")
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk());

        verify(userService).updateProfile(eq(AUTHENTICATED_USER_ID), any(UserProfileUpdateRequestDto.class));
    }

    @Test
    void checkPhoneAvailabilityReturnsServiceResult() throws Exception {
        when(userService.isPhoneAvailable(eq(AUTHENTICATED_USER_ID), eq("01012345678"))).thenReturn(true);

        MvcResult result = mockMvc.perform(get("/api/users/phone-availability")
                        .param("phone", "01012345678")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertTrue(response.at("/data/available").asBoolean());
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

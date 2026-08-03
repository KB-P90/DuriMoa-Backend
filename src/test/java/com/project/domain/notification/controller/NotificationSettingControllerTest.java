package com.project.domain.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.domain.notification.dto.NotificationSettingResponseDTO;
import com.project.domain.notification.dto.NotificationSettingUpdateRequestDTO;
import com.project.domain.notification.service.NotificationService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationSettingControllerTest {

    private NotificationService notificationService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        NotificationSettingController notificationSettingController =
                new NotificationSettingController(notificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(notificationSettingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSettingsReturnsAuthenticatedUsersSettings() throws Exception {
        Mockito.when(notificationService.getSettings(1L))
                .thenReturn(List.of(setting()));
        setAuthentication(1L);

        MvcResult result = mockMvc.perform(get("/api/notification-settings"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertTrue(response.get("success").asBoolean());
        Assertions.assertEquals("GOAL", response.at("/data/0/type").asText());
        Assertions.assertFalse(response.at("/data/0/pushEnabled").asBoolean());
        Mockito.verify(notificationService).getSettings(1L);
    }

    @Test
    void updateSettingDelegatesToService() throws Exception {
        setAuthentication(1L);
        NotificationSettingUpdateRequestDTO request = NotificationSettingUpdateRequestDTO.builder()
                .type("GOAL")
                .pushEnabled(false)
                .build();

        MvcResult result = mockMvc.perform(patch("/api/notification-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertEquals("알림 설정을 저장했습니다.", response.get("message").asText());
        Mockito.verify(notificationService).updateSetting(
                Mockito.eq(1L),
                Mockito.any(NotificationSettingUpdateRequestDTO.class)
        );
    }

    @Test
    void updateSettingRejectsInvalidRequest() throws Exception {
        setAuthentication(1L);

        MvcResult result = mockMvc.perform(patch("/api/notification-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"\",\"pushEnabled\":true}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertFalse(response.get("success").asBoolean());
    }

    private void setAuthentication(Long userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private NotificationSettingResponseDTO setting() {
        return NotificationSettingResponseDTO.builder()
                .id(1L)
                .userId(1L)
                .type("GOAL")
                .pushEnabled(false)
                .createdAt(LocalDateTime.of(2026, 7, 31, 10, 30))
                .build();
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

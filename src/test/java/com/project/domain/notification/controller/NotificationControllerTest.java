package com.project.domain.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.domain.notification.dto.NotificationDeleteBatchResponseDTO;
import com.project.domain.notification.dto.NotificationDeleteResponseDTO;
import com.project.domain.notification.dto.NotificationListResponseDTO;
import com.project.domain.notification.dto.NotificationReadAllResponseDTO;
import com.project.domain.notification.dto.NotificationReadResponseDTO;
import com.project.domain.notification.dto.NotificationResponseDTO;
import com.project.domain.notification.dto.NotificationUnreadCountResponseDTO;
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

class NotificationControllerTest {

    private NotificationService notificationService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        NotificationController notificationController =
                new NotificationController(notificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
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
    void getNotificationsReturnsAuthenticatedUsersNotifications() throws Exception {
        Mockito.when(notificationService.getNotifications(1L, 0, 20, false))
                .thenReturn(NotificationListResponseDTO.builder()
                        .notices(List.of(notification()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .hasNext(false)
                        .unreadCount(1)
                        .build());
        setAuthentication(1L);

        MvcResult result = mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "20")
                        .param("isRead", "false"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertTrue(response.get("success").asBoolean());
        Assertions.assertEquals("알림 목록 조회에 성공했습니다.", response.get("message").asText());
        Assertions.assertEquals(1L, response.at("/data/notices/0/noticeId").asLong());
        Mockito.verify(notificationService).getNotifications(1L, 0, 20, false);
    }

    @Test
    void getUnreadCountReturnsAuthenticatedUsersCount() throws Exception {
        Mockito.when(notificationService.getUnreadCount(1L))
                .thenReturn(NotificationUnreadCountResponseDTO.builder()
                        .unreadCount(2)
                        .build());
        setAuthentication(1L);

        MvcResult result = mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertEquals(2, response.at("/data/unreadCount").asInt());
        Mockito.verify(notificationService).getUnreadCount(1L);
    }

    @Test
    void markAsReadDelegatesToService() throws Exception {
        Mockito.when(notificationService.markAsRead(1L, 10L))
                .thenReturn(NotificationReadResponseDTO.builder()
                        .noticeId(10L)
                        .isRead(true)
                        .unreadCount(0)
                        .build());
        setAuthentication(1L);

        MvcResult result = mockMvc.perform(patch("/api/notifications/10/read"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertEquals("알림을 읽음 처리했습니다.", response.get("message").asText());
        Assertions.assertTrue(response.at("/data/isRead").asBoolean());
        Mockito.verify(notificationService).markAsRead(1L, 10L);
    }

    @Test
    void markAllAsReadDelegatesToService() throws Exception {
        Mockito.when(notificationService.markAllAsRead(1L))
                .thenReturn(NotificationReadAllResponseDTO.builder()
                        .updatedCount(3)
                        .unreadCount(0)
                        .build());
        setAuthentication(1L);

        MvcResult result = mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertEquals("모든 알림을 읽음 처리했습니다.", response.get("message").asText());
        Assertions.assertEquals(3, response.at("/data/updatedCount").asInt());
        Mockito.verify(notificationService).markAllAsRead(1L);
    }

    @Test
    void deleteNotificationDelegatesToService() throws Exception {
        Mockito.when(notificationService.deleteNotification(1L, 10L))
                .thenReturn(NotificationDeleteResponseDTO.builder()
                        .noticeId(10L)
                        .unreadCount(0)
                        .build());
        setAuthentication(1L);

        MvcResult result = mockMvc.perform(delete("/api/notifications/10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertEquals("알림을 삭제했습니다.", response.get("message").asText());
        Assertions.assertEquals(10L, response.at("/data/noticeId").asLong());
        Mockito.verify(notificationService).deleteNotification(1L, 10L);
    }

    @Test
    void deleteNotificationsDelegatesToService() throws Exception {
        Mockito.when(notificationService.deleteNotifications(Mockito.eq(1L), Mockito.any()))
                .thenReturn(NotificationDeleteBatchResponseDTO.builder()
                        .deletedCount(2)
                        .unreadCount(0)
                        .build());
        setAuthentication(1L);

        MvcResult result = mockMvc.perform(delete("/api/notifications/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noticeIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertEquals("선택한 알림을 삭제했습니다.", response.get("message").asText());
        Assertions.assertEquals(2, response.at("/data/deletedCount").asInt());
        Mockito.verify(notificationService).deleteNotifications(Mockito.eq(1L), Mockito.any());
    }

    @Test
    void getNotificationsReturnsUnauthorizedWithoutAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode response = readResponse(result);
        Assertions.assertFalse(response.get("success").asBoolean());
    }

    private void setAuthentication(Long userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private NotificationResponseDTO notification() {
        return NotificationResponseDTO.builder()
                .id(1L)
                .userId(1L)
                .type("GOAL")
                .title("목표 변경")
                .content("목표가 변경되었습니다.")
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 7, 31, 10, 30))
                .build();
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

package com.project.domain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.notification.domain.NotificationVO;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.dto.NotificationDeleteBatchRequestDTO;
import com.project.domain.notification.dto.NotificationDeleteBatchResponseDTO;
import com.project.domain.notification.dto.NotificationDeleteResponseDTO;
import com.project.domain.notification.dto.NotificationReadAllResponseDTO;
import com.project.domain.notification.dto.NotificationReadResponseDTO;
import com.project.domain.notification.dto.NotificationResponseDTO;
import com.project.domain.notification.dto.NotificationSettingUpdateRequestDTO;
import com.project.domain.notification.dto.NotificationUnreadCountResponseDTO;
import com.project.domain.notification.mapper.NotificationMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceImplTest {

    @Test
    void getNotificationsReturnsEmptyListWhenMapperReturnsNull() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        when(notificationMapper.selectNotificationsByUserId(1L)).thenReturn(null);

        List<NotificationResponseDTO> response = service.getNotifications(1L);

        assertEquals(Collections.emptyList(), response);
    }

    @Test
    void getUnreadCountReturnsZeroWhenMapperReturnsNull() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        when(notificationMapper.countUnreadNotifications(1L)).thenReturn(null);

        NotificationUnreadCountResponseDTO response = service.getUnreadCount(1L);

        assertEquals(0, response.getUnreadCount());
    }

    @Test
    void markAsReadUpdatesOnlyAuthenticatedUsersNotification() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        when(notificationMapper.existsNotification(1L, 10L)).thenReturn(1);
        when(notificationMapper.updateNotificationRead(1L, 10L)).thenReturn(1);
        when(notificationMapper.countUnreadNotifications(1L)).thenReturn(2);

        NotificationReadResponseDTO response = service.markAsRead(1L, 10L);

        assertEquals(10L, response.getNoticeId());
        assertEquals(true, response.getIsRead());
        assertEquals(2, response.getUnreadCount());
        verify(notificationMapper).updateNotificationRead(1L, 10L);
        verify(notificationMapper, never()).deleteNotification(1L, 10L);
    }

    @Test
    void markAsReadRejectsMissingNotification() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        when(notificationMapper.existsNotification(1L, 10L)).thenReturn(0);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.markAsRead(1L, 10L)
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void markAllAsReadUpdatesUnreadNotifications() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        when(notificationMapper.updateAllNotificationsRead(1L)).thenReturn(3);
        when(notificationMapper.countUnreadNotifications(1L)).thenReturn(0);

        NotificationReadAllResponseDTO response = service.markAllAsRead(1L);

        assertEquals(3, response.getUpdatedCount());
        assertEquals(0, response.getUnreadCount());
        verify(notificationMapper).updateAllNotificationsRead(1L);
    }

    @Test
    void deleteNotificationDeletesOnlyAuthenticatedUsersNotification() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        when(notificationMapper.existsNotification(1L, 10L)).thenReturn(1);
        when(notificationMapper.deleteNotification(1L, 10L)).thenReturn(1);
        when(notificationMapper.countUnreadNotifications(1L)).thenReturn(1);

        NotificationDeleteResponseDTO response = service.deleteNotification(1L, 10L);

        assertEquals(10L, response.getNoticeId());
        assertEquals(1, response.getUnreadCount());
        verify(notificationMapper).deleteNotification(1L, 10L);
    }

    @Test
    void deleteNotificationsDeduplicatesIdsBeforeDeleting() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        NotificationDeleteBatchRequestDTO request = NotificationDeleteBatchRequestDTO.builder()
                .noticeIds(List.of(1L, 2L, 2L, 3L))
                .build();
        when(notificationMapper.deleteNotifications(1L, List.of(1L, 2L, 3L))).thenReturn(3);
        when(notificationMapper.countUnreadNotifications(1L)).thenReturn(0);

        NotificationDeleteBatchResponseDTO response = service.deleteNotifications(1L, request);

        assertEquals(3, response.getDeletedCount());
        assertEquals(0, response.getUnreadCount());
        verify(notificationMapper).deleteNotifications(1L, List.of(1L, 2L, 3L));
    }

    @Test
    void deleteNotificationsRejectsEmptyIds() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        NotificationDeleteBatchRequestDTO request = NotificationDeleteBatchRequestDTO.builder()
                .noticeIds(Collections.emptyList())
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.deleteNotifications(1L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(notificationMapper, never()).deleteNotifications(any(), any());
    }

    @Test
    void updateSettingUpsertsUserNotificationSetting() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        NotificationSettingUpdateRequestDTO request = NotificationSettingUpdateRequestDTO.builder()
                .type("GOAL")
                .pushEnabled(false)
                .build();
        when(notificationMapper.upsertNotificationSetting(1L, "GOAL", false)).thenReturn(1);

        service.updateSetting(1L, request);

        verify(notificationMapper).upsertNotificationSetting(1L, "GOAL", false);
    }

    @Test
    void createNotificationSkipsInsertWhenSettingIsDisabled() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        NotificationCreateRequestDTO request = createRequest();
        when(notificationMapper.selectNotificationSettingEnabled(1L, "GOAL")).thenReturn(false);

        Long notificationId = service.createNotification(request);

        assertNull(notificationId);
        verify(notificationMapper, never()).insertNotification(any(NotificationVO.class));
    }

    @Test
    void createNotificationStoresNotificationWhenSettingIsMissing() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        NotificationCreateRequestDTO request = createRequest();
        when(notificationMapper.selectNotificationSettingEnabled(1L, "GOAL")).thenReturn(null);
        when(notificationMapper.insertNotification(any(NotificationVO.class))).thenAnswer(invocation -> {
            NotificationVO notification = invocation.getArgument(0);
            notification.setId(100L);
            return 1;
        });

        Long notificationId = service.createNotification(request);

        assertEquals(100L, notificationId);
        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        verify(notificationMapper).insertNotification(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("GOAL", captor.getValue().getType());
        assertEquals("목표 변경", captor.getValue().getTitle());
    }

    @Test
    void createNotificationRejectsLongType() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);
        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .userId(1L)
                .type("TOO_LONG_NOTIFICATION_TYPE_NAME_OVER_FIFTY_CHARACTERS")
                .title("목표 변경")
                .content("목표가 변경되었습니다.")
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createNotification(request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(notificationMapper, never()).insertNotification(any(NotificationVO.class));
    }

    @Test
    void getNotificationsRejectsUnauthenticatedUser() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationServiceImpl(notificationMapper);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.getNotifications(null)
        );

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    private NotificationCreateRequestDTO createRequest() {
        return NotificationCreateRequestDTO.builder()
                .userId(1L)
                .type("GOAL")
                .title("목표 변경")
                .content("목표가 변경되었습니다.")
                .build();
    }
}

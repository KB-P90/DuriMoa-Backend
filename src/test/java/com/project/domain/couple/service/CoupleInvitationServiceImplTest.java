package com.project.domain.couple.service;

import com.project.common.exception.CustomException;
import com.project.domain.couple.dto.CoupleInvitationDTO;
import com.project.domain.couple.mapper.CoupleInvitationMapper;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoupleInvitationServiceImplTest {

    @Mock
    private CoupleInvitationMapper coupleInvitationMapper;

    @Mock
    private NotificationService notificationService;

    private CoupleInvitationServiceImpl coupleInvitationService;

    @BeforeEach
    void setUp() {
        coupleInvitationService =
                new CoupleInvitationServiceImpl(coupleInvitationMapper, notificationService);
    }

    @Test
    void createInviteCodeRetriesUntilCodeIsUnique() {
        when(coupleInvitationMapper.existsInviteCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        coupleInvitationService.createInviteCode(1L);

        verify(coupleInvitationMapper, times(2)).existsInviteCode(anyString());
        verify(coupleInvitationMapper).insertInvitation(eq(1L), anyString(), any(LocalDateTime.class));
    }

    @Test
    void getStatusesReturnsEmptyListWhenNoCoupleExists() {
        when(coupleInvitationMapper.findCouplesByUserId(1L)).thenReturn(Collections.emptyList());

        assertTrue(coupleInvitationService.getStatuses(1L).isEmpty());
    }

    @Test
    void getStatusesReturnsAllPendingAndConnectedRelationships() {
        List<CoupleInvitationDTO> couples = List.of(
                new CoupleInvitationDTO(2L, "이유진", "B", "WAIT"),
                new CoupleInvitationDTO(3L, "박서연", "B", "WAIT"));
        when(coupleInvitationMapper.findCouplesByUserId(1L)).thenReturn(couples);

        List<CoupleInvitationDTO> result = coupleInvitationService.getStatuses(1L);

        assertEquals(2, result.size());
    }

    @Test
    void inviteRejectsInvalidInviteCode() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("INVALID")).thenReturn(null);

        assertThrows(CustomException.class,
                () -> coupleInvitationService.invite(1L, "INVALID"));
    }

    @Test
    void inviteRejectsSelfInviteCode() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("ABC123"))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));

        assertThrows(CustomException.class,
                () -> coupleInvitationService.invite(1L, "ABC123"));
    }

    @Test
    void inviteRejectsSameRole() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("XYZ789"))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "G", null));

        assertThrows(CustomException.class,
                () -> coupleInvitationService.invite(1L, "XYZ789"));
    }

    @Test
    void inviteRejectsWhenAlreadyConnectedToSomeoneElse() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("XYZ789"))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.countCoupleExcept(1L, 2L)).thenReturn(1);

        assertThrows(CustomException.class,
                () -> coupleInvitationService.invite(1L, "XYZ789"));
    }

    @Test
    void inviteAllowsSecondRequesterWhileFirstIsStillWaiting() {
        // C가 A(userId=1)의 코드를 입력하는 상황. A는 이미 B와 WAIT 상태지만 CONNECTED는 아니므로 countCoupleExcept는 0이어야 통과한다.
        when(coupleInvitationMapper.findUserById(3L)).thenReturn(new CoupleInvitationDTO(3L, "박서연", "B", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("AAA111"))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.countCoupleExcept(3L, 1L)).thenReturn(0);
        when(coupleInvitationMapper.countCoupleExcept(1L, 3L)).thenReturn(0);
        when(coupleInvitationMapper.findCoupleStatus(1L, 3L)).thenReturn(null);

        CoupleInvitationDTO result = coupleInvitationService.invite(3L, "AAA111");

        verify(coupleInvitationMapper).insertCouple(1L, 3L);
        verify(notificationService).createNotification(any(NotificationCreateRequestDTO.class));
        assertEquals("WAIT", result.getStatus());
    }

    @Test
    void inviteCreatesCoupleAsWaitOnFirstRequest() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("XYZ789"))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.countCoupleExcept(1L, 2L)).thenReturn(0);
        when(coupleInvitationMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleInvitationMapper.findCoupleStatus(1L, 2L)).thenReturn(null);

        CoupleInvitationDTO result = coupleInvitationService.invite(1L, "XYZ789");

        verify(coupleInvitationMapper).insertCouple(1L, 2L);
        verify(coupleInvitationMapper).requestInvitation("XYZ789", 1L);
        verify(coupleInvitationMapper, never()).connectCouple(any(), any());
        verify(notificationService).createNotification(any(NotificationCreateRequestDTO.class));
        assertEquals("WAIT", result.getStatus());
        assertEquals("이유진", result.getName());
    }

    @Test
    void inviteKeepsWaitingWhenNotificationFails() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("XYZ789"))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.countCoupleExcept(1L, 2L)).thenReturn(0);
        when(coupleInvitationMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleInvitationMapper.findCoupleStatus(1L, 2L)).thenReturn(null);
        doThrow(new RuntimeException("notification failed"))
                .when(notificationService).createNotification(any(NotificationCreateRequestDTO.class));

        CoupleInvitationDTO result = coupleInvitationService.invite(1L, "XYZ789");

        verify(coupleInvitationMapper).insertCouple(1L, 2L);
        verify(coupleInvitationMapper).requestInvitation("XYZ789", 1L);
        assertEquals("WAIT", result.getStatus());
    }

    @Test
    void inviteConnectsExistingWaitCoupleWhenBothEnterCodes() {
        when(coupleInvitationMapper.findUserById(2L)).thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("ABC123"))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleInvitationMapper.countCoupleExcept(1L, 2L)).thenReturn(0);
        when(coupleInvitationMapper.findCoupleStatus(1L, 2L)).thenReturn("WAIT");
        when(coupleInvitationMapper.existsRequestedInvitation(2L, 1L)).thenReturn(true);

        CoupleInvitationDTO result = coupleInvitationService.invite(2L, "ABC123");

        verify(coupleInvitationMapper).connectCouple(1L, 2L);
        verify(coupleInvitationMapper).acceptInvitation("ABC123", 2L);
        verify(coupleInvitationMapper).acceptOwnInvitation(2L, 1L);
        verify(coupleInvitationMapper).deleteOtherPendingRequests(1L, 2L);
        verify(coupleInvitationMapper).deleteOtherPendingInvitations(2L, 1L);
        verify(coupleInvitationMapper, never()).insertCouple(any(), any());
        verify(notificationService, times(2)).createNotification(any(NotificationCreateRequestDTO.class));
        assertEquals("CONNECTED", result.getStatus());
    }

    @Test
    void inviteKeepsWaitingWhenRequesterEntersSamePartnerCodeAgain() {
        when(coupleInvitationMapper.findUserById(2L)).thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("ABC123"))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleInvitationMapper.countCoupleExcept(1L, 2L)).thenReturn(0);
        when(coupleInvitationMapper.findCoupleStatus(1L, 2L)).thenReturn("WAIT");
        when(coupleInvitationMapper.existsRequestedInvitation(2L, 1L)).thenReturn(false);

        CoupleInvitationDTO result = coupleInvitationService.invite(2L, "ABC123");

        verify(coupleInvitationMapper, never()).connectCouple(any(), any());
        verify(coupleInvitationMapper, never()).acceptInvitation(anyString(), any());
        verify(coupleInvitationMapper, never()).acceptOwnInvitation(any(), any());
        verify(notificationService, never()).createNotification(any(NotificationCreateRequestDTO.class));
        assertEquals("WAIT", result.getStatus());
    }

    @Test
    void inviteRejectsWhenCoupleAlreadyConnected() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("XYZ789"))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.countCoupleExcept(1L, 2L)).thenReturn(0);
        when(coupleInvitationMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleInvitationMapper.findCoupleStatus(1L, 2L)).thenReturn("CONNECTED");

        assertThrows(CustomException.class,
                () -> coupleInvitationService.invite(1L, "XYZ789"));
    }

    @Test
    void inviteRejectsUnknownUser() {
        when(coupleInvitationMapper.findUserById(99L)).thenReturn(null);

        assertThrows(CustomException.class,
                () -> coupleInvitationService.invite(99L, "XYZ789"));
    }

    @Test
    void acceptConfirmsSpecifiedPendingRequest() {
        when(coupleInvitationMapper.findUserById(2L)).thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.findCoupleByUserIds(2L, 1L))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", "WAIT"));
        when(coupleInvitationMapper.countCoupleExcept(2L, 1L)).thenReturn(0);

        CoupleInvitationDTO result = coupleInvitationService.accept(2L, 1L);

        verify(coupleInvitationMapper).connectCouple(1L, 2L);
        verify(coupleInvitationMapper).acceptOwnInvitation(2L, 1L);
        verify(coupleInvitationMapper).deleteOtherPendingRequests(1L, 2L);
        verify(coupleInvitationMapper).deleteOtherPendingInvitations(2L, 1L);
        ArgumentCaptor<NotificationCreateRequestDTO> notificationCaptor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationService).createNotification(notificationCaptor.capture());
        NotificationCreateRequestDTO notification = notificationCaptor.getValue();
        assertEquals(1L, notification.getUserId());
        assertEquals("COUPLE_ACCEPTED", notification.getType());
        assertEquals("COUPLE", notification.getTargetType());
        assertEquals(2L, notification.getTargetId());
        assertEquals("CONNECTED", result.getStatus());
        assertEquals("김주영", result.getName());
    }

    @Test
    void acceptRejectsWhenNoPendingRequestFromThatPartner() {
        when(coupleInvitationMapper.findUserById(2L)).thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.findCoupleByUserIds(2L, 1L)).thenReturn(null);

        assertThrows(CustomException.class, () -> coupleInvitationService.accept(2L, 1L));
    }

    @Test
    void acceptRejectsWhenAlreadyConnectedToAnotherPartner() {
        // A(1)가 B(2)를 이미 수락해 CONNECTED된 상태에서, C(3)의 요청을 뒤늦게 수락하려는 상황
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findCoupleByUserIds(1L, 3L))
                .thenReturn(new CoupleInvitationDTO(3L, "박서연", "B", "WAIT"));
        when(coupleInvitationMapper.countCoupleExcept(1L, 3L)).thenReturn(1);

        assertThrows(CustomException.class, () -> coupleInvitationService.accept(1L, 3L));
        verify(coupleInvitationMapper, never()).connectCouple(any(), any());
    }

    @Test
    void acceptRejectsWhenPartnerIsAlreadyConnectedToSomeoneElse() {
        // B(2)가 A(1) 코드를 입력해 WAIT 상태를 만들어뒀는데, 그 사이 A가 C와 먼저 CONNECTED된 상황.
        // B 본인은 다른 CONNECTED가 없지만, 상대(A) 쪽이 이미 다른 사람과 CONNECTED라 막혀야 한다.
        when(coupleInvitationMapper.findUserById(2L)).thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.findCoupleByUserIds(2L, 1L))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", "WAIT"));
        when(coupleInvitationMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleInvitationMapper.countCoupleExcept(1L, 2L)).thenReturn(1);

        assertThrows(CustomException.class, () -> coupleInvitationService.accept(2L, 1L));
        verify(coupleInvitationMapper, never()).connectCouple(any(), any());
    }

    @Test
    void disconnectTurnsConnectedCoupleIntoDisconnected() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findCouplesByUserId(1L))
                .thenReturn(List.of(new CoupleInvitationDTO(2L, "이유진", "B", "CONNECTED")));

        CoupleInvitationDTO result = coupleInvitationService.disconnect(1L);

        verify(coupleInvitationMapper).disconnectCouple(1L, 2L);
        assertEquals("DISCONNECTED", result.getStatus());
        assertEquals("이유진", result.getName());
    }

    @Test
    void disconnectRejectsWhenNoConnectedCouple() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findCouplesByUserId(1L)).thenReturn(Collections.emptyList());

        assertThrows(CustomException.class, () -> coupleInvitationService.disconnect(1L));
        verify(coupleInvitationMapper, never()).disconnectCouple(any(), any());
    }

    @Test
    void disconnectRejectsWhenCoupleIsStillWaiting() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findCouplesByUserId(1L))
                .thenReturn(List.of(new CoupleInvitationDTO(2L, "이유진", "B", "WAIT")));

        assertThrows(CustomException.class, () -> coupleInvitationService.disconnect(1L));
        verify(coupleInvitationMapper, never()).disconnectCouple(any(), any());
    }
}

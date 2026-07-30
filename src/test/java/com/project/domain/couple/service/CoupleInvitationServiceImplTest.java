package com.project.domain.couple.service;

import com.project.common.exception.CustomException;
import com.project.domain.couple.dto.CoupleInvitationDTO;
import com.project.domain.couple.mapper.CoupleInvitationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoupleInvitationServiceImplTest {

    @Mock
    private CoupleInvitationMapper coupleInvitationMapper;

    private CoupleInvitationServiceImpl coupleInvitationService;

    @BeforeEach
    void setUp() {
        coupleInvitationService = new CoupleInvitationServiceImpl(coupleInvitationMapper);
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
    void getStatusReturnsNullWhenNoCoupleExists() {
        when(coupleInvitationMapper.findCoupleByUserId(1L)).thenReturn(null);

        assertNull(coupleInvitationService.getStatus(1L));
    }

    @Test
    void getStatusShowsRequestedWhenMyCodeWasUsed() {
        when(coupleInvitationMapper.findCoupleByUserId(1L))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", "WAIT"));
        when(coupleInvitationMapper.existsPendingRequestToMe(1L)).thenReturn(true);

        CoupleInvitationDTO result = coupleInvitationService.getStatus(1L);

        assertEquals("REQUESTED", result.getStatus());
    }

    @Test
    void getStatusShowsWaitWhenIUsedTheirCode() {
        when(coupleInvitationMapper.findCoupleByUserId(2L))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", "WAIT"));
        when(coupleInvitationMapper.existsPendingRequestToMe(2L)).thenReturn(false);

        CoupleInvitationDTO result = coupleInvitationService.getStatus(2L);

        assertEquals("WAIT", result.getStatus());
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
        assertEquals("WAIT", result.getStatus());
        assertEquals("이유진", result.getName());
    }

    @Test
    void inviteConnectsExistingWaitCoupleWhenBothEnterCodes() {
        when(coupleInvitationMapper.findUserById(2L)).thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.findPartnerByInviteCode("ABC123"))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleInvitationMapper.countCoupleExcept(1L, 2L)).thenReturn(0);
        when(coupleInvitationMapper.findCoupleStatus(1L, 2L)).thenReturn("WAIT");

        CoupleInvitationDTO result = coupleInvitationService.invite(2L, "ABC123");

        verify(coupleInvitationMapper).connectCouple(1L, 2L);
        verify(coupleInvitationMapper).acceptInvitation("ABC123", 2L);
        verify(coupleInvitationMapper).acceptOwnInvitation(2L);
        verify(coupleInvitationMapper, never()).insertCouple(any(), any());
        assertEquals("CONNECTED", result.getStatus());
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
    void acceptConfirmsPendingRequestWithoutCode() {
        when(coupleInvitationMapper.findUserById(2L)).thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.findCoupleByUserId(2L))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", "WAIT"));
        when(coupleInvitationMapper.existsPendingRequestToMe(2L)).thenReturn(true);

        CoupleInvitationDTO result = coupleInvitationService.accept(2L);

        verify(coupleInvitationMapper).connectCouple(1L, 2L);
        verify(coupleInvitationMapper).acceptOwnInvitation(2L);
        verify(coupleInvitationMapper, never()).acceptInvitation(anyString(), any());
        assertEquals("CONNECTED", result.getStatus());
        assertEquals("김주영", result.getName());
    }

    @Test
    void acceptRejectsWhenNoPendingRequest() {
        when(coupleInvitationMapper.findUserById(2L)).thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", null));
        when(coupleInvitationMapper.findCoupleByUserId(2L)).thenReturn(null);

        assertThrows(CustomException.class, () -> coupleInvitationService.accept(2L));
    }

    @Test
    void acceptRejectsWhenIAmTheWaitingSide() {
        when(coupleInvitationMapper.findUserById(1L)).thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", null));
        when(coupleInvitationMapper.findCoupleByUserId(1L))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", "WAIT"));
        when(coupleInvitationMapper.existsPendingRequestToMe(1L)).thenReturn(false);

        assertThrows(CustomException.class, () -> coupleInvitationService.accept(1L));
    }
}

package com.project.domain.couple.service;

import com.project.common.exception.CustomException;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.mapper.CoupleMapper;
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
class CoupleServiceImplTest {

    @Mock
    private CoupleMapper coupleMapper;

    private CoupleServiceImpl coupleService;

    @BeforeEach
    void setUp() {
        coupleService = new CoupleServiceImpl(coupleMapper);
    }

    @Test
    void createInviteCodeRetriesUntilCodeIsUnique() {
        when(coupleMapper.existsInviteCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        coupleService.createInviteCode(1L);

        verify(coupleMapper, times(2)).existsInviteCode(anyString());
        verify(coupleMapper).insertInvitation(eq(1L), anyString(), any(LocalDateTime.class));
    }

    @Test
    void getStatusReturnsNullWhenNoCoupleExists() {
        when(coupleMapper.findCoupleByUserId(1L)).thenReturn(null);

        assertNull(coupleService.getStatus(1L));
    }

    @Test
    void getStatusShowsRequestedWhenMyCodeWasUsed() {
        when(coupleMapper.findCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(2L, "이유진", "B", "WAIT"));
        when(coupleMapper.existsPendingRequestToMe(1L)).thenReturn(true);

        CoupleDTO result = coupleService.getStatus(1L);

        assertEquals("REQUESTED", result.getStatus());
    }

    @Test
    void getStatusShowsWaitWhenIUsedTheirCode() {
        when(coupleMapper.findCoupleByUserId(2L))
                .thenReturn(new CoupleDTO(1L, "김주영", "G", "WAIT"));
        when(coupleMapper.existsPendingRequestToMe(2L)).thenReturn(false);

        CoupleDTO result = coupleService.getStatus(2L);

        assertEquals("WAIT", result.getStatus());
    }

    @Test
    void inviteRejectsInvalidInviteCode() {
        when(coupleMapper.findUserById(1L)).thenReturn(new CoupleDTO(1L, "김주영", "G", null));
        when(coupleMapper.findPartnerByInviteCode("INVALID")).thenReturn(null);

        assertThrows(CustomException.class,
                () -> coupleService.invite(1L, "INVALID"));
    }

    @Test
    void inviteRejectsSelfInviteCode() {
        when(coupleMapper.findUserById(1L)).thenReturn(new CoupleDTO(1L, "김주영", "G", null));
        when(coupleMapper.findPartnerByInviteCode("ABC123"))
                .thenReturn(new CoupleDTO(1L, "김주영", "G", null));

        assertThrows(CustomException.class,
                () -> coupleService.invite(1L, "ABC123"));
    }

    @Test
    void inviteRejectsSameRole() {
        when(coupleMapper.findUserById(1L)).thenReturn(new CoupleDTO(1L, "김주영", "G", null));
        when(coupleMapper.findPartnerByInviteCode("XYZ789"))
                .thenReturn(new CoupleDTO(2L, "이유진", "G", null));

        assertThrows(CustomException.class,
                () -> coupleService.invite(1L, "XYZ789"));
    }

    @Test
    void inviteRejectsWhenAlreadyConnectedToSomeoneElse() {
        when(coupleMapper.findUserById(1L)).thenReturn(new CoupleDTO(1L, "김주영", "G", null));
        when(coupleMapper.findPartnerByInviteCode("XYZ789"))
                .thenReturn(new CoupleDTO(2L, "이유진", "B", null));
        when(coupleMapper.countCoupleExcept(1L, 2L)).thenReturn(1);

        assertThrows(CustomException.class,
                () -> coupleService.invite(1L, "XYZ789"));
    }

    @Test
    void inviteCreatesCoupleAsWaitOnFirstRequest() {
        when(coupleMapper.findUserById(1L)).thenReturn(new CoupleDTO(1L, "김주영", "G", null));
        when(coupleMapper.findPartnerByInviteCode("XYZ789"))
                .thenReturn(new CoupleDTO(2L, "이유진", "B", null));
        when(coupleMapper.countCoupleExcept(1L, 2L)).thenReturn(0);
        when(coupleMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleMapper.findCoupleStatus(1L, 2L)).thenReturn(null);

        CoupleDTO result = coupleService.invite(1L, "XYZ789");

        verify(coupleMapper).insertCouple(1L, 2L);
        verify(coupleMapper).requestInvitation("XYZ789", 1L);
        verify(coupleMapper, never()).connectCouple(any(), any());
        assertEquals("WAIT", result.getStatus());
        assertEquals("이유진", result.getName());
    }

    @Test
    void inviteConnectsExistingWaitCoupleWhenBothEnterCodes() {
        when(coupleMapper.findUserById(2L)).thenReturn(new CoupleDTO(2L, "이유진", "B", null));
        when(coupleMapper.findPartnerByInviteCode("ABC123"))
                .thenReturn(new CoupleDTO(1L, "김주영", "G", null));
        when(coupleMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleMapper.countCoupleExcept(1L, 2L)).thenReturn(0);
        when(coupleMapper.findCoupleStatus(1L, 2L)).thenReturn("WAIT");

        CoupleDTO result = coupleService.invite(2L, "ABC123");

        verify(coupleMapper).connectCouple(1L, 2L);
        verify(coupleMapper).acceptInvitation("ABC123", 2L);
        verify(coupleMapper).acceptOwnInvitation(2L);
        verify(coupleMapper, never()).insertCouple(any(), any());
        assertEquals("CONNECTED", result.getStatus());
    }

    @Test
    void inviteRejectsWhenCoupleAlreadyConnected() {
        when(coupleMapper.findUserById(1L)).thenReturn(new CoupleDTO(1L, "김주영", "G", null));
        when(coupleMapper.findPartnerByInviteCode("XYZ789"))
                .thenReturn(new CoupleDTO(2L, "이유진", "B", null));
        when(coupleMapper.countCoupleExcept(1L, 2L)).thenReturn(0);
        when(coupleMapper.countCoupleExcept(2L, 1L)).thenReturn(0);
        when(coupleMapper.findCoupleStatus(1L, 2L)).thenReturn("CONNECTED");

        assertThrows(CustomException.class,
                () -> coupleService.invite(1L, "XYZ789"));
    }

    @Test
    void inviteRejectsUnknownUser() {
        when(coupleMapper.findUserById(99L)).thenReturn(null);

        assertThrows(CustomException.class,
                () -> coupleService.invite(99L, "XYZ789"));
    }

    @Test
    void acceptConfirmsPendingRequestWithoutCode() {
        when(coupleMapper.findUserById(2L)).thenReturn(new CoupleDTO(2L, "이유진", "B", null));
        when(coupleMapper.findCoupleByUserId(2L))
                .thenReturn(new CoupleDTO(1L, "김주영", "G", "WAIT"));
        when(coupleMapper.existsPendingRequestToMe(2L)).thenReturn(true);

        CoupleDTO result = coupleService.accept(2L);

        verify(coupleMapper).connectCouple(1L, 2L);
        verify(coupleMapper).acceptOwnInvitation(2L);
        verify(coupleMapper, never()).acceptInvitation(anyString(), any());
        assertEquals("CONNECTED", result.getStatus());
        assertEquals("김주영", result.getName());
    }

    @Test
    void acceptRejectsWhenNoPendingRequest() {
        when(coupleMapper.findUserById(2L)).thenReturn(new CoupleDTO(2L, "이유진", "B", null));
        when(coupleMapper.findCoupleByUserId(2L)).thenReturn(null);

        assertThrows(CustomException.class, () -> coupleService.accept(2L));
    }

    @Test
    void acceptRejectsWhenIAmTheWaitingSide() {
        when(coupleMapper.findUserById(1L)).thenReturn(new CoupleDTO(1L, "김주영", "G", null));
        when(coupleMapper.findCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(2L, "이유진", "B", "WAIT"));
        when(coupleMapper.existsPendingRequestToMe(1L)).thenReturn(false);

        assertThrows(CustomException.class, () -> coupleService.accept(1L));
    }
}

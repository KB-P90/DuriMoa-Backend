package com.project.domain.couple.service;

import com.project.common.exception.CustomException;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.mapper.CoupleMapper;
import com.project.domain.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoupleServiceImplTest {

    @Mock
    private CoupleMapper coupleMapper;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private CoupleServiceImpl coupleService;

    @Test
    void returnsConnectedCoupleForMember() {
        CoupleDTO couple = new CoupleDTO(1L, 10L, 20L, "CONNECTED");
        when(memberService.existsMemberById(10L)).thenReturn(true);
        when(coupleMapper.findCouplesByUserId(10L)).thenReturn(List.of(couple));

        assertEquals(couple, coupleService.getConnectedCoupleByUserId(10L));
    }

    @Test
    void rejectsUserWithoutCouple() {
        when(memberService.existsMemberById(10L)).thenReturn(true);
        when(coupleMapper.findCouplesByUserId(10L)).thenReturn(Collections.emptyList());

        assertThrows(CustomException.class,
                () -> coupleService.getConnectedCoupleByUserId(10L));
    }

    @Test
    void rejectsNonConnectedCouple() {
        CoupleDTO couple = new CoupleDTO(1L, 10L, 20L, "WAIT");
        when(memberService.existsMemberById(10L)).thenReturn(true);
        when(coupleMapper.findCouplesByUserId(10L)).thenReturn(List.of(couple));

        assertThrows(CustomException.class,
                () -> coupleService.getConnectedCoupleByUserId(10L));
    }

    @Test
    void rejectsNullUserId() {
        assertThrows(CustomException.class,
                () -> coupleService.getConnectedCoupleByUserId(null));
    }

    @Test
    void rejectsDeletedOrUnknownUser() {
        when(memberService.existsMemberById(10L)).thenReturn(false);

        assertThrows(CustomException.class,
                () -> coupleService.getConnectedCoupleByUserId(10L));
    }
}

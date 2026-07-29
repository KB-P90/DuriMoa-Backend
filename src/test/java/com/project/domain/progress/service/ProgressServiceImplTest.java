package com.project.domain.progress.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.project.common.exception.SavingRecordNotFoundException;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.service.CoupleService;
import com.project.domain.progress.dto.CoupleSavingSummaryDTO;
import com.project.domain.progress.dto.MonthlySavingDTO;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.mapper.ProgressMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressServiceImplTest {

    @Mock
    private CoupleService coupleService;

    @Mock
    private ProgressMapper progressMapper;

    @InjectMocks
    private ProgressServiceImpl progressService;

    @Test
    void returnsProgressForConnectedCouple() {
        CoupleDTO couple = new CoupleDTO(1L, 10L, 20L, "CONNECTED");
        List<MonthlySavingDTO> monthlySavings = List.of(new MonthlySavingDTO("2026-07", 1_250_000L));
        when(coupleService.getConnectedCoupleForCurrentUser()).thenReturn(couple);
        when(progressMapper.existsSavingRecord(10L, 20L)).thenReturn(true);
        when(progressMapper.findTotalSavingAmount(10L, 20L)).thenReturn(1_250_000L);
        when(progressMapper.findCoupleSavingSummary(10L, 20L))
                .thenReturn(new CoupleSavingSummaryDTO(500_000L, 750_000L));
        when(progressMapper.findMonthlySavingAmounts(10L, 20L)).thenReturn(monthlySavings);

        ProgressResponseDTO result = progressService.getProgress();

        assertEquals(1_250_000L, result.getTotalSavingAmount());
        assertEquals(50_000_000L, result.getTargetAmount());
        assertEquals(2, result.getProgressRate());
        assertEquals(500_000L, result.getGroomSavingAmount());
        assertEquals(750_000L, result.getBrideSavingAmount());
        assertEquals(monthlySavings, result.getMonthlySavings());
    }

    @Test
    void allowsProgressRateOverOneHundred() {
        CoupleDTO couple = new CoupleDTO(1L, 10L, 20L, "CONNECTED");
        when(coupleService.getConnectedCoupleForCurrentUser()).thenReturn(couple);
        when(progressMapper.existsSavingRecord(10L, 20L)).thenReturn(true);
        when(progressMapper.findTotalSavingAmount(10L, 20L)).thenReturn(51_000_000L);
        when(progressMapper.findCoupleSavingSummary(10L, 20L))
                .thenReturn(new CoupleSavingSummaryDTO(25_000_000L, 26_000_000L));
        when(progressMapper.findMonthlySavingAmounts(10L, 20L)).thenReturn(List.of());

        ProgressResponseDTO result = progressService.getProgress();

        assertEquals(102, result.getProgressRate());
    }

    @Test
    void throwsExceptionWhenCoupleHasNoSavingRecord() {
        CoupleDTO couple = new CoupleDTO(1L, 10L, 20L, "CONNECTED");
        when(coupleService.getConnectedCoupleForCurrentUser()).thenReturn(couple);
        when(progressMapper.existsSavingRecord(10L, 20L)).thenReturn(false);

        assertThrows(SavingRecordNotFoundException.class, () -> progressService.getProgress());

        verify(progressMapper).existsSavingRecord(10L, 20L);
        verifyNoMoreInteractions(progressMapper);
    }
}

package com.project.domain.progress.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.service.CoupleService;
import com.project.domain.progress.dto.CoupleSavingSummaryDTO;
import com.project.domain.progress.dto.MonthlySavingDTO;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.mapper.ProgressMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private static final long TARGET_AMOUNT = 50_000_000L;

    private final CoupleService coupleService;
    private final ProgressMapper progressMapper;

    @Override
    @Transactional(readOnly = true)
    public ProgressResponseDTO getProgress() {
        CoupleDTO couple = coupleService.getConnectedCoupleForCurrentUser();
        Long groomId = couple.getGroomId();
        Long brideId = couple.getBrideId();

        if (!progressMapper.existsSavingRecord(groomId, brideId)) {
            throw new CustomException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "저축 거래 내역이 없습니다.");
        }

        Long totalSavingAmount = progressMapper.findTotalSavingAmount(groomId, brideId);
        CoupleSavingSummaryDTO savingSummary = progressMapper.findCoupleSavingSummary(groomId, brideId);
        List<MonthlySavingDTO> monthlySavings = progressMapper.findMonthlySavingAmounts(groomId, brideId);

        return ProgressResponseDTO.builder()
                .totalSavingAmount(totalSavingAmount)
                .targetAmount(TARGET_AMOUNT)
                .progressRate(calculateProgressRate(totalSavingAmount))
                .groomSavingAmount(savingSummary.getGroomSavingAmount())
                .brideSavingAmount(savingSummary.getBrideSavingAmount())
                .monthlySavings(monthlySavings == null ? Collections.emptyList() : monthlySavings)
                .build();
    }

    private int calculateProgressRate(Long totalSavingAmount) {
        return (int) (totalSavingAmount * 100 / TARGET_AMOUNT);
    }
}

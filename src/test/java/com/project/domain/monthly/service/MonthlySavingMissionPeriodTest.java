package com.project.domain.monthly.service;

import com.project.common.exception.CustomException;
import com.project.domain.monthly.dto.SavingMissionDTO;
import com.project.domain.monthly.dto.SavingMissionResponseDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class MonthlySavingMissionPeriodTest {

    @Mock
    private MonthlyMapper monthlyMapper;

    @InjectMocks
    private MonthlyServiceImpl monthlyService;

    @Test
    @DisplayName("이전 달 절약 미션은 선택할 수 없다.")
    void getMonthlySavingMissions_DisablesPreviousMonthMission() {
        Long userId = 1L;
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        SavingMissionDTO mission = SavingMissionDTO.builder()
                .missionId(14L)
                .expectedSavingAmount(30000L)
                .status("도전하기")
                .isSelectable(true)
                .build();
        Mockito.when(monthlyMapper.selectMonthlySavingMissions(
                        userId, previousMonth.getYear(), previousMonth.getMonthValue()))
                .thenReturn(List.of(mission));

        SavingMissionResponseDTO response = monthlyService.getMonthlySavingMissions(
                userId, previousMonth.getYear(), previousMonth.getMonthValue());

        Assertions.assertFalse(response.getMissions().get(0).getIsSelectable());
    }

    @Test
    @DisplayName("미래 달 절약 미션도 선택할 수 없다.")
    void getMonthlySavingMissions_DisablesFutureMonthMission() {
        Long userId = 1L;
        YearMonth futureMonth = YearMonth.now().plusMonths(1);
        SavingMissionDTO mission = SavingMissionDTO.builder()
                .missionId(15L)
                .expectedSavingAmount(30000L)
                .status("도전하기")
                .isSelectable(true)
                .build();
        Mockito.when(monthlyMapper.selectMonthlySavingMissions(
                        userId, futureMonth.getYear(), futureMonth.getMonthValue()))
                .thenReturn(List.of(mission));

        SavingMissionResponseDTO response = monthlyService.getMonthlySavingMissions(
                userId, futureMonth.getYear(), futureMonth.getMonthValue());

        Assertions.assertFalse(response.getMissions().get(0).getIsSelectable());
    }

    @Test
    @DisplayName("도전 조건에 맞지 않는 미션은 시작할 수 없다.")
    void startSavingMission_RejectsMissionWhenUpdateDoesNotMatch() {
        Long userId = 1L;
        Long missionId = 14L;
        Mockito.when(monthlyMapper.updateSavingMissionStatusToInProgress(userId, missionId))
                .thenReturn(0);

        CustomException exception = Assertions.assertThrows(
                CustomException.class,
                () -> monthlyService.startSavingMission(userId, missionId));

        Assertions.assertEquals(
                "현재 월의 도전하기 미션만 도전할 수 있습니다.",
                exception.getMessage());
    }
}

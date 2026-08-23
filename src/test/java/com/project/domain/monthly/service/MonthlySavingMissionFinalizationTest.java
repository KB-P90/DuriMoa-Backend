package com.project.domain.monthly.service;

import com.project.common.enums.MissionStatus;
import com.project.domain.monthly.mapper.MonthlyMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonthlySavingMissionFinalizationTest {

    @Mock
    private MonthlyMapper monthlyMapper;

    @Test
    void finalizeSavingMissionsEvaluatesInProgressAndExpiresChallenges() {
        MonthlyServiceImpl monthlyService = new MonthlyServiceImpl(
                monthlyMapper,
                Mockito.mock(MonthlyExpenseCacheService.class));
        YearMonth missionMonth = YearMonth.of(2026, 8);
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 9, 1);
        Mockito.when(monthlyMapper.updateInProgressSavingMissionResults(
                        startDate,
                        endDate,
                        MissionStatus.SUCCESS.getStatus(),
                        MissionStatus.FAILURE.getStatus(),
                        MissionStatus.IN_PROGRESS.getStatus()))
                .thenReturn(3);
        Mockito.when(monthlyMapper.expireUnstartedSavingMissions(
                        startDate,
                        endDate,
                        MissionStatus.CHALLENGE.getStatus(),
                        MissionStatus.EXPIRED.getStatus()))
                .thenReturn(2);

        int finalizedCount = monthlyService.finalizeSavingMissions(missionMonth);

        Assertions.assertEquals(5, finalizedCount);
        InOrder inOrder = Mockito.inOrder(monthlyMapper);
        inOrder.verify(monthlyMapper).updateInProgressSavingMissionResults(
                startDate,
                endDate,
                MissionStatus.SUCCESS.getStatus(),
                MissionStatus.FAILURE.getStatus(),
                MissionStatus.IN_PROGRESS.getStatus());
        inOrder.verify(monthlyMapper).expireUnstartedSavingMissions(
                startDate,
                endDate,
                MissionStatus.CHALLENGE.getStatus(),
                MissionStatus.EXPIRED.getStatus());
    }
}

package com.project.domain.monthly.scheduler;

import com.project.domain.monthly.service.MonthlyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.YearMonth;
import java.time.ZoneId;

class MonthlySavingMissionSchedulerTest {

    @Test
    void generateMonthlySavingMissions_UsesCurrentMonth() {
        MonthlyService monthlyService = Mockito.mock(MonthlyService.class);
        MonthlySavingMissionScheduler scheduler =
                new MonthlySavingMissionScheduler(monthlyService);

        scheduler.generateMonthlySavingMissions();

        ArgumentCaptor<YearMonth> finalizeCaptor = ArgumentCaptor.forClass(YearMonth.class);
        ArgumentCaptor<YearMonth> generateCaptor = ArgumentCaptor.forClass(YearMonth.class);
        Mockito.verify(monthlyService).finalizeSavingMissions(finalizeCaptor.capture());
        Mockito.verify(monthlyService).generateScheduledSavingMissions(generateCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1), finalizeCaptor.getValue());
        org.junit.jupiter.api.Assertions.assertEquals(
                YearMonth.now(ZoneId.of("Asia/Seoul")), generateCaptor.getValue());
    }
}

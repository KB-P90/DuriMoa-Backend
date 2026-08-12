package com.project.domain.monthly.scheduler;

import com.project.domain.monthly.service.MonthlyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.YearMonth;

class MonthlySavingMissionSchedulerTest {

    @Test
    void generateMonthlySavingMissions_UsesCurrentMonth() {
        MonthlyService monthlyService = Mockito.mock(MonthlyService.class);
        MonthlySavingMissionScheduler scheduler =
                new MonthlySavingMissionScheduler(monthlyService);

        scheduler.generateMonthlySavingMissions();

        ArgumentCaptor<YearMonth> captor = ArgumentCaptor.forClass(YearMonth.class);
        Mockito.verify(monthlyService).generateScheduledSavingMissions(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(YearMonth.now(), captor.getValue());
    }
}

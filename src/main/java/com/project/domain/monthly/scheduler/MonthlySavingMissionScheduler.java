package com.project.domain.monthly.scheduler;

import com.project.domain.monthly.service.MonthlyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;

@Log4j2
@Component
@RequiredArgsConstructor
public class MonthlySavingMissionScheduler {

    private static final ZoneId SCHEDULER_ZONE = ZoneId.of("Asia/Seoul");

    private final MonthlyService monthlyService;

    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Seoul")
    public void generateMonthlySavingMissions() {
        YearMonth targetMonth = YearMonth.now(SCHEDULER_ZONE);
        YearMonth completedMonth = targetMonth.minusMonths(1);
        int finalizedMissionCount = monthlyService.finalizeSavingMissions(completedMonth);
        int generatedUserCount = monthlyService.generateScheduledSavingMissions(targetMonth);
        log.info("월별 절약 미션 판정 및 자동 생성을 완료했습니다. completedMonth={}, finalizedMissionCount={}, "
                        + "targetMonth={}, generatedUserCount={}",
                completedMonth, finalizedMissionCount, targetMonth, generatedUserCount);
    }
}

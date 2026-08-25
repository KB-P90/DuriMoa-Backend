package com.project.domain.transaction.scheduler;

import com.project.domain.monthly.mapper.MonthlyMapper;
import com.project.domain.monthly.service.MonthlyService;
import com.project.domain.transaction.dto.TransactionSyncResponseDTO;
import com.project.domain.transaction.service.TransactionSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class DailyTransactionSyncScheduler {

    private final MonthlyMapper monthlyMapper;
    private final TransactionSyncService transactionSyncService;
    private final MonthlyService monthlyService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void syncTransactionsAndNotifyMissionExpenses() {
        List<Long> userIds = monthlyMapper.selectActiveUserIds();

        int successCount = 0;
        int failureCount = 0;

        for (Long userId : userIds) {
            try {
                TransactionSyncResponseDTO response = transactionSyncService.syncUserTransactions(userId);
                monthlyService.notifyInProgressMissionExpenses(userId, response.getNewExpenseRecords());
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("일일 거래 동기화 및 미션 알림 처리 실패 - userId: {}", userId, e);
            }
        }

        log.info("일일 거래 동기화 및 미션 알림 배치 완료 - totalUsers: {}, success: {}, failure: {}",
                userIds.size(), successCount, failureCount);
    }
}

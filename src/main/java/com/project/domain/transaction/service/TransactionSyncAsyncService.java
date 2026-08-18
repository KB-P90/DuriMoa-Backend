package com.project.domain.transaction.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TransactionSyncAsyncService {

    private final TransactionSyncService transactionSyncService;

    // TODO: 서버 여러 대 사용하게 되면 Redis 기반 lock 등 처리 필요
    // 현재 이미 sync 중인 사용자 관리: async 처리 중 중복 호출 방지
    private final ConcurrentHashMap<Long, Boolean> syncingUsers = new ConcurrentHashMap<>();

    public boolean tryStarySync(Long userId) {
        return syncingUsers.putIfAbsent(userId, true) == null;
    }

    public void finishSync(Long userId) {
        syncingUsers.remove(userId);
    }

    @Async
    public void syncUserTransactionsAsync(Long userId) {
        try {
            transactionSyncService.syncUserTransactions(userId);
        } finally {
            finishSync(userId);
        }
    }
}

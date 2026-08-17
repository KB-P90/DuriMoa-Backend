package com.project.domain.transaction.service;

import com.project.domain.transaction.dto.TransactionSyncResponseDTO;

public interface TransactionSyncService {

    public boolean needSync(Long userId);

    TransactionSyncResponseDTO syncUserTransactions(Long userId);
}

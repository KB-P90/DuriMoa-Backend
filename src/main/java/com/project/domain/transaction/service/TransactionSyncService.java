package com.project.domain.transaction.service;

import com.project.domain.transaction.dto.TransactionSyncResponseDTO;

public interface TransactionSyncService {

    TransactionSyncResponseDTO syncUserTransactions(Long userId);
}

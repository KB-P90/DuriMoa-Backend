package com.project.domain.transaction.service;

import com.project.common.config.RootConfig;
import com.project.domain.transaction.dto.TransactionSyncResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

//@Transactional
@SpringJUnitConfig(RootConfig.class)
class TransactionSyncServiceImplTest {

    @Autowired
    private TransactionSyncService transactionSyncService;

    private static final Long TEST_USER_ID = 5L;

    @Test
    void syncUserTransactions() {
        TransactionSyncResponseDTO response =
                transactionSyncService.syncUserTransactions(TEST_USER_ID);

        assertNotNull(response);

        System.out.println();
        System.out.println("========== CODEF 거래내역 동기화 결과 ==========");
        System.out.println("사용자 ID   : " + TEST_USER_ID);
        System.out.println("계좌 수     : " + response.getAccountCount());
        System.out.println("카드 수     : " + response.getCardCount());
        System.out.println("조회 건수   : " + response.getFetchedCount());
        System.out.println("저장 건수   : " + response.getSavedCount());
        System.out.println("중복 건수   : " + response.getDuplicateCount());
        System.out.println("===============================================");
        System.out.println();

        assertTrue(response.getFetchedCount() >= 0);
        assertTrue(response.getSavedCount() >= 0);
        assertTrue(response.getDuplicateCount() >= 0);

        assertEquals(
                response.getFetchedCount(),
                response.getSavedCount() + response.getDuplicateCount()
        );
    }
}
package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.dto.CardProductCollectionResultDTO;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CardGorillaCardProductSchedulerTest {

    // 일일 자동 수집 스케줄러는 카드고릴라 신용카드와 체크카드를 모두 수집합니다.
    @Test
    void collectDailyCardProductsCollectsAllCardTypes() {
        CardGorillaCardProductCollectionService collectionService =
                Mockito.mock(CardGorillaCardProductCollectionService.class);
        CardGorillaCardProductScheduler scheduler = new CardGorillaCardProductScheduler(collectionService);
        CardProductCollectionResultDTO result = CardProductCollectionResultDTO.builder()
                .requestedCount(1)
                .successCount(1)
                .failureCount(0)
                .failedProducts(Collections.emptyList())
                .build();
        Mockito.when(collectionService.collectCardProducts(null, "ALL")).thenReturn(result);

        scheduler.collectDailyCardProducts();

        Mockito.verify(collectionService).collectCardProducts(null, "ALL");
    }

    // 외부 수집 실패가 발생해도 스케줄러 메서드 밖으로 예외를 전파하지 않습니다.
    @Test
    void collectDailyCardProductsSwallowsCollectionException() {
        CardGorillaCardProductCollectionService collectionService =
                Mockito.mock(CardGorillaCardProductCollectionService.class);
        CardGorillaCardProductScheduler scheduler = new CardGorillaCardProductScheduler(collectionService);
        Mockito.when(collectionService.collectCardProducts(null, "ALL"))
                .thenThrow(new IllegalStateException("collection failed"));

        scheduler.collectDailyCardProducts();

        Mockito.verify(collectionService).collectCardProducts(null, "ALL");
    }
}

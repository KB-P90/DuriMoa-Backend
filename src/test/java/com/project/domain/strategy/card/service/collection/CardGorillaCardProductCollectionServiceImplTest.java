package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.dto.CardProductCollectRequestDTO;
import com.project.domain.strategy.card.dto.CardProductCollectionResultDTO;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardGorillaCardProductCollectionServiceImplTest {

    // 크롤링 결과를 기존 카드 상품 collection service로 전달합니다.
    @Test
    void collectCardProductsDelegatesCrawledProductsToCollectionService() {
        CardGorillaCardProductCrawler crawler = Mockito.mock(CardGorillaCardProductCrawler.class);
        CardProductCollectionService collectionService = Mockito.mock(CardProductCollectionService.class);
        CardGorillaCardProductCollectionServiceImpl service =
                new CardGorillaCardProductCollectionServiceImpl(crawler, collectionService);
        CardProductCollectRequestDTO product = CardProductCollectRequestDTO.builder()
                .company("KB국민")
                .name("KB 기본")
                .annualFee(10_000)
                .benefit("{\"benefits\":[]}")
                .conditionText("전월실적 없음")
                .build();
        CardProductCollectionResultDTO collectionResult = CardProductCollectionResultDTO.builder()
                .requestedCount(1)
                .successCount(1)
                .failureCount(0)
                .failedProducts(Collections.emptyList())
                .build();
        Mockito.when(crawler.crawlCardProducts(5, "ALL")).thenReturn(Collections.singletonList(product));
        Mockito.when(collectionService.collectProducts(Collections.singletonList(product))).thenReturn(collectionResult);

        CardProductCollectionResultDTO result = service.collectCardProducts(5, "ALL");

        Assertions.assertEquals(1, result.getSuccessCount());
        Mockito.verify(collectionService).collectProducts(Collections.singletonList(product));
    }

    // 기존 신용카드 전용 메서드는 CRD 유형 수집으로 위임합니다.
    @Test
    void collectCreditCardProductsDelegatesToCreditTypeCollection() {
        CardGorillaCardProductCrawler crawler = Mockito.mock(CardGorillaCardProductCrawler.class);
        CardProductCollectionService collectionService = Mockito.mock(CardProductCollectionService.class);
        CardGorillaCardProductCollectionServiceImpl service =
                new CardGorillaCardProductCollectionServiceImpl(crawler, collectionService);
        Mockito.when(crawler.crawlCardProducts(5, "CRD")).thenReturn(Collections.emptyList());
        Mockito.when(collectionService.collectProducts(Collections.emptyList()))
                .thenReturn(CardProductCollectionResultDTO.builder()
                        .requestedCount(0)
                        .successCount(0)
                        .failureCount(0)
                        .failedProducts(Collections.emptyList())
                        .build());

        service.collectCreditCardProducts(5);

        Mockito.verify(crawler).crawlCardProducts(5, "CRD");
    }
}

package com.project.domain.strategy.card.service;

import com.project.domain.strategy.card.dto.CardProductCollectRequestDTO;
import com.project.domain.strategy.card.dto.CardProductCollectionResultDTO;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductSource;
import com.project.domain.strategy.card.service.collection.CardProductCatalogWriter;
import com.project.domain.strategy.card.service.collection.CardProductCollectionServiceImpl;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardProductCollectionServiceImplTest {

    // 수집 서비스는 값을 정규화한 뒤 DB 작업을 트랜잭션 writer에 위임합니다.
    @Test
    void collectProductsNormalizesBenefitAndDelegatesToWriter() {
        CardProductCatalogWriter writer = Mockito.mock(CardProductCatalogWriter.class);
        CardProductCollectionServiceImpl service = new CardProductCollectionServiceImpl(writer);

        CardProductCollectionResultDTO result = service.collectProducts(Arrays.asList(
                product("신한", "신한 Mr.Life", 15_000)
        ));

        Assertions.assertEquals(1, result.getSuccessCount());
        ArgumentCaptor<CardProductSource> captor = ArgumentCaptor.forClass(CardProductSource.class);
        Mockito.verify(writer).upsertProduct(captor.capture());
        Assertions.assertEquals("신한", captor.getValue().getCompany());
        Assertions.assertEquals("신한 Mr.Life", captor.getValue().getName());
        Assertions.assertEquals("[\"생활 1% 할인\"]", captor.getValue().getBenefit());
    }

    // 검증 실패 상품은 기존 정상 상품 저장을 방해하지 않고 실패 목록에 남깁니다.
    @Test
    void collectProductsKeepsProcessingAfterInvalidProduct() {
        CardProductCatalogWriter writer = Mockito.mock(CardProductCatalogWriter.class);
        CardProductCollectionServiceImpl service = new CardProductCollectionServiceImpl(writer);

        CardProductCollectionResultDTO result = service.collectProducts(Arrays.asList(
                product("KB국민", "KB 웨딩", 10_000),
                product("현대", "현대 ZERO", -1)
        ));

        Assertions.assertEquals(1, result.getSuccessCount());
        Assertions.assertEquals(1, result.getFailureCount());
        Assertions.assertEquals("현대 현대 ZERO", result.getFailedProducts().get(0));
        Mockito.verify(writer, Mockito.times(1)).upsertProduct(Mockito.any(CardProductSource.class));
    }

    // 한 상품의 DB 작업이 실패해도 다음 상품 수집은 계속 진행합니다.
    @Test
    void collectProductsKeepsProcessingAfterWriterFailure() {
        CardProductCatalogWriter writer = Mockito.mock(CardProductCatalogWriter.class);
        CardProductCollectionServiceImpl service = new CardProductCollectionServiceImpl(writer);
        Mockito.doThrow(new RuntimeException("link failed"))
                .doNothing()
                .when(writer)
                .upsertProduct(Mockito.any(CardProductSource.class));

        CardProductCollectionResultDTO result = service.collectProducts(Arrays.asList(
                product("KB국민", "KB 웨딩", 10_000),
                product("신한", "신한 Mr.Life", 15_000)
        ));

        Assertions.assertEquals(1, result.getSuccessCount());
        Assertions.assertEquals(1, result.getFailureCount());
        Assertions.assertEquals("KB국민 KB 웨딩", result.getFailedProducts().get(0));
        Mockito.verify(writer, Mockito.times(2)).upsertProduct(Mockito.any(CardProductSource.class));
    }

    // 카드 상품 수집 테스트 데이터를 만듭니다.
    private CardProductCollectRequestDTO product(String company, String name, Integer annualFee) {
        return CardProductCollectRequestDTO.builder()
                .company(company)
                .name(name)
                .annualFee(annualFee)
                .image("/cards/test.png")
                .benefit("생활 1% 할인")
                .conditionText("전월 30만원 이상")
                .build();
    }
}

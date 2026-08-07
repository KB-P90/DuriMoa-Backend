package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.mapper.CardStrategyMapper;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductSource;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class CardProductCatalogWriterTest {

    // 신규 상품은 등록 후 사용자 보유 카드와 연결합니다.
    @Test
    void upsertProductInsertsNewProductAndLinksUserCards() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCatalogWriter writer = new CardProductCatalogWriter(mapper);
        CardProductSource source = source("KB국민", "KB 웨딩");
        Mockito.when(mapper.selectCardProductIdByCompanyAndName("KB국민", "KB 웨딩")).thenReturn(null);

        writer.upsertProduct(source);

        InOrder inOrder = Mockito.inOrder(mapper);
        inOrder.verify(mapper).selectCardProductIdByCompanyAndName("KB국민", "KB 웨딩");
        inOrder.verify(mapper).insertCardProduct(source);
        inOrder.verify(mapper).updateUserCardsProductIdByCompanyAndName(source);
        Mockito.verify(mapper, Mockito.never()).updateCardProductByCompanyAndName(source);
    }

    // 기존 상품은 갱신 후 사용자 보유 카드와 연결합니다.
    @Test
    void upsertProductUpdatesExistingProductAndLinksUserCards() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCatalogWriter writer = new CardProductCatalogWriter(mapper);
        CardProductSource source = source("신한", "신한 Mr.Life");
        Mockito.when(mapper.selectCardProductIdByCompanyAndName("신한", "신한 Mr.Life")).thenReturn(10L);

        writer.upsertProduct(source);

        InOrder inOrder = Mockito.inOrder(mapper);
        inOrder.verify(mapper).selectCardProductIdByCompanyAndName("신한", "신한 Mr.Life");
        inOrder.verify(mapper).updateCardProductByCompanyAndName(source);
        inOrder.verify(mapper).updateUserCardsProductIdByCompanyAndName(source);
        Mockito.verify(mapper, Mockito.never()).insertCardProduct(source);
    }

    // 상품 단위 DB 작업은 스프링 트랜잭션 경계 안에서 실행합니다.
    @Test
    void upsertProductIsTransactional() throws NoSuchMethodException {
        Method method = CardProductCatalogWriter.class.getMethod("upsertProduct", CardProductSource.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        Assertions.assertNotNull(transactional);
        Assertions.assertFalse(transactional.readOnly());
        Assertions.assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    // 사용자 카드 연결 실패는 호출자에게 전파되어 트랜잭션 롤백 대상이 됩니다.
    @Test
    void upsertProductPropagatesLinkFailure() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCatalogWriter writer = new CardProductCatalogWriter(mapper);
        CardProductSource source = source("현대", "현대 ZERO");
        RuntimeException failure = new RuntimeException("link failed");
        Mockito.when(mapper.selectCardProductIdByCompanyAndName("현대", "현대 ZERO")).thenReturn(20L);
        Mockito.when(mapper.updateUserCardsProductIdByCompanyAndName(source)).thenThrow(failure);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> writer.upsertProduct(source));

        Assertions.assertSame(failure, thrown);
    }

    private CardProductSource source(String company, String name) {
        return CardProductSource.builder()
                .company(company)
                .name(name)
                .annualFee(10_000)
                .image("/cards/test.png")
                .benefit("[\"생활 1% 할인\"]")
                .conditionText("전월 30만원 이상")
                .build();
    }
}

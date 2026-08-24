package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.mapper.CardStrategyMapper;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductSource;
import com.project.domain.strategy.card.model.CardStrategyRecord.UserCardRecord;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class CardProductCatalogWriterTest {

    // 신규 상품은 카드 상품 카탈로그에 등록합니다.
    @Test
    void upsertProductInsertsNewProduct() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCatalogWriter writer = new CardProductCatalogWriter(mapper);
        CardProductSource source = source("KB국민", "KB 웨딩");
        Mockito.when(mapper.selectCardProductIdByCompanyAndName("KB국민", "KB 웨딩")).thenReturn(null);

        writer.upsertProduct(source);

        InOrder inOrder = Mockito.inOrder(mapper);
        inOrder.verify(mapper).selectCardProductIdByCompanyAndName("KB국민", "KB 웨딩");
        inOrder.verify(mapper).insertCardProduct(source);
        Mockito.verify(mapper, Mockito.never()).updateCardProductByCompanyAndName(source);
    }

    // 기존 상품은 카드 상품 카탈로그의 최신 정보로 갱신합니다.
    @Test
    void upsertProductUpdatesExistingProduct() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCatalogWriter writer = new CardProductCatalogWriter(mapper);
        CardProductSource source = source("신한", "신한 Mr.Life");
        Mockito.when(mapper.selectCardProductIdByCompanyAndName("신한", "신한 Mr.Life")).thenReturn(10L);

        writer.upsertProduct(source);

        InOrder inOrder = Mockito.inOrder(mapper);
        inOrder.verify(mapper).selectCardProductIdByCompanyAndName("신한", "신한 Mr.Life");
        inOrder.verify(mapper).updateCardProductByCompanyAndName(source);
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

    // 수집된 전체 카드 상품 중 사용자 카드와 가장 비슷한 상품을 연결합니다.
    @Test
    void linkUserCardsToBestProductsUsesBestMatchingCardProduct() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCatalogWriter writer = new CardProductCatalogWriter(mapper);
        UserCardRecord userCard = userCard(100L, "신한카드", "신한카드 Mr.Life(후불교통) RF 체크");
        CardProductRecord deepDream = cardProduct(10L, "신한", "신한 Deep Dream");
        CardProductRecord mrLife = cardProduct(20L, "신한카드", "신한 Mr.Life");
        Mockito.when(mapper.selectUnlinkedUserCardsForMatching()).thenReturn(Collections.singletonList(userCard));
        Mockito.when(mapper.selectAllCardProductsForMatching()).thenReturn(Arrays.asList(deepDream, mrLife));

        writer.linkUserCardsToBestProducts();

        Mockito.verify(mapper).updateUserCardProductId(100L, 20L);
    }

    // 매칭 가능한 카드 상품이 없으면 사용자 카드 연결을 시도하지 않습니다.
    @Test
    void linkUserCardsToBestProductsSkipsWhenNoProductMatches() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCatalogWriter writer = new CardProductCatalogWriter(mapper);
        Mockito.when(mapper.selectUnlinkedUserCardsForMatching())
                .thenReturn(Collections.singletonList(userCard(100L, "현대", "ZERO")));
        Mockito.when(mapper.selectAllCardProductsForMatching())
                .thenReturn(Collections.singletonList(cardProduct(10L, "신한", "ZERO")));

        writer.linkUserCardsToBestProducts();

        Mockito.verify(mapper, Mockito.never()).updateUserCardProductId(Mockito.anyLong(), Mockito.anyLong());
    }

    // 사용자 카드 연결 작업도 스프링 트랜잭션 경계 안에서 실행합니다.
    @Test
    void linkUserCardsToBestProductsIsTransactional() throws NoSuchMethodException {
        Method method = CardProductCatalogWriter.class.getMethod("linkUserCardsToBestProducts");

        Transactional transactional = method.getAnnotation(Transactional.class);

        Assertions.assertNotNull(transactional);
        Assertions.assertFalse(transactional.readOnly());
        Assertions.assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    // 사용자 카드 연결 실패는 호출자에게 전파되어 트랜잭션 롤백 대상이 됩니다.
    @Test
    void linkUserCardsToBestProductsPropagatesLinkFailure() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCatalogWriter writer = new CardProductCatalogWriter(mapper);
        RuntimeException failure = new RuntimeException("link failed");
        Mockito.when(mapper.selectUnlinkedUserCardsForMatching())
                .thenReturn(Collections.singletonList(userCard(100L, "현대카드", "현대카드 ZERO")));
        Mockito.when(mapper.selectAllCardProductsForMatching())
                .thenReturn(Collections.singletonList(cardProduct(20L, "현대", "현대 ZERO")));
        Mockito.when(mapper.updateUserCardProductId(100L, 20L)).thenThrow(failure);

        RuntimeException thrown = Assertions.assertThrows(
                RuntimeException.class,
                writer::linkUserCardsToBestProducts
        );

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

    private UserCardRecord userCard(Long cardId, String company, String cardName) {
        UserCardRecord userCard = new UserCardRecord();
        userCard.setCardId(cardId);
        userCard.setCompany(company);
        userCard.setCardName(cardName);
        return userCard;
    }

    private CardProductRecord cardProduct(Long cardProductId, String company, String cardName) {
        CardProductRecord cardProduct = new CardProductRecord();
        cardProduct.setCardProductId(cardProductId);
        cardProduct.setCompany(company);
        cardProduct.setCardName(cardName);
        cardProduct.setConditionCheckedAt("2026-07-01 00:00:00");
        return cardProduct;
    }
}

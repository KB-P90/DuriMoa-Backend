package com.project.domain.strategy.card.service;

import com.project.domain.strategy.card.dto.CardProductCollectionDTO.CardProductCollectRequestDTO;
import com.project.domain.strategy.card.dto.CardProductCollectionDTO.CardProductCollectionResultDTO;
import com.project.domain.strategy.card.mapper.CardStrategyMapper;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductSource;
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

    // 기존 상품은 삭제 없이 카드사와 상품명 기준으로 갱신합니다.
    @Test
    void collectProductsUpdatesExistingProduct() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCollectionServiceImpl service = new CardProductCollectionServiceImpl(mapper);
        Mockito.when(mapper.selectCardProductIdByCompanyAndName("KB국민", "KB 웨딩")).thenReturn(10L);

        CardProductCollectionResultDTO result = service.collectProducts(Arrays.asList(product("KB국민", "KB 웨딩", 10_000)));

        Assertions.assertEquals(1, result.getSuccessCount());
        Mockito.verify(mapper).updateCardProductByCompanyAndName(Mockito.any(CardProductSource.class));
        Mockito.verify(mapper, Mockito.never()).insertCardProduct(Mockito.any(CardProductSource.class));
    }

    // 신규 상품은 혜택 텍스트를 JSON 배열 문자열로 정규화해 등록합니다.
    @Test
    void collectProductsInsertsNewProductWithNormalizedBenefit() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCollectionServiceImpl service = new CardProductCollectionServiceImpl(mapper);
        Mockito.when(mapper.selectCardProductIdByCompanyAndName("신한", "신한 Mr.Life")).thenReturn(null);

        service.collectProducts(Arrays.asList(product("신한", "신한 Mr.Life", 15_000)));

        ArgumentCaptor<CardProductSource> captor = ArgumentCaptor.forClass(CardProductSource.class);
        Mockito.verify(mapper).insertCardProduct(captor.capture());
        Assertions.assertEquals("[\"생활 1% 할인\"]", captor.getValue().getBenefit());
    }

    // 검증 실패 상품은 기존 정상 상품 저장을 방해하지 않고 실패 목록에 남깁니다.
    @Test
    void collectProductsKeepsProcessingAfterInvalidProduct() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardProductCollectionServiceImpl service = new CardProductCollectionServiceImpl(mapper);

        CardProductCollectionResultDTO result = service.collectProducts(Arrays.asList(
                product("KB국민", "KB 웨딩", 10_000),
                product("현대", "현대 ZERO", -1)
        ));

        Assertions.assertEquals(1, result.getSuccessCount());
        Assertions.assertEquals(1, result.getFailureCount());
        Assertions.assertEquals("현대 현대 ZERO", result.getFailedProducts().get(0));
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

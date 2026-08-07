package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.mapper.CardStrategyMapper;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CardProductCatalogWriter {

    private final CardStrategyMapper cardStrategyMapper;

    // 상품 등록/갱신과 사용자 보유 카드 연결은 한 상품 단위로 함께 성공하거나 롤백되어야 합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertProduct(CardProductSource source) {
        Long existingProductId = cardStrategyMapper.selectCardProductIdByCompanyAndName(
                source.getCompany(),
                source.getName()
        );

        if (existingProductId == null) {
            cardStrategyMapper.insertCardProduct(source);
        } else {
            cardStrategyMapper.updateCardProductByCompanyAndName(source);
        }

        cardStrategyMapper.updateUserCardsProductIdByCompanyAndName(source);
    }
}

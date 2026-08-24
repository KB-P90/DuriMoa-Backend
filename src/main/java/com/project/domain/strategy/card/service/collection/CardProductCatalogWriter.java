package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.mapper.CardStrategyMapper;
import com.project.domain.strategy.card.model.CardProductMatcher;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductSource;
import com.project.domain.strategy.card.model.CardStrategyRecord.UserCardRecord;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CardProductCatalogWriter {

    private final CardStrategyMapper cardStrategyMapper;
    private final CardProductMatcher cardProductMatcher = new CardProductMatcher();

    // 수집한 상품은 카드사와 상품명 기준으로 신규 등록하거나 최신 정보로 갱신합니다.
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
    }

    // 수집된 전체 상품 중 사용자 보유 카드와 가장 유사한 상품을 찾아 연결합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void linkUserCardsToBestProducts() {
        List<UserCardRecord> userCards = cardStrategyMapper.selectUnlinkedUserCardsForMatching();
        if (userCards == null || userCards.isEmpty()) {
            return;
        }
        List<CardProductRecord> cardProducts = cardStrategyMapper.selectAllCardProductsForMatching();
        if (cardProducts == null || cardProducts.isEmpty()) {
            return;
        }

        for (UserCardRecord userCard : userCards) {
            CardProductRecord matchedProduct = cardProductMatcher.findBestMatchingProduct(
                    userCard.getCompany(),
                    userCard.getCardName(),
                    cardProducts
            );
            if (matchedProduct == null) {
                continue;
            }
            cardStrategyMapper.updateUserCardProductId(
                    userCard.getCardId(),
                    matchedProduct.getCardProductId()
            );
        }
    }
}

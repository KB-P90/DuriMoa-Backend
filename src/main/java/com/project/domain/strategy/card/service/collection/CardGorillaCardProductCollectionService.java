package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.dto.CardProductCollectionResultDTO;

public interface CardGorillaCardProductCollectionService {

    // 카드고릴라 카드 상품을 크롤링해 카드 상품 DB에 등록 또는 갱신합니다.
    CardProductCollectionResultDTO collectCardProducts(Integer maxCount, String cardType);

    // 카드고릴라 신용카드 상품을 크롤링해 카드 상품 DB에 등록 또는 갱신합니다.
    CardProductCollectionResultDTO collectCreditCardProducts(Integer maxCount);
}

package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.dto.CardProductCollectRequestDTO;
import com.project.domain.strategy.card.dto.CardProductCollectionResultDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardGorillaCardProductCollectionServiceImpl implements CardGorillaCardProductCollectionService {

    // 카드고릴라 응답을 카드 상품 DTO로 변환하는 크롤러입니다.
    private final CardGorillaCardProductCrawler cardGorillaCardProductCrawler;

    // 정규화된 카드 상품 DTO를 DB에 upsert하는 기존 수집 서비스입니다.
    private final CardProductCollectionService cardProductCollectionService;

    // 카드고릴라 카드 상품을 수집해 기존 card_product 테이블 구조에 맞춰 저장합니다.
    @Override
    public CardProductCollectionResultDTO collectCardProducts(Integer maxCount, String cardType) {
        List<CardProductCollectRequestDTO> products = cardGorillaCardProductCrawler.crawlCardProducts(maxCount, cardType);
        return cardProductCollectionService.collectProducts(products);
    }

    // 카드고릴라 신용카드 상품만 수집해 기존 card_product 테이블 구조에 맞춰 저장합니다.
    @Override
    public CardProductCollectionResultDTO collectCreditCardProducts(Integer maxCount) {
        return collectCardProducts(maxCount, "CRD");
    }
}

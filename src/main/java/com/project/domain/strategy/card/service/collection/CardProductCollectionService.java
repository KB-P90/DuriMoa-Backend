package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.dto.CardProductCollectionDTO.CardProductCollectRequestDTO;
import com.project.domain.strategy.card.dto.CardProductCollectionDTO.CardProductCollectionResultDTO;
import java.util.List;

public interface CardProductCollectionService {

    // 카드 상품 수집 결과를 정규화해 신규 등록 또는 중복 갱신합니다.
    CardProductCollectionResultDTO collectProducts(List<CardProductCollectRequestDTO> products);
}

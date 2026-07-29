package com.project.domain.strategy.card.service;

import com.project.domain.strategy.card.dto.CardDetailResponseDTO;
import com.project.domain.strategy.card.dto.CardRecommendationResponseDTO;

public interface CardStrategyService {

    // 결제 예정 금액 기준으로 활성 커플의 카드 추천 결과를 조회합니다.
    CardRecommendationResponseDTO recommendCards(Long authenticatedUserId, Long paymentAmount);

    // 사용자 카드 식별자로 권한 검증 후 카드 상세 정보를 조회합니다.
    CardDetailResponseDTO getCardDetail(Long authenticatedUserId, Long userCardKey);
}

package com.project.domain.strategy.card.service;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
class CardProductRecommendationCandidate {

    // 카드 상품 식별자로 사용하는 card_product.id입니다.
    private final Long cardProductId;

    // 카드사 이름입니다.
    private final String company;

    // 카드 상품명입니다.
    private final String cardName;

    // 카드 상품 연회비입니다.
    private final Integer annualFee;

    // 카드 이미지 경로 또는 URL입니다.
    private final String image;

    // 입력 결제 금액 기준 예상 혜택 금액입니다.
    private final Long expectedBenefitAmount;

    // 상단 상품 추천 선정에 사용한 혜택률입니다.
    private final BigDecimal benefitRate;

    // 적용된 혜택의 월 한도입니다.
    private final Long monthlyBenefitLimit;

    // 적용된 혜택 이름입니다.
    private final String benefitTitle;

    // 전월 실적 등 카드 이용 조건 문구입니다.
    private final String conditionSummary;

    // 추천 계산 포함 가능 여부입니다.
    private final Boolean recommendable;

    // 카드 상품의 추천 가능 상태입니다.
    private final String recommendableStatus;

    // 추천 제외 또는 추정 계산 안내 문구입니다.
    private final String recommendationNotice;

    // 카드 상품 조건이 마지막으로 확인된 시점입니다.
    private final String conditionCheckedAt;
}

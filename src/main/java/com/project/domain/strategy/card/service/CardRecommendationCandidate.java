package com.project.domain.strategy.card.service;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
class CardRecommendationCandidate {

    // 카드 목록 안에서의 추천 순위입니다.
    private final Integer rank;

    // 사용자 보유 카드 식별자로 사용하는 card.id입니다.
    private final Long userCardKey;

    // 연결된 카드 상품 ID입니다.
    private final Long cardProductId;

    // 카드 소유 사용자 ID입니다.
    private final Long userId;

    // 카드 소유자 구분입니다. SELF 또는 PARTNER입니다.
    private final String ownerType;

    // 카드 소유자의 화면 표시 이름입니다.
    private final String ownerName;

    // 신랑·신부 구분에 사용하는 사용자 역할입니다.
    private final String ownerRole;

    // 카드사 이름입니다.
    private final String company;

    // 카드 상품명입니다.
    private final String cardName;

    // 카드 상품 연회비입니다.
    private final Integer annualFee;

    // 카드 이미지 경로 또는 URL입니다.
    private final String image;

    // 결제 예정 금액 기준 예상 혜택 금액입니다.
    private final Long expectedBenefitAmount;

    // 취소·환불을 차감한 현재 월 인정 실적입니다.
    private final Long currentPerformanceAmount;

    // 현재 인정 실적에 결제 예정 금액을 더한 금액입니다.
    private final Long performanceAfterPayment;

    // 현재 실적 이후 가장 가까운 혜택 구간입니다.
    private final Long nextBenefitThreshold;

    // 다음 혜택 구간까지 필요한 부족 금액입니다.
    private final Long shortageAmount;

    // 적용된 혜택의 월 한도입니다.
    private final Long monthlyBenefitLimit;

    // 적용된 혜택률입니다.
    private final BigDecimal benefitRate;

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

    // 기존 후보에 소유자 내 순위만 적용한 새 후보를 만듭니다.
    CardRecommendationCandidate withRank(Integer nextRank) {
        return CardRecommendationCandidate.builder()
                .rank(nextRank)
                .userCardKey(userCardKey)
                .cardProductId(cardProductId)
                .userId(userId)
                .ownerType(ownerType)
                .ownerName(ownerName)
                .ownerRole(ownerRole)
                .company(company)
                .cardName(cardName)
                .annualFee(annualFee)
                .image(image)
                .expectedBenefitAmount(expectedBenefitAmount)
                .currentPerformanceAmount(currentPerformanceAmount)
                .performanceAfterPayment(performanceAfterPayment)
                .nextBenefitThreshold(nextBenefitThreshold)
                .shortageAmount(shortageAmount)
                .monthlyBenefitLimit(monthlyBenefitLimit)
                .benefitRate(benefitRate)
                .conditionSummary(conditionSummary)
                .recommendable(recommendable)
                .recommendableStatus(recommendableStatus)
                .recommendationNotice(recommendationNotice)
                .conditionCheckedAt(conditionCheckedAt)
                .build();
    }
}

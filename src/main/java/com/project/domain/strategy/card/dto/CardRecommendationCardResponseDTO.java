package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardRecommendationCardResponseDTO {

    // 해당 소유자 카드 목록 안에서의 추천 순위입니다.
    @JsonProperty("rank")
    private final Integer rank;

    // 사용자 보유 카드 식별자로 사용하는 card.id입니다.
    @JsonProperty("user_card_key")
    private final Long userCardKey;

    // 연결된 카드 상품 ID입니다.
    @JsonProperty("card_product_id")
    private final Long cardProductId;

    // 카드사 이름입니다.
    @JsonProperty("company")
    private final String company;

    // 카드 상품명입니다.
    @JsonProperty("card_name")
    private final String cardName;

    // 카드 이미지 경로 또는 URL입니다.
    @JsonProperty("image")
    private final String image;

    // 카드 상품 연회비입니다.
    @JsonProperty("annual_fee")
    private final Integer annualFee;

    // 결제 예정 금액 기준 예상 혜택 금액입니다.
    @JsonProperty("expected_benefit_amount")
    private final Long expectedBenefitAmount;

    // 취소·환불을 차감한 현재 월 인정 실적입니다.
    @JsonProperty("current_performance_amount")
    private final Long currentPerformanceAmount;

    // 현재 인정 실적에 결제 예정 금액을 더한 금액입니다.
    @JsonProperty("performance_after_payment")
    private final Long performanceAfterPayment;

    // 현재 실적 이후 가장 가까운 혜택 구간입니다.
    @JsonProperty("next_benefit_threshold")
    private final Long nextBenefitThreshold;

    // 다음 혜택 구간까지 필요한 부족 금액입니다.
    @JsonProperty("shortage_amount")
    private final Long shortageAmount;

    // 적용된 혜택의 월 한도입니다.
    @JsonProperty("monthly_benefit_limit")
    private final Long monthlyBenefitLimit;

    // 적용된 혜택률입니다.
    @JsonProperty("benefit_rate")
    private final BigDecimal benefitRate;

    // 전월 실적 등 카드 이용 조건 문구입니다.
    @JsonProperty("condition_summary")
    private final String conditionSummary;

    // 추천 계산 포함 가능 여부입니다.
    @JsonProperty("recommendable")
    private final Boolean recommendable;

    // 카드 상품의 추천 가능 상태입니다.
    @JsonProperty("recommendable_status")
    private final String recommendableStatus;

    // 추천 제외 또는 추정 계산 안내 문구입니다.
    @JsonProperty("recommendation_notice")
    private final String recommendationNotice;

    // 카드 상품 조건이 마지막으로 확인된 시점입니다.
    @JsonProperty("condition_checked_at")
    private final String conditionCheckedAt;
}

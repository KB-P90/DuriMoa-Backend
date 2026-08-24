package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BestCardResponseDTO {

    // 사용자 보유 카드 추천일 때 사용하는 card.id입니다. 카드 상품 추천일 때는 null입니다.
    @JsonProperty("card_id")
    private final Long cardId;

    // 카드 상품 식별자로 사용하는 card_product.id입니다.
    @JsonProperty("card_product_id")
    private final Long cardProductId;

    // 상단 상품 추천에 사용한 카테고리 코드입니다.
    @JsonProperty("category")
    private final String category;

    // 상단 상품 추천에 사용한 카테고리 표시명입니다.
    @JsonProperty("category_name")
    private final String categoryName;

    // 카드 소유자 구분입니다. SELF 또는 PARTNER입니다.
    @JsonProperty("owner_type")
    private final String ownerType;

    // 카드 소유자의 화면 표시 이름입니다.
    @JsonProperty("owner_name")
    private final String ownerName;

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

    // 적용된 혜택률입니다.
    @JsonProperty("benefit_rate")
    private final BigDecimal benefitRate;

    // 적용된 혜택 이름입니다.
    @JsonProperty("benefit_title")
    private final String benefitTitle;

    // 적용된 혜택의 월 한도입니다.
    @JsonProperty("monthly_benefit_limit")
    private final Long monthlyBenefitLimit;

    // 전월 실적 등 카드 이용 조건 문구입니다.
    @JsonProperty("condition_summary")
    private final String conditionSummary;

    // 현재 카드의 추천 가능 상태입니다.
    @JsonProperty("recommendable_status")
    private final String recommendableStatus;

    // 추천 제외 또는 추정 계산 안내 문구입니다.
    @JsonProperty("recommendation_notice")
    private final String recommendationNotice;
}

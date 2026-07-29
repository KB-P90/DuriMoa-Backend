package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardDetailResponseDTO {

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

    // 카드 상품의 주요 혜택 목록입니다.
    @JsonProperty("benefits")
    private final List<CardBenefitResponseDTO> benefits;

    // 전월 실적 등 카드 이용 조건 문구입니다.
    @JsonProperty("condition")
    private final String condition;

    // 카드 상품 조건이 마지막으로 확인된 시점입니다.
    @JsonProperty("condition_checked_at")
    private final String conditionCheckedAt;

    // 현재 스키마에 공식 URL 컬럼이 없어 null로 내려가는 상품 상세 주소입니다.
    @JsonProperty("official_url")
    private final String officialUrl;

    // 현재 스키마에 판매 상태 컬럼이 없어 UNKNOWN으로 내려가는 판매 상태입니다.
    @JsonProperty("sale_status")
    private final String saleStatus;

    // 카드 상품의 추천 가능 상태입니다.
    @JsonProperty("recommendable_status")
    private final String recommendableStatus;

    // 예상 혜택이 확정 혜택이 아님을 안내하는 문구입니다.
    @JsonProperty("benefit_notice")
    private final String benefitNotice;

    @Getter
    @Builder
    public static class CardBenefitResponseDTO {

        // 혜택 영역 또는 혜택 이름입니다.
        @JsonProperty("title")
        private final String title;

        // 화면 상세에 표시할 혜택 설명입니다.
        @JsonProperty("description")
        private final String description;

        // 결제 금액에 곱하는 혜택률입니다.
        @JsonProperty("benefit_rate")
        private final BigDecimal benefitRate;

        // 비율 대신 고정으로 적용되는 혜택 금액입니다.
        @JsonProperty("fixed_benefit_amount")
        private final Long fixedBenefitAmount;

        // 한 달 동안 받을 수 있는 해당 혜택의 최대 금액입니다.
        @JsonProperty("monthly_limit_amount")
        private final Long monthlyLimitAmount;

        // 혜택 적용에 필요한 인정 실적 기준 금액입니다.
        @JsonProperty("minimum_performance_amount")
        private final Long minimumPerformanceAmount;

        // 제외 업종이나 제외 조건 안내 키워드입니다.
        @JsonProperty("excluded_keywords")
        private final List<String> excludedKeywords;
    }
}

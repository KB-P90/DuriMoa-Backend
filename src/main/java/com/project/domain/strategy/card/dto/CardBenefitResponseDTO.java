package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBenefitResponseDTO {

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

    // 혜택이 적용되는 소비 카테고리입니다.
    @JsonProperty("categories")
    private final List<String> categories;

    // 사용처 입력 없이도 확정 계산에 사용할 수 있는 혜택인지 여부입니다.
    @JsonProperty("amount_only_applicable")
    private final Boolean amountOnlyApplicable;

    // 제외 업종이나 제외 조건 안내 키워드입니다.
    @JsonProperty("excluded_keywords")
    private final List<String> excludedKeywords;
}

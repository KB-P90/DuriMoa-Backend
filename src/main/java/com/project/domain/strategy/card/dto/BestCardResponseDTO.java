package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BestCardResponseDTO {

    // 사용자 보유 카드 식별자로 사용하는 account.id입니다.
    @JsonProperty("user_card_key")
    private final Long userCardKey;

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

    // 결제 예정 금액 기준 예상 혜택 금액입니다.
    @JsonProperty("expected_benefit_amount")
    private final Long expectedBenefitAmount;

    // 적용된 혜택률입니다.
    @JsonProperty("benefit_rate")
    private final BigDecimal benefitRate;

    // 현재 카드의 추천 가능 상태입니다.
    @JsonProperty("recommendable_status")
    private final String recommendableStatus;
}

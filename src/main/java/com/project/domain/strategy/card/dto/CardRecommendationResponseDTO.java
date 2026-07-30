package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardRecommendationResponseDTO {

    // 사용자가 입력한 결제 예정 금액입니다.
    @JsonProperty("payment_amount")
    private final Long paymentAmount;

    // 전체 카드 중 예상 혜택이 가장 큰 카드입니다.
    @JsonProperty("best_card")
    private final BestCardResponseDTO bestCard;

    // 본인과 상대방별 카드 추천 목록입니다.
    @JsonProperty("owners")
    private final List<CardRecommendationOwnerResponseDTO> owners;

    // 계산 결과가 확정 혜택이 아님을 안내하는 문구입니다.
    @JsonProperty("benefit_notice")
    private final String benefitNotice;
}

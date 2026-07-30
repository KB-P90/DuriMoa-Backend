package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardRecommendationOwnerResponseDTO {

    // 카드 소유자 구분입니다. SELF 또는 PARTNER입니다.
    @JsonProperty("owner_type")
    private final String ownerType;

    // 카드 소유 사용자 ID입니다.
    @JsonProperty("user_id")
    private final Long userId;

    // 카드 소유자의 화면 표시 이름입니다.
    @JsonProperty("owner_name")
    private final String ownerName;

    // 신랑·신부 구분에 사용하는 사용자 역할입니다.
    @JsonProperty("owner_role")
    private final String ownerRole;

    // 해당 소유자의 보유 카드 개수입니다.
    @JsonProperty("card_count")
    private final Integer cardCount;

    // 해당 소유자의 카드별 추천 순위 목록입니다.
    @JsonProperty("cards")
    private final List<CardRecommendationCardResponseDTO> cards;
}

package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpectedBudgetRegionResponseDTO {

    // DB에 저장된 원본 지역명입니다.
    @JsonProperty("region")
    private final String region;

    // 홈 차트에서 짧게 표시할 지역명입니다.
    @JsonProperty("display_region")
    private final String displayRegion;

    // 균형형 기준으로 계산한 지역별 예상 예산 금액입니다.
    @JsonProperty("balanced_amount")
    private final Long balancedAmount;

    // 화면 표시 순서입니다.
    @JsonProperty("rank")
    private final Integer rank;
}

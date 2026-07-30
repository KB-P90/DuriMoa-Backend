package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardProductCollectionResultDTO {

    // 수집 요청으로 받은 상품 개수입니다.
    @JsonProperty("requested_count")
    private final Integer requestedCount;

    // 정상 등록 또는 갱신된 상품 개수입니다.
    @JsonProperty("success_count")
    private final Integer successCount;

    // 검증 또는 저장 실패 상품 개수입니다.
    @JsonProperty("failure_count")
    private final Integer failureCount;

    // 재시도 또는 점검이 필요한 상품명 목록입니다.
    @JsonProperty("failed_products")
    private final List<String> failedProducts;
}

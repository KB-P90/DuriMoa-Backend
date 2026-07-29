package com.project.domain.strategy.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

public final class CardProductCollectionDTO {

    // 카드 상품 수집 DTO를 묶기 위한 유틸리티 클래스입니다.
    private CardProductCollectionDTO() {
    }

    @Getter
    @Builder
    public static class CardProductCollectRequestDTO {

        // 중복 갱신 기준이 되는 카드사 이름입니다.
        private final String company;

        // 중복 갱신 기준이 되는 카드 상품명입니다.
        private final String name;

        // 카드 상품 연회비입니다.
        private final Integer annualFee;

        // 카드 이미지 경로 또는 URL입니다.
        private final String image;

        // 카드 혜택 원본 또는 정규화 대상 JSON 문자열입니다.
        private final String benefit;

        // 전월 실적 등 이용 조건 문구입니다.
        private final String conditionText;
    }

    @Getter
    @Builder
    public static class CardProductCollectionResultDTO {

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
}

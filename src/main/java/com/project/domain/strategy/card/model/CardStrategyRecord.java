package com.project.domain.strategy.card.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public final class CardStrategyRecord {

    // 카드 추천 DB 조회 결과 모델을 묶기 위한 클래스입니다.
    private CardStrategyRecord() {
    }

    @Getter
    @Setter
    public static class CardOwnerRecord {

        // 인증 사용자가 속한 활성 커플 ID입니다.
        private Long coupleId;

        // 카드 목록 조회 대상 사용자 ID입니다.
        private Long userId;

        // 카드 소유자의 화면 표시 이름입니다.
        private String ownerName;

        // 신랑·신부 구분에 사용하는 사용자 역할입니다.
        private String ownerRole;
    }

    @Getter
    @Setter
    public static class UserCardRecord {

        // 사용자 보유 카드 식별자로 사용하는 card.id입니다.
        private Long cardId;

        // 카드 소유 사용자 ID입니다.
        private Long userId;

        // 카드 소유자의 화면 표시 이름입니다.
        private String ownerName;

        // 신랑·신부 구분에 사용하는 사용자 역할입니다.
        private String ownerRole;

        // 연결된 카드 상품 ID입니다.
        private Long cardProductId;

        // 카드사 또는 금융기관 이름입니다.
        private String company;

        // 사용자 카드명 또는 카드 상품명입니다.
        private String cardName;

        // 카드 상품 연회비입니다.
        private Integer annualFee;

        // 카드 이미지 경로 또는 URL입니다.
        private String image;

        // 카드 상품 혜택 원본 JSON입니다.
        private String benefit;

        // 전월 실적 등 카드 이용 조건 문구입니다.
        private String conditionText;

        // 카드 상품 정보가 마지막으로 확인된 시점 문자열입니다.
        private String conditionCheckedAt;
    }

    @Getter
    @Setter
    public static class CardPerformanceRecord {

        // 사용자 보유 카드 식별자로 사용하는 card.id입니다.
        private Long cardId;

        // 매출에서 취소·환불을 차감한 인정 실적입니다.
        private Long recognizedPerformanceAmount;
    }

    @Getter
    @Setter
    public static class CardProductRecord {

        // 사용자 보유 카드 식별자로 사용하는 card.id입니다.
        private Long cardId;

        // 연결된 카드 상품 ID입니다.
        private Long cardProductId;

        // 카드사 이름입니다.
        private String company;

        // 카드 상품명입니다.
        private String cardName;

        // 카드 이미지 경로 또는 URL입니다.
        private String image;

        // 카드 상품 연회비입니다.
        private Integer annualFee;

        // 카드 상품 혜택 원본 JSON입니다.
        private String benefit;

        // 전월 실적 등 카드 이용 조건 문구입니다.
        private String conditionText;

        // 카드 상품 조건이 마지막으로 확인된 시점 문자열입니다.
        private String conditionCheckedAt;
    }

    @Getter
    @Builder
    public static class CardProductSource {

        // 중복 갱신 기준이 되는 카드사 이름입니다.
        private final String company;

        // 중복 갱신 기준이 되는 카드 상품명입니다.
        private final String name;

        // 카드 상품 연회비입니다.
        private final Integer annualFee;

        // 카드 이미지 경로 또는 URL입니다.
        private final String image;

        // 정규화된 혜택 JSON 문자열입니다.
        private final String benefit;

        // 전월 실적 등 이용 조건 문구입니다.
        private final String conditionText;
    }
}

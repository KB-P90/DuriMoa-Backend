package com.project.domain.strategy.card.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardProductRecord {

    // 사용자 보유 카드 식별자로 사용하는 card.id입니다.
    private Long userCardKey;

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

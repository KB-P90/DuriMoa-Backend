package com.project.domain.strategy.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardProductCollectRequestDTO {

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

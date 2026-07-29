package com.project.domain.strategy.card.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCardRecord {

    // 사용자 보유 카드 식별자로 사용하는 card.id입니다.
    private Long userCardKey;

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

    // 카드 상품 정보가 마지막으로 확인된 시점입니다.
    private LocalDateTime conditionCheckedAt;
}

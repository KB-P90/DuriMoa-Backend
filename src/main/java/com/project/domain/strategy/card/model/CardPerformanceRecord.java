package com.project.domain.strategy.card.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardPerformanceRecord {

    // 사용자 보유 카드 식별자로 사용하는 account.id입니다.
    private Long userCardKey;

    // 매출에서 취소·환불을 차감한 인정 실적입니다.
    private Long recognizedPerformanceAmount;
}

package com.project.domain.strategy.card.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardOwnerRecord {

    // 인증 사용자가 속한 활성 커플 ID입니다.
    private Long coupleId;

    // 카드 목록 조회 대상 사용자 ID입니다.
    private Long userId;

    // 카드 소유자의 화면 표시 이름입니다.
    private String ownerName;

    // 신랑·신부 구분에 사용하는 사용자 역할입니다.
    private String ownerRole;
}

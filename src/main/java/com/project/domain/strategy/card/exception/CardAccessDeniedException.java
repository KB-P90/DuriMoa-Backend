package com.project.domain.strategy.card.exception;

import org.springframework.http.HttpStatus;

public class CardAccessDeniedException extends CardStrategyException {

    // 카드 소유권 검증 실패를 403 오류로 전달합니다.
    public CardAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "조회 권한이 없는 카드입니다.");
    }
}

package com.project.domain.strategy.card.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CardStrategyException extends RuntimeException {

    // 카드 추천 API 오류 응답에 사용할 HTTP 상태입니다.
    private final HttpStatus status;

    // 카드 추천 도메인 예외의 상태와 메시지를 함께 보관합니다.
    public CardStrategyException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}

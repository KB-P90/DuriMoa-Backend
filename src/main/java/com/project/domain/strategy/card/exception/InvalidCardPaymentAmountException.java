package com.project.domain.strategy.card.exception;

import org.springframework.http.HttpStatus;

public class InvalidCardPaymentAmountException extends CardStrategyException {

    // 결제 예정 금액 검증 실패를 400 오류로 전달합니다.
    public InvalidCardPaymentAmountException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

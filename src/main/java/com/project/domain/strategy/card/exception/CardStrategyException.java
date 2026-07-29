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

    public static class InvalidCardPaymentAmountException extends CardStrategyException {

        // 결제 예정 금액 검증 실패를 400 오류로 전달합니다.
        public InvalidCardPaymentAmountException(String message) {
            super(HttpStatus.BAD_REQUEST, message);
        }
    }

    public static class CardAuthenticationRequiredException extends CardStrategyException {

        // 인증 사용자 식별 실패를 401 오류로 전달합니다.
        public CardAuthenticationRequiredException() {
            super(HttpStatus.UNAUTHORIZED, "인증 사용자 정보를 확인할 수 없습니다.");
        }
    }

    public static class CardAccessDeniedException extends CardStrategyException {

        // 카드 소유권 검증 실패를 403 오류로 전달합니다.
        public CardAccessDeniedException() {
            super(HttpStatus.FORBIDDEN, "조회 권한이 없는 카드입니다.");
        }
    }

    public static class CardNotFoundException extends CardStrategyException {

        // 카드 또는 활성 커플 조회 실패를 404 오류로 전달합니다.
        public CardNotFoundException(String message) {
            super(HttpStatus.NOT_FOUND, message);
        }
    }
}

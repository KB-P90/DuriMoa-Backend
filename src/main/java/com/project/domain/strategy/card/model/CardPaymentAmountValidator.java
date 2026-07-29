package com.project.domain.strategy.card.model;

import com.project.domain.strategy.card.exception.CardStrategyException.InvalidCardPaymentAmountException;

public final class CardPaymentAmountValidator {

    // 화면에서 허용하는 최소 결제 예정 금액입니다.
    public static final Long MIN_PAYMENT_AMOUNT = 1_000L;

    // 과도한 요청과 계산 오버플로를 막기 위한 결제 예정 금액 상한입니다.
    public static final Long MAX_PAYMENT_AMOUNT = 100_000_000L;

    private CardPaymentAmountValidator() {
    }

    // 결제 예정 금액 문자열을 원 단위 양수 금액으로 변환하고 검증합니다.
    public static Long validate(String amountText) {
        if (amountText == null || amountText.trim().isEmpty()) {
            throw new InvalidCardPaymentAmountException("결제 예정 금액은 필수입니다.");
        }

        // 콤마를 제거한 순수 숫자 금액 문자열입니다.
        String normalizedAmount = amountText.trim().replace(",", "");
        if (!normalizedAmount.matches("\\d+")) {
            throw new InvalidCardPaymentAmountException("결제 예정 금액은 숫자만 입력할 수 있습니다.");
        }

        // 검증이 끝난 원 단위 결제 예정 금액입니다.
        Long paymentAmount;
        try {
            paymentAmount = Long.valueOf(normalizedAmount);
        } catch (NumberFormatException exception) {
            throw new InvalidCardPaymentAmountException("결제 예정 금액이 허용 범위를 초과했습니다.");
        }

        if (paymentAmount < MIN_PAYMENT_AMOUNT) {
            throw new InvalidCardPaymentAmountException("결제 예정 금액은 1,000원 이상이어야 합니다.");
        }
        if (paymentAmount > MAX_PAYMENT_AMOUNT) {
            throw new InvalidCardPaymentAmountException("결제 예정 금액은 100,000,000원 이하만 입력할 수 있습니다.");
        }
        return paymentAmount;
    }
}

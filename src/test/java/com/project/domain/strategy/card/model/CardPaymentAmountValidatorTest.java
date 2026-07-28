package com.project.domain.strategy.card.model;

import com.project.domain.strategy.card.exception.InvalidCardPaymentAmountException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CardPaymentAmountValidatorTest {

    // 콤마가 포함된 결제 예정 금액을 원 단위 숫자로 변환합니다.
    @Test
    void validateAllowsCommaSeparatedAmount() {
        Long paymentAmount = CardPaymentAmountValidator.validate("300,000");

        Assertions.assertEquals(300_000L, paymentAmount);
    }

    // 화면 기준 최소 금액 미만 입력을 검증 오류로 처리합니다.
    @Test
    void validateRejectsAmountBelowMinimum() {
        Assertions.assertThrows(
                InvalidCardPaymentAmountException.class,
                () -> CardPaymentAmountValidator.validate("999")
        );
    }

    // 숫자가 아닌 결제 예정 금액 입력을 검증 오류로 처리합니다.
    @Test
    void validateRejectsNonNumericAmount() {
        Assertions.assertThrows(
                InvalidCardPaymentAmountException.class,
                () -> CardPaymentAmountValidator.validate("30만원")
        );
    }

    // 허용 범위를 초과한 결제 예정 금액을 검증 오류로 처리합니다.
    @Test
    void validateRejectsAmountOverMaximum() {
        Assertions.assertThrows(
                InvalidCardPaymentAmountException.class,
                () -> CardPaymentAmountValidator.validate("100000001")
        );
    }
}

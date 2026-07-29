package com.project.domain.strategy.card.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CardBenefitCalculatorTest {

    // 결제 예정 금액에 혜택률을 곱하고 원 단위 HALF_UP 반올림을 적용합니다.
    @Test
    void calculateRoundsPercentageBenefitHalfUp() {
        CardBenefitCalculator calculator = new CardBenefitCalculator();
        CardBenefitRule rule = CardBenefitRule.builder()
                .title("기본 적립")
                .benefitRate(new BigDecimal("0.015"))
                .benefitThresholds(Collections.singletonList(10_000L))
                .build();

        CardBenefitCalculation calculation = calculator.calculate(999L, 0L, Collections.singletonList(rule));

        Assertions.assertEquals(15L, calculation.getExpectedBenefitAmount());
    }

    // 현재 실적에서 이미 사용한 혜택을 차감해 월 혜택 잔여 한도를 적용합니다.
    @Test
    void calculateAppliesRemainingMonthlyBenefitLimit() {
        CardBenefitCalculator calculator = new CardBenefitCalculator();
        CardBenefitRule rule = CardBenefitRule.builder()
                .title("생활 할인")
                .benefitRate(new BigDecimal("0.10"))
                .monthlyLimitAmount(10_000L)
                .benefitThresholds(Arrays.asList(100_000L, 300_000L))
                .build();

        CardBenefitCalculation calculation = calculator.calculate(20_000L, 90_000L, Collections.singletonList(rule));

        Assertions.assertEquals(1_000L, calculation.getExpectedBenefitAmount());
        Assertions.assertEquals(100_000L, calculation.getNextBenefitThreshold());
        Assertions.assertEquals(10_000L, calculation.getShortageAmount());
    }

    // 결제 후에도 필요한 실적 기준에 못 미치면 혜택을 0원으로 계산합니다.
    @Test
    void calculateReturnsZeroWhenPaymentDoesNotReachRequiredPerformance() {
        CardBenefitCalculator calculator = new CardBenefitCalculator();
        CardBenefitRule rule = CardBenefitRule.builder()
                .title("웨딩 할인")
                .benefitRate(new BigDecimal("0.02"))
                .minimumPerformanceAmount(300_000L)
                .benefitThresholds(Collections.singletonList(300_000L))
                .build();

        CardBenefitCalculation calculation = calculator.calculate(50_000L, 200_000L, Collections.singletonList(rule));

        Assertions.assertEquals(0L, calculation.getExpectedBenefitAmount());
        Assertions.assertEquals(300_000L, calculation.getNextBenefitThreshold());
        Assertions.assertEquals(100_000L, calculation.getShortageAmount());
    }
}

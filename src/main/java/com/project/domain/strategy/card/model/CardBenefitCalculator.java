package com.project.domain.strategy.card.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CardBenefitCalculator {

    // 결제 예정 금액과 혜택률을 곱한 뒤 원 단위로 반올림하는 규칙입니다.
    private static final RoundingMode BENEFIT_ROUNDING_MODE = RoundingMode.HALF_UP;

    // 카드의 혜택 규칙 중 결제 예정 금액에 가장 유리한 혜택을 계산합니다.
    public CardBenefitCalculation calculate(Long paymentAmount, Long currentPerformanceAmount, List<CardBenefitRule> rules) {
        // 추천 계산에 반영할 원 단위 결제 예정 금액입니다.
        Long safePaymentAmount = Math.max(paymentAmount == null ? 0L : paymentAmount, 0L);
        // 취소·환불을 차감한 현재 월 인정 실적입니다.
        Long safeCurrentPerformanceAmount = Math.max(currentPerformanceAmount == null ? 0L : currentPerformanceAmount, 0L);
        // 결제 예정 금액이 반영된 예상 인정 실적입니다.
        Long performanceAfterPayment = safeCurrentPerformanceAmount + safePaymentAmount;
        // 현재 실적 이후 가장 가까운 혜택 구간입니다.
        Long nextBenefitThreshold = findNextBenefitThreshold(safeCurrentPerformanceAmount, rules);
        // 다음 혜택 구간까지 필요한 원 단위 부족 금액입니다.
        Long shortageAmount = nextBenefitThreshold == null
                ? 0L
                : Math.max(nextBenefitThreshold - safeCurrentPerformanceAmount, 0L);

        if (rules == null || rules.isEmpty()) {
            return CardBenefitCalculation.empty(safePaymentAmount, safeCurrentPerformanceAmount);
        }

        return rules.stream()
                .filter(this::isCalculable)
                .map(rule -> calculateRuleBenefit(rule, safePaymentAmount, safeCurrentPerformanceAmount, performanceAfterPayment))
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparing(CardBenefitCalculation::getExpectedBenefitAmount)
                        .thenComparing(calculation -> nullToZero(calculation.getAppliedBenefitRate()))
                        .thenComparing(calculation -> nullToZero(calculation.getMonthlyBenefitLimit())))
                .map(calculation -> CardBenefitCalculation.builder()
                        .expectedBenefitAmount(calculation.getExpectedBenefitAmount())
                        .currentPerformanceAmount(safeCurrentPerformanceAmount)
                        .performanceAfterPayment(performanceAfterPayment)
                        .nextBenefitThreshold(nextBenefitThreshold)
                        .shortageAmount(shortageAmount)
                        .monthlyBenefitLimit(calculation.getMonthlyBenefitLimit())
                        .appliedBenefitRate(calculation.getAppliedBenefitRate())
                        .appliedBenefitTitle(calculation.getAppliedBenefitTitle())
                        .build())
                .orElseGet(() -> CardBenefitCalculation.builder()
                        .expectedBenefitAmount(0L)
                        .currentPerformanceAmount(safeCurrentPerformanceAmount)
                        .performanceAfterPayment(performanceAfterPayment)
                        .nextBenefitThreshold(nextBenefitThreshold)
                        .shortageAmount(shortageAmount)
                        .build());
    }

    // 혜택 계산에 필요한 값과 제외 상태를 확인합니다.
    private Boolean isCalculable(CardBenefitRule rule) {
        return rule != null
                && !Boolean.TRUE.equals(rule.getExcluded())
                && Boolean.TRUE.equals(rule.hasBenefitValue());
    }

    // 단일 혜택 규칙으로 결제 예정 금액의 기대 혜택을 계산합니다.
    private CardBenefitCalculation calculateRuleBenefit(
            CardBenefitRule rule,
            Long paymentAmount,
            Long currentPerformanceAmount,
            Long performanceAfterPayment
    ) {
        // 혜택 적용에 필요한 인정 실적 기준 금액입니다.
        Long requiredPerformanceAmount = nullToZero(rule.getMinimumPerformanceAmount());
        if (performanceAfterPayment < requiredPerformanceAmount) {
            return null;
        }

        // 현재 실적으로 이미 사용했다고 보는 월 혜택 금액입니다.
        Long currentBenefitUsed = currentPerformanceAmount < requiredPerformanceAmount
                ? 0L
                : calculateRawBenefitAmount(currentPerformanceAmount, rule);
        // 결제 예정 금액 자체에서 발생하는 기대 혜택 금액입니다.
        Long expectedBenefitAmount = calculateRawBenefitAmount(paymentAmount, rule);
        // 해당 혜택 규칙에 적용되는 월 혜택 한도입니다.
        Long monthlyBenefitLimit = rule.getMonthlyLimitAmount();

        if (monthlyBenefitLimit != null) {
            // 현재 사용분을 차감한 남은 월 혜택 한도입니다.
            Long remainingBenefitLimit = Math.max(monthlyBenefitLimit - currentBenefitUsed, 0L);
            expectedBenefitAmount = Math.min(expectedBenefitAmount, remainingBenefitLimit);
        }

        return CardBenefitCalculation.builder()
                .expectedBenefitAmount(Math.max(expectedBenefitAmount, 0L))
                .monthlyBenefitLimit(monthlyBenefitLimit)
                .appliedBenefitRate(rule.getBenefitRate())
                .appliedBenefitTitle(rule.getTitle())
                .build();
    }

    // 비율 또는 고정 혜택을 원 단위 기대 혜택으로 변환합니다.
    private Long calculateRawBenefitAmount(Long baseAmount, CardBenefitRule rule) {
        if (rule.getFixedBenefitAmount() != null) {
            return Math.max(rule.getFixedBenefitAmount(), 0L);
        }
        return BigDecimal.valueOf(baseAmount)
                .multiply(rule.getBenefitRate())
                .setScale(0, BENEFIT_ROUNDING_MODE)
                .longValue();
    }

    // 현재 실적보다 큰 가장 가까운 혜택 구간을 찾습니다.
    private Long findNextBenefitThreshold(Long currentPerformanceAmount, List<CardBenefitRule> rules) {
        if (rules == null) {
            return null;
        }
        return rules.stream()
                .flatMap(rule -> rule.safeBenefitThresholds().stream())
                .filter(Objects::nonNull)
                .filter(threshold -> threshold > currentPerformanceAmount)
                .min(Long::compareTo)
                .orElse(null);
    }

    // 비어 있는 금액 값을 비교 가능한 0으로 바꿉니다.
    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    // 비어 있는 비율 값을 비교 가능한 0으로 바꿉니다.
    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

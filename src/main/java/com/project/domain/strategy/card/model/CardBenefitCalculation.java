package com.project.domain.strategy.card.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBenefitCalculation {

    // 결제 예정 금액으로 추가 기대되는 혜택 금액입니다.
    private final Long expectedBenefitAmount;

    // 취소·환불을 차감한 현재 월 인정 실적입니다.
    private final Long currentPerformanceAmount;

    // 현재 인정 실적에 결제 예정 금액을 더한 금액입니다.
    private final Long performanceAfterPayment;

    // 현재 실적 이후 가장 가까운 혜택 구간입니다.
    private final Long nextBenefitThreshold;

    // 다음 혜택 구간까지 필요한 부족 금액입니다.
    private final Long shortageAmount;

    // 적용된 혜택의 월 한도입니다.
    private final Long monthlyBenefitLimit;

    // 적용된 혜택률입니다.
    private final BigDecimal appliedBenefitRate;

    // 적용된 혜택 이름입니다.
    private final String appliedBenefitTitle;

    // 혜택 계산이 불가능할 때 사용하는 빈 계산 결과입니다.
    public static CardBenefitCalculation empty(Long paymentAmount, Long currentPerformanceAmount) {
        Long safeCurrentPerformanceAmount = Math.max(currentPerformanceAmount == null ? 0L : currentPerformanceAmount, 0L);
        Long safePaymentAmount = Math.max(paymentAmount == null ? 0L : paymentAmount, 0L);
        return CardBenefitCalculation.builder()
                .expectedBenefitAmount(0L)
                .currentPerformanceAmount(safeCurrentPerformanceAmount)
                .performanceAfterPayment(safeCurrentPerformanceAmount + safePaymentAmount)
                .shortageAmount(0L)
                .build();
    }
}

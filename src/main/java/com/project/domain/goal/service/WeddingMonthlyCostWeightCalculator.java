package com.project.domain.goal.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WeddingMonthlyCostWeightCalculator {

    private static final Integer WEIGHT_SCALE = 10;
    private static final RoundingMode WEIGHT_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final RoundingMode AMOUNT_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final Long AMOUNT_ROUNDING_UNIT = 10_000L;
    private static final Long VENUE_BASE_PRICE = 1_640L;
    private static final Long SDM_BASE_PRICE = 290L;

    // 2025년 7월 조회 기준 전국 중간값에서 2025년 7월~2026년 6월 예식월 가격을 사용합니다.
    private static final Map<Month, Long> VENUE_MONTHLY_PRICES = Map.ofEntries(
            Map.entry(Month.JANUARY, 1_190L),
            Map.entry(Month.FEBRUARY, 1_248L),
            Map.entry(Month.MARCH, 1_650L),
            Map.entry(Month.APRIL, 1_725L),
            Map.entry(Month.MAY, 1_740L),
            Map.entry(Month.JUNE, VENUE_BASE_PRICE),
            Map.entry(Month.JULY, 1_272L),
            Map.entry(Month.AUGUST, 1_220L),
            Map.entry(Month.SEPTEMBER, 1_070L),
            Map.entry(Month.OCTOBER, 1_420L),
            Map.entry(Month.NOVEMBER, 1_230L),
            Map.entry(Month.DECEMBER, 1_360L)
    );

    // 스드메 패키지 가중치는 스튜디오·드레스·메이크업 통계에 동일하게 적용합니다.
    private static final Map<Month, Long> SDM_MONTHLY_PRICES = Map.ofEntries(
            Map.entry(Month.JANUARY, 280L),
            Map.entry(Month.FEBRUARY, 276L),
            Map.entry(Month.MARCH, 280L),
            Map.entry(Month.APRIL, 289L),
            Map.entry(Month.MAY, 289L),
            Map.entry(Month.JUNE, SDM_BASE_PRICE),
            Map.entry(Month.JULY, 270L),
            Map.entry(Month.AUGUST, 270L),
            Map.entry(Month.SEPTEMBER, 280L),
            Map.entry(Month.OCTOBER, 300L),
            Map.entry(Month.NOVEMBER, 298L),
            Map.entry(Month.DECEMBER, 280L)
    );

    private static final Set<String> SDM_CATEGORIES = Set.of(
            "스튜디오",
            "드레스",
            "메이크업"
    );

    public Long adjustAmount(String category, Month weddingMonth, Long amount) {
        if (amount == null || amount <= 0L) {
            return 0L;
        }
        return BigDecimal.valueOf(amount)
                .multiply(resolveWeight(category, weddingMonth))
                .divide(BigDecimal.valueOf(AMOUNT_ROUNDING_UNIT), 0, AMOUNT_ROUNDING_MODE)
                .multiply(BigDecimal.valueOf(AMOUNT_ROUNDING_UNIT))
                .longValue();
    }

    public BigDecimal resolveWeight(String category, Month weddingMonth) {
        if (weddingMonth == null) {
            throw new IllegalArgumentException("예식 예정 월은 필수입니다.");
        }
        if ("예식장".equals(category)) {
            return calculateWeight(VENUE_MONTHLY_PRICES.get(weddingMonth), VENUE_BASE_PRICE);
        }
        if (SDM_CATEGORIES.contains(category)) {
            return calculateWeight(SDM_MONTHLY_PRICES.get(weddingMonth), SDM_BASE_PRICE);
        }
        throw new IllegalArgumentException("지원하지 않는 결혼 비용 카테고리입니다.");
    }

    private BigDecimal calculateWeight(Long monthlyPrice, Long basePrice) {
        if (monthlyPrice == null) {
            throw new IllegalArgumentException("월별 결혼 비용 가중치가 존재하지 않습니다.");
        }
        return BigDecimal.valueOf(monthlyPrice)
                .divide(BigDecimal.valueOf(basePrice), WEIGHT_SCALE, WEIGHT_ROUNDING_MODE);
    }
}

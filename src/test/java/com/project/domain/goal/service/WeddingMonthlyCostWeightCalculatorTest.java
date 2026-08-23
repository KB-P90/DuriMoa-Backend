package com.project.domain.goal.service;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WeddingMonthlyCostWeightCalculatorTest {

    private final WeddingMonthlyCostWeightCalculator calculator =
            new WeddingMonthlyCostWeightCalculator();

    @Test
    void adjustsAllWeddingMonthsWithSeparateVenueAndSdmPrices() {
        List<Long> venuePrices = List.of(
                11_900_000L, 12_480_000L, 16_500_000L, 17_250_000L,
                17_400_000L, 16_400_000L, 12_720_000L, 12_200_000L,
                10_700_000L, 14_200_000L, 12_300_000L, 13_600_000L
        );
        List<Long> sdmPrices = List.of(
                2_800_000L, 2_760_000L, 2_800_000L, 2_890_000L,
                2_890_000L, 2_900_000L, 2_700_000L, 2_700_000L,
                2_800_000L, 3_000_000L, 2_980_000L, 2_800_000L
        );

        for (Month month : Month.values()) {
            Integer monthIndex = month.getValue() - 1;
            Assertions.assertEquals(
                    venuePrices.get(monthIndex),
                    calculator.adjustAmount("예식장", month, 16_400_000L),
                    month.name()
            );
            Assertions.assertEquals(
                    sdmPrices.get(monthIndex),
                    calculator.adjustAmount("메이크업", month, 2_900_000L),
                    month.name()
            );
        }
    }

    @Test
    void octoberUsesSeparateVenueAndSdmWeights() {
        Assertions.assertEquals(
                new BigDecimal("0.8658536585"),
                calculator.resolveWeight("예식장", Month.OCTOBER)
        );
        Assertions.assertEquals(
                new BigDecimal("1.0344827586"),
                calculator.resolveWeight("스튜디오", Month.OCTOBER)
        );
        Assertions.assertEquals(
                12_990_000L,
                calculator.adjustAmount("예식장", Month.OCTOBER, 15_000_000L)
        );
        Assertions.assertEquals(
                2_590_000L,
                calculator.adjustAmount("드레스", Month.OCTOBER, 2_500_000L)
        );
    }
}

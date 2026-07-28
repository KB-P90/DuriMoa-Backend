package com.project.domain.strategy.card.model;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CardBenefitCatalogTest {

    // 혜택 JSON의 snake_case 필드를 계산 규칙으로 정규화합니다.
    @Test
    void parseNormalizesSnakeCaseBenefitJson() {
        String benefitJson = "{\"benefits\":[{\"benefit_name\":\"웨딩\",\"benefit_rate\":1.2,"
                + "\"monthly_limit_amount\":5000,\"minimum_monthly_spend\":300000}]}";

        List<CardBenefitRule> rules = CardBenefitCatalog.parse(benefitJson, "전월 30만원 이상");

        Assertions.assertEquals(1, rules.size());
        Assertions.assertEquals("웨딩", rules.get(0).getTitle());
        Assertions.assertEquals(new BigDecimal("0.012000"), rules.get(0).getBenefitRate());
        Assertions.assertEquals(5_000L, rules.get(0).getMonthlyLimitAmount());
        Assertions.assertEquals(300_000L, rules.get(0).getMinimumPerformanceAmount());
        Assertions.assertTrue(rules.get(0).getBenefitThresholds().contains(300_000L));
    }

    // 텍스트 혜택 설명의 퍼센트 표기를 계산 가능한 혜택률로 추출합니다.
    @Test
    void parseExtractsPercentFromTextBenefit() {
        List<CardBenefitRule> rules = CardBenefitCatalog.parse("\"전 가맹점 0.8% 적립\"", "전월실적 없음");

        Assertions.assertEquals(1, rules.size());
        Assertions.assertEquals(new BigDecimal("0.008000"), rules.get(0).getBenefitRate());
    }
}

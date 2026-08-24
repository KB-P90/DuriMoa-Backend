package com.project.domain.strategy.card.model;

import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductRecord;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CardProductMatcherTest {

    // 괄호 문구와 부가어를 제거한 뒤 같은 카드 상품으로 인식합니다.
    @Test
    void calculateMatchScoreRemovesBracketTextAndSupplementalWords() {
        CardProductMatcher matcher = new CardProductMatcher();

        Integer score = matcher.calculateMatchScore(
                "KB국민카드",
                "KB국민 행복카드(RF 후불교통) 체크",
                "KB국민",
                "국민행복"
        );

        Assertions.assertTrue(score > 0);
    }

    // 정규화된 카드명 중 한쪽이 다른 쪽을 포함하면 후보로 인정합니다.
    @Test
    void calculateMatchScoreAllowsNameContainsRelation() {
        CardProductMatcher matcher = new CardProductMatcher();

        Integer score = matcher.calculateMatchScore(
                "롯데카드",
                "LOCA LIKIT 신용",
                "롯데",
                "LOCA LIKIT"
        );

        Assertions.assertTrue(score > 0);
    }

    // 카드사가 같은 후보 중 카드명 토큰이 더 많이 겹치는 상품을 선택합니다.
    @Test
    void findBestMatchingProductUsesTokenOverlapScore() {
        CardProductMatcher matcher = new CardProductMatcher();
        CardProductRecord deepDream = cardProduct(10L, "신한", "신한 Deep Dream");
        CardProductRecord mrLife = cardProduct(20L, "신한카드", "신한 Mr.Life");

        CardProductRecord matchedProduct = matcher.findBestMatchingProduct(
                "신한카드",
                "신한카드 Mr.Life(후불교통) RF 체크",
                Arrays.asList(deepDream, mrLife)
        );

        Assertions.assertEquals(20L, matchedProduct.getCardProductId());
    }

    // 카드사가 맞지 않으면 카드명이 비슷해도 매칭하지 않습니다.
    @Test
    void findBestMatchingProductRequiresCompanyMatch() {
        CardProductMatcher matcher = new CardProductMatcher();

        CardProductRecord matchedProduct = matcher.findBestMatchingProduct(
                "현대",
                "ZERO",
                Arrays.asList(cardProduct(10L, "신한", "ZERO"))
        );

        Assertions.assertNull(matchedProduct);
    }

    private CardProductRecord cardProduct(Long cardProductId, String company, String cardName) {
        CardProductRecord cardProduct = new CardProductRecord();
        cardProduct.setCardProductId(cardProductId);
        cardProduct.setCompany(company);
        cardProduct.setCardName(cardName);
        cardProduct.setConditionCheckedAt("2026-07-01 00:00:00");
        return cardProduct;
    }
}

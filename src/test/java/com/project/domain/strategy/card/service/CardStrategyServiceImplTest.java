package com.project.domain.strategy.card.service;

import com.project.common.exception.CustomException;
import com.project.domain.strategy.card.dto.CardDetailResponseDTO;
import com.project.domain.strategy.card.dto.CardRecommendationResponseDTO;
import com.project.domain.strategy.card.mapper.CardStrategyMapper;
import com.project.domain.strategy.card.model.CardBenefitCalculator;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardOwnerRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardPerformanceRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.UserCardRecord;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardStrategyServiceImplTest {

    // 결제 예정 금액 기준으로 소유자별 카드 순위와 전체 최적 카드를 계산합니다.
    @Test
    void recommendCardsBuildsOwnersAndBestCard() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );

        Mockito.when(mapper.selectActiveCoupleMembers(1L))
                .thenReturn(Arrays.asList(owner(1L, "김민준", "G"), owner(2L, "이서연", "B")));
        Mockito.when(mapper.selectUserCardsByUserIds(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(
                        userCard(10L, 1L, "김민준", "KB국민", "KB 웨딩", "0.01", 3_000L, 0L),
                        userCard(11L, 1L, "김민준", "신한", "신한 Mr.Life", "0.005", null, 0L),
                        userCard(20L, 2L, "이서연", "현대", "현대 ZERO", "0.02", 10_000L, 0L)
                ));
        Mockito.when(mapper.selectCurrentMonthPerformances(
                        Arrays.asList(10L, 11L, 20L),
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 28)
                ))
                .thenReturn(Arrays.asList(
                        performance(10L, 250_000L),
                        performance(11L, 0L),
                        performance(20L, 100_000L)
                ));

        CardRecommendationResponseDTO response = service.recommendCards(1L, 300_000L);

        Assertions.assertEquals(300_000L, response.getPaymentAmount());
        Assertions.assertEquals(2, response.getOwners().size());
        Assertions.assertEquals("PARTNER", response.getBestCard().getOwnerType());
        Assertions.assertEquals(20L, response.getBestCard().getCardId());
        Assertions.assertEquals(6_000L, response.getBestCard().getExpectedBenefitAmount());
        Assertions.assertEquals(2, response.getOwners().get(0).getCardCount());
        Assertions.assertEquals(1, response.getOwners().get(0).getCards().get(0).getRank());
    }

    // 오래된 상품은 계산 가능한 혜택이 커도 전체 최적 카드에서 제외합니다.
    @Test
    void recommendCardsExcludesStaleProductFromBestCard() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );

        UserCardRecord freshCard = userCard(10L, 1L, "김민준", "KB국민", "KB 웨딩", "0.01", null, 0L);
        UserCardRecord staleCard = userCard(11L, 1L, "김민준", "신한", "신한 고혜택", "0.10", null, 0L);
        staleCard.setConditionCheckedAt("2025-12-01 00:00:00");

        Mockito.when(mapper.selectActiveCoupleMembers(1L))
                .thenReturn(Collections.singletonList(owner(1L, "김민준", "G")));
        Mockito.when(mapper.selectUserCardsByUserIds(Collections.singletonList(1L)))
                .thenReturn(Arrays.asList(staleCard, freshCard));
        Mockito.when(mapper.selectCurrentMonthPerformances(
                        Arrays.asList(11L, 10L),
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 28)
                ))
                .thenReturn(Arrays.asList(performance(11L, 0L), performance(10L, 0L)));

        CardRecommendationResponseDTO response = service.recommendCards(1L, 100_000L);

        Assertions.assertEquals(10L, response.getBestCard().getCardId());
        Assertions.assertEquals("NEEDS_REVIEW", response.getOwners().get(0).getCards().get(1).getRecommendableStatus());
    }

    // 결제 사용처가 없을 때는 특정 카테고리 고혜택보다 전 가맹점·기본 혜택을 우선 추천합니다.
    @Test
    void recommendCardsUsesOnlyAmountOnlyBenefitsWhenCategoryIsUnknown() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );

        UserCardRecord cafeCard = userCardWithBenefit(
                10L,
                1L,
                "김민준",
                "신한",
                "신한 카페특화",
                "{\"benefits\":[{\"benefit_name\":\"카페 할인\",\"categories\":[\"CAFE\"],"
                        + "\"benefit_rate\":0.2,\"monthly_limit_amount\":20000,"
                        + "\"minimum_monthly_spend\":0}]}"
        );
        UserCardRecord defaultCard = userCardWithBenefit(
                11L,
                1L,
                "김민준",
                "KB국민",
                "KB 기본",
                "{\"benefits\":[{\"benefit_name\":\"전 가맹점 적립\",\"categories\":[\"ALL\"],"
                        + "\"benefit_rate\":0.01,\"minimum_monthly_spend\":0}]}"
        );

        Mockito.when(mapper.selectActiveCoupleMembers(1L))
                .thenReturn(Collections.singletonList(owner(1L, "김민준", "G")));
        Mockito.when(mapper.selectUserCardsByUserIds(Collections.singletonList(1L)))
                .thenReturn(Arrays.asList(cafeCard, defaultCard));
        Mockito.when(mapper.selectCurrentMonthPerformances(
                        Arrays.asList(10L, 11L),
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 28)
                ))
                .thenReturn(Arrays.asList(performance(10L, 0L), performance(11L, 0L)));

        CardRecommendationResponseDTO response = service.recommendCards(1L, 100_000L);

        Assertions.assertEquals(11L, response.getBestCard().getCardId());
        Assertions.assertEquals(1_000L, response.getBestCard().getExpectedBenefitAmount());
        Assertions.assertEquals(11L, response.getOwners().get(0).getCards().get(0).getCardId());
        Assertions.assertEquals(10L, response.getOwners().get(0).getCards().get(1).getCardId());
        Assertions.assertEquals(0L, response.getOwners().get(0).getCards().get(1).getExpectedBenefitAmount());
        Assertions.assertFalse(response.getOwners().get(0).getCards().get(1).getRecommendable());
        Assertions.assertEquals("CATEGORY_REQUIRED", response.getOwners().get(0).getCards().get(1).getRecommendableStatus());
    }

    // 카테고리 입력 시 보유 카드는 카테고리 기준으로 정렬하고 상단은 카드 상품 중 혜택률이 가장 높은 카드를 추천합니다.
    @Test
    void recommendCardsUsesCategoryForOwnedCardsAndProductBestCard() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );

        UserCardRecord selfFurnitureCard = userCardWithBenefit(
                10L,
                1L,
                "김민준",
                "KB국민",
                "KB 가구",
                "{\"benefits\":[{\"benefit_name\":\"가구 할인\",\"categories\":[\"FURNITURE\"],"
                        + "\"benefit_rate\":0.05,\"monthly_limit_amount\":100000,"
                        + "\"minimum_monthly_spend\":0}]}"
        );
        UserCardRecord selfTravelCard = userCardWithBenefit(
                11L,
                1L,
                "김민준",
                "신한",
                "신한 여행",
                "{\"benefits\":[{\"benefit_name\":\"여행 할인\",\"categories\":[\"TRAVEL\"],"
                        + "\"benefit_rate\":0.2,\"minimum_monthly_spend\":0}]}"
        );
        UserCardRecord partnerMartCard = userCardWithBenefit(
                20L,
                2L,
                "이서연",
                "현대",
                "현대 마트",
                "{\"benefits\":[{\"benefit_name\":\"홈플러스 할인\",\"categories\":[\"MART\"],"
                        + "\"benefit_rate\":0.08,\"minimum_monthly_spend\":0}]}"
        );

        Mockito.when(mapper.selectActiveCoupleMembers(1L))
                .thenReturn(Arrays.asList(owner(1L, "김민준", "G"), owner(2L, "이서연", "B")));
        Mockito.when(mapper.selectUserCardsByUserIds(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(selfFurnitureCard, selfTravelCard, partnerMartCard));
        Mockito.when(mapper.selectCurrentMonthPerformances(
                        Arrays.asList(10L, 11L, 20L),
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 28)
                ))
                .thenReturn(Arrays.asList(
                        performance(10L, 0L),
                        performance(11L, 0L),
                        performance(20L, 0L)
                ));
        Mockito.when(mapper.selectCardProducts())
                .thenReturn(Arrays.asList(
                        cardProduct(
                                100L,
                                "삼성",
                                "삼성 가구",
                                "{\"benefits\":[{\"benefit_name\":\"가구 할인\",\"categories\":[\"FURNITURE\"],"
                                        + "\"benefit_rate\":0.10,\"minimum_monthly_spend\":0}]}"
                        ),
                        cardProduct(
                                101L,
                                "롯데",
                                "롯데 여행",
                                "{\"benefits\":[{\"benefit_name\":\"여행 할인\",\"categories\":[\"TRAVEL\"],"
                                        + "\"benefit_rate\":0.20,\"minimum_monthly_spend\":0}]}"
                        ),
                        cardProduct(
                                102L,
                                "현대",
                                "현대 홈플러스",
                                "{\"benefits\":[{\"benefit_name\":\"홈플러스 할인\",\"categories\":[\"MART\"],"
                                        + "\"benefit_rate\":0.12,\"minimum_monthly_spend\":0}]}"
                        )
                ));

        CardRecommendationResponseDTO response = service.recommendCards(1L, 3_000_000L, "가구");

        Assertions.assertEquals("FURNITURE", response.getCategory());
        Assertions.assertEquals("가구", response.getCategoryName());
        Assertions.assertNull(response.getBestCard().getCardId());
        Assertions.assertEquals(102L, response.getBestCard().getCardProductId());
        Assertions.assertEquals(0, new BigDecimal("0.12").compareTo(response.getBestCard().getBenefitRate()));
        Assertions.assertEquals(10L, response.getOwners().get(0).getCards().get(0).getCardId());
        Assertions.assertEquals(100_000L, response.getOwners().get(0).getCards().get(0).getExpectedBenefitAmount());
        Assertions.assertEquals("CATEGORY_NOT_MATCHED", response.getOwners().get(0).getCards().get(1).getRecommendableStatus());
        Assertions.assertEquals(20L, response.getOwners().get(1).getCards().get(0).getCardId());
        Assertions.assertEquals(240_000L, response.getOwners().get(1).getCards().get(0).getExpectedBenefitAmount());
    }

    // 카드고릴라 수집 상품에 카테고리 추천 후보가 없으면 임시 카드 상품 후보로 상단 추천을 채웁니다.
    @Test
    void recommendCardsFallsBackToMockProductWhenCrawledBestCardIsMissing() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );

        UserCardRecord selfFurnitureCard = userCardWithBenefit(
                10L,
                1L,
                "김민준",
                "KB국민",
                "KB 가구",
                "{\"benefits\":[{\"benefit_name\":\"가구 할인\",\"categories\":[\"FURNITURE\"],"
                        + "\"benefit_rate\":0.05,\"minimum_monthly_spend\":0}]}"
        );

        Mockito.when(mapper.selectActiveCoupleMembers(1L))
                .thenReturn(Collections.singletonList(owner(1L, "김민준", "G")));
        Mockito.when(mapper.selectUserCardsByUserIds(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonList(selfFurnitureCard));
        Mockito.when(mapper.selectCurrentMonthPerformances(
                        Collections.singletonList(10L),
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 28)
                ))
                .thenReturn(Collections.singletonList(performance(10L, 0L)));
        Mockito.when(mapper.selectCardProducts()).thenReturn(Collections.emptyList());
        Mockito.when(mapper.selectFallbackCardProducts())
                .thenReturn(Collections.singletonList(cardProduct(
                        9003L,
                        "하나",
                        "하나 생활기본",
                        "{\"benefits\":[{\"benefit_name\":\"생활 영역 할인\",\"categories\":[\"MART\"],"
                                + "\"benefit_rate\":0.05,\"minimum_monthly_spend\":0}]}"
                )));

        CardRecommendationResponseDTO response = service.recommendCards(1L, 100_000L, "가구");

        Assertions.assertEquals(9003L, response.getBestCard().getCardProductId());
        Assertions.assertEquals("하나 생활기본", response.getBestCard().getCardName());
        Mockito.verify(mapper).selectFallbackCardProducts();
    }

    // card_product_id가 없는 보유 카드는 수집 상품 중 가장 유사한 상품의 혜택으로 추천을 계산합니다.
    @Test
    void recommendCardsMatchesUnlinkedUserCardToBestCardProduct() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );
        UserCardRecord rawCard = unlinkedUserCard(
                10L,
                1L,
                "김민준",
                "신한카드",
                "신한카드 Mr.Life(후불교통) RF 체크"
        );
        CardProductRecord deepDream = cardProduct(
                100L,
                "신한",
                "신한 Deep Dream",
                "{\"benefits\":[{\"benefit_name\":\"전 가맹점 적립\",\"categories\":[\"ALL\"],"
                        + "\"benefit_rate\":0.01,\"minimum_monthly_spend\":0}]}"
        );
        CardProductRecord mrLife = cardProduct(
                101L,
                "신한카드",
                "신한 Mr.Life",
                "{\"benefits\":[{\"benefit_name\":\"생활 할인\",\"categories\":[\"ALL\"],"
                        + "\"benefit_rate\":0.05,\"minimum_monthly_spend\":0}]}"
        );

        Mockito.when(mapper.selectActiveCoupleMembers(1L))
                .thenReturn(Collections.singletonList(owner(1L, "김민준", "G")));
        Mockito.when(mapper.selectUserCardsByUserIds(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonList(rawCard));
        Mockito.when(mapper.selectAllCardProductsForMatching()).thenReturn(Arrays.asList(deepDream, mrLife));
        Mockito.when(mapper.selectCurrentMonthPerformances(
                        Collections.singletonList(10L),
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 28)
                ))
                .thenReturn(Collections.singletonList(performance(10L, 0L)));

        CardRecommendationResponseDTO response = service.recommendCards(1L, 100_000L);

        Assertions.assertEquals(101L, response.getBestCard().getCardProductId());
        Assertions.assertEquals(5_000L, response.getBestCard().getExpectedBenefitAmount());
        Assertions.assertEquals(101L, response.getOwners().get(0).getCards().get(0).getCardProductId());
        Mockito.verify(mapper).selectAllCardProductsForMatching();
    }

    // 활성 커플 권한이 있는 사용자 카드만 상세 조회를 허용합니다.
    @Test
    void getCardDetailRejectsUnauthorizedCardId() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );

        Mockito.when(mapper.existsCardAccessInActiveCouple(1L, 10L)).thenReturn(0);

        Assertions.assertThrows(CustomException.class, () -> service.getCardDetail(1L, 10L));
    }

    // 카드 상세 응답에 상품 기본 정보와 혜택 목록을 포함합니다.
    @Test
    void getCardDetailReturnsProductBenefits() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );

        Mockito.when(mapper.selectCardDetailByCardId(10L)).thenReturn(cardDetail(10L));
        Mockito.when(mapper.existsCardAccessInActiveCouple(1L, 10L)).thenReturn(1);

        CardDetailResponseDTO response = service.getCardDetail(1L, 10L);

        Assertions.assertEquals("KB국민", response.getCompany());
        Assertions.assertEquals("UNKNOWN", response.getSaleStatus());
        Assertions.assertEquals(1, response.getBenefits().size());
        Assertions.assertTrue(response.getBenefits().get(0).getAmountOnlyApplicable());
        Assertions.assertEquals("RECOMMENDABLE", response.getRecommendableStatus());
    }

    // 추천 카드 상품 식별자로 카드 상품 상세 응답을 조회합니다.
    @Test
    void getCardProductDetailReturnsProductBenefits() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );
        Mockito.when(mapper.selectCardProductDetailByProductId(100L))
                .thenReturn(cardProduct(
                        100L,
                        "현대",
                        "현대 홈플러스",
                        "{\"benefits\":[{\"benefit_name\":\"홈플러스 할인\",\"categories\":[\"MART\"],"
                                + "\"benefit_rate\":0.12,\"monthly_limit_amount\":10000,"
                                + "\"minimum_monthly_spend\":0}]}"
                ));

        CardDetailResponseDTO response = service.getCardProductDetail(1L, 100L);

        Assertions.assertNull(response.getCardId());
        Assertions.assertEquals(100L, response.getCardProductId());
        Assertions.assertEquals("현대 홈플러스", response.getCardName());
        Assertions.assertEquals(1, response.getBenefits().size());
        Assertions.assertEquals("홈플러스 할인", response.getBenefits().get(0).getTitle());
    }

    // 테스트에서 현재 날짜를 고정하기 위한 시계를 제공합니다.
    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-28T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    }

    // 활성 커플 구성원 테스트 데이터를 만듭니다.
    private CardOwnerRecord owner(Long userId, String ownerName, String ownerRole) {
        CardOwnerRecord owner = new CardOwnerRecord();
        owner.setCoupleId(100L);
        owner.setUserId(userId);
        owner.setOwnerName(ownerName);
        owner.setOwnerRole(ownerRole);
        return owner;
    }

    // 사용자 보유 카드 테스트 데이터를 만듭니다.
    private UserCardRecord userCard(
            Long cardId,
            Long userId,
            String ownerName,
            String company,
            String cardName,
            String benefitRate,
            Long monthlyLimit,
            Long minimumSpend
    ) {
        UserCardRecord card = new UserCardRecord();
        card.setCardId(cardId);
        card.setUserId(userId);
        card.setOwnerName(ownerName);
        card.setOwnerRole(userId.equals(1L) ? "G" : "B");
        card.setCardProductId(cardId + 100L);
        card.setCompany(company);
        card.setCardName(cardName);
        card.setAnnualFee(10_000);
        card.setImage("/cards/test.png");
        card.setBenefit("{\"benefits\":[{\"benefit_name\":\"기본\",\"benefit_rate\":" + benefitRate
                + ",\"monthly_limit_amount\":" + monthlyLimit
                + ",\"minimum_monthly_spend\":" + minimumSpend
                + ",\"benefit_thresholds\":[300000,600000]}]}");
        card.setConditionText("전월 30만원 이상");
        card.setConditionCheckedAt("2026-07-01 00:00:00");
        return card;
    }

    // 혜택 JSON을 직접 지정하는 사용자 보유 카드 테스트 데이터를 만듭니다.
    private UserCardRecord userCardWithBenefit(
            Long cardId,
            Long userId,
            String ownerName,
            String company,
            String cardName,
            String benefitJson
    ) {
        UserCardRecord card = new UserCardRecord();
        card.setCardId(cardId);
        card.setUserId(userId);
        card.setOwnerName(ownerName);
        card.setOwnerRole(userId.equals(1L) ? "G" : "B");
        card.setCardProductId(cardId + 100L);
        card.setCompany(company);
        card.setCardName(cardName);
        card.setAnnualFee(10_000);
        card.setImage("/cards/test.png");
        card.setBenefit(benefitJson);
        card.setConditionText("전월 30만원 이상");
        card.setConditionCheckedAt("2026-07-01 00:00:00");
        return card;
    }

    // 아직 카드 상품과 연결되지 않은 사용자 보유 카드 테스트 데이터를 만듭니다.
    private UserCardRecord unlinkedUserCard(
            Long cardId,
            Long userId,
            String ownerName,
            String company,
            String cardName
    ) {
        UserCardRecord card = new UserCardRecord();
        card.setCardId(cardId);
        card.setUserId(userId);
        card.setOwnerName(ownerName);
        card.setOwnerRole(userId.equals(1L) ? "G" : "B");
        card.setCompany(company);
        card.setCardName(cardName);
        card.setAnnualFee(0);
        return card;
    }

    // 카드 인정 실적 테스트 데이터를 만듭니다.
    private CardPerformanceRecord performance(Long cardId, Long amount) {
        CardPerformanceRecord performance = new CardPerformanceRecord();
        performance.setCardId(cardId);
        performance.setRecognizedPerformanceAmount(amount);
        return performance;
    }

    // 카드 상세 테스트 데이터를 만듭니다.
    private CardProductRecord cardDetail(Long cardId) {
        CardProductRecord cardDetail = new CardProductRecord();
        cardDetail.setCardId(cardId);
        cardDetail.setCardProductId(100L);
        cardDetail.setCompany("KB국민");
        cardDetail.setCardName("KB 웨딩");
        cardDetail.setAnnualFee(20_000);
        cardDetail.setImage("/cards/kb.png");
        cardDetail.setBenefit("{\"benefits\":[{\"benefit_name\":\"전 가맹점 적립\",\"categories\":[\"ALL\"],"
                + "\"benefit_rate\":0.02,\"monthly_limit_amount\":10000,"
                + "\"minimum_monthly_spend\":300000}]}");
        cardDetail.setConditionText("전월 30만원 이상");
        cardDetail.setConditionCheckedAt("2026-07-01 00:00:00");
        return cardDetail;
    }

    // 카드 상품 추천 테스트 데이터를 만듭니다.
    private CardProductRecord cardProduct(Long cardProductId, String company, String cardName, String benefitJson) {
        CardProductRecord cardProduct = new CardProductRecord();
        cardProduct.setCardProductId(cardProductId);
        cardProduct.setCompany(company);
        cardProduct.setCardName(cardName);
        cardProduct.setAnnualFee(20_000);
        cardProduct.setImage("/cards/product.png");
        cardProduct.setBenefit(benefitJson);
        cardProduct.setConditionText("전월 30만원 이상");
        cardProduct.setConditionCheckedAt("2026-07-01 00:00:00");
        return cardProduct;
    }
}

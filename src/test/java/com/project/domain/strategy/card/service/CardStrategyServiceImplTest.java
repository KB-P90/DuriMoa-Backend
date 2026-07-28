package com.project.domain.strategy.card.service;

import com.project.domain.strategy.card.dto.CardDetailResponseDTO;
import com.project.domain.strategy.card.dto.CardRecommendationResponseDTO;
import com.project.domain.strategy.card.exception.CardAccessDeniedException;
import com.project.domain.strategy.card.mapper.CardStrategyMapper;
import com.project.domain.strategy.card.model.CardBenefitCalculator;
import com.project.domain.strategy.card.model.CardOwnerRecord;
import com.project.domain.strategy.card.model.CardPerformanceRecord;
import com.project.domain.strategy.card.model.CardProductRecord;
import com.project.domain.strategy.card.model.UserCardRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        Assertions.assertEquals(20L, response.getBestCard().getUserCardKey());
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
        staleCard.setConditionCheckedAt(LocalDateTime.of(2025, 12, 1, 0, 0));

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

        Assertions.assertEquals(10L, response.getBestCard().getUserCardKey());
        Assertions.assertEquals("NEEDS_REVIEW", response.getOwners().get(0).getCards().get(1).getRecommendableStatus());
    }

    // 활성 커플 권한이 있는 사용자 카드만 상세 조회를 허용합니다.
    @Test
    void getCardDetailRejectsUnauthorizedUserCardKey() {
        CardStrategyMapper mapper = Mockito.mock(CardStrategyMapper.class);
        CardStrategyServiceImpl service = new CardStrategyServiceImpl(
                mapper,
                new CardBenefitCalculator(),
                fixedClock()
        );

        Mockito.when(mapper.existsCardAccessInActiveCouple(1L, 10L)).thenReturn(0);

        Assertions.assertThrows(CardAccessDeniedException.class, () -> service.getCardDetail(1L, 10L));
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

        Mockito.when(mapper.selectCardDetailByUserCardKey(10L)).thenReturn(cardDetail(10L));
        Mockito.when(mapper.existsCardAccessInActiveCouple(1L, 10L)).thenReturn(1);

        CardDetailResponseDTO response = service.getCardDetail(1L, 10L);

        Assertions.assertEquals("KB국민", response.getCompany());
        Assertions.assertEquals("UNKNOWN", response.getSaleStatus());
        Assertions.assertEquals(1, response.getBenefits().size());
        Assertions.assertEquals("RECOMMENDABLE", response.getRecommendableStatus());
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
            Long userCardKey,
            Long userId,
            String ownerName,
            String company,
            String cardName,
            String benefitRate,
            Long monthlyLimit,
            Long minimumSpend
    ) {
        UserCardRecord card = new UserCardRecord();
        card.setUserCardKey(userCardKey);
        card.setUserId(userId);
        card.setOwnerName(ownerName);
        card.setOwnerRole(userId.equals(1L) ? "G" : "B");
        card.setCardProductId(userCardKey + 100L);
        card.setCompany(company);
        card.setCardName(cardName);
        card.setAnnualFee(10_000);
        card.setImage("/cards/test.png");
        card.setBenefit("{\"benefits\":[{\"benefit_name\":\"기본\",\"benefit_rate\":" + benefitRate
                + ",\"monthly_limit_amount\":" + monthlyLimit
                + ",\"minimum_monthly_spend\":" + minimumSpend
                + ",\"benefit_thresholds\":[300000,600000]}]}");
        card.setConditionText("전월 30만원 이상");
        card.setConditionCheckedAt(LocalDateTime.of(2026, 7, 1, 0, 0));
        return card;
    }

    // 카드 인정 실적 테스트 데이터를 만듭니다.
    private CardPerformanceRecord performance(Long userCardKey, Long amount) {
        CardPerformanceRecord performance = new CardPerformanceRecord();
        performance.setUserCardKey(userCardKey);
        performance.setRecognizedPerformanceAmount(amount);
        return performance;
    }

    // 카드 상세 테스트 데이터를 만듭니다.
    private CardProductRecord cardDetail(Long userCardKey) {
        CardProductRecord cardDetail = new CardProductRecord();
        cardDetail.setUserCardKey(userCardKey);
        cardDetail.setCardProductId(100L);
        cardDetail.setCompany("KB국민");
        cardDetail.setCardName("KB 웨딩");
        cardDetail.setAnnualFee(20_000);
        cardDetail.setImage("/cards/kb.png");
        cardDetail.setBenefit("{\"benefits\":[{\"benefit_name\":\"웨딩\",\"benefit_rate\":0.02,"
                + "\"monthly_limit_amount\":10000,\"minimum_monthly_spend\":300000}]}");
        cardDetail.setConditionText("전월 30만원 이상");
        cardDetail.setConditionCheckedAt(LocalDateTime.of(2026, 7, 1, 0, 0));
        return cardDetail;
    }
}

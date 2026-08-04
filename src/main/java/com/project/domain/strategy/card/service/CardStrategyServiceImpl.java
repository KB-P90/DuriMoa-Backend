package com.project.domain.strategy.card.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.strategy.card.dto.BestCardResponseDTO;
import com.project.domain.strategy.card.dto.CardBenefitResponseDTO;
import com.project.domain.strategy.card.dto.CardDetailResponseDTO;
import com.project.domain.strategy.card.dto.CardRecommendationResponseDTO;
import com.project.domain.strategy.card.dto.CardRecommendationCardResponseDTO;
import com.project.domain.strategy.card.dto.CardRecommendationOwnerResponseDTO;
import com.project.domain.strategy.card.mapper.CardStrategyMapper;
import com.project.domain.strategy.card.model.CardBenefitCalculation;
import com.project.domain.strategy.card.model.CardBenefitCalculator;
import com.project.domain.strategy.card.model.CardBenefitCatalog;
import com.project.domain.strategy.card.model.CardBenefitRule;
import com.project.domain.strategy.card.model.CardProductRecommendStatus;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardOwnerRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardPerformanceRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.UserCardRecord;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardStrategyServiceImpl implements CardStrategyService {

    // 예상 혜택 계산이 확정 청구 할인은 아니라는 공통 안내 문구입니다.
    private static final String BENEFIT_NOTICE = "예상 혜택은 카드사 승인, 제외 업종, 취소·환불 반영 시 달라질 수 있습니다.";

    // 현재 스키마에 판매 상태 컬럼이 없어 상세 응답에 사용하는 기본 상태입니다.
    private static final String UNKNOWN_SALE_STATUS = "UNKNOWN";

    // 응답의 조건 확인 일시를 고정된 포맷으로 내려주기 위한 포매터입니다.
    private static final DateTimeFormatter CONDITION_CHECKED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 카드 추천 전용 DB 접근 Mapper입니다.
    private final CardStrategyMapper cardStrategyMapper;

    // 카드 혜택 계산을 담당하는 순수 계산 객체입니다.
    private final CardBenefitCalculator cardBenefitCalculator;

    // 월 실적 기준 기간과 상품 확인 기준일 판단에 사용하는 시계입니다.
    private final Clock clock;

    // 운영 환경에서 사용하는 서비스 생성자입니다.
    @Autowired
    public CardStrategyServiceImpl(CardStrategyMapper cardStrategyMapper) {
        this(cardStrategyMapper, new CardBenefitCalculator(), Clock.systemDefaultZone());
    }

    // 테스트에서 기준일과 계산 객체를 고정하기 위한 생성자입니다.
    CardStrategyServiceImpl(
            CardStrategyMapper cardStrategyMapper,
            CardBenefitCalculator cardBenefitCalculator,
            Clock clock
    ) {
        this.cardStrategyMapper = cardStrategyMapper;
        this.cardBenefitCalculator = cardBenefitCalculator;
        this.clock = clock;
    }

    // 결제 예정 금액 기준으로 활성 커플의 카드 추천 결과를 조회합니다.
    @Override
    @Transactional(readOnly = true)
    public CardRecommendationResponseDTO recommendCards(Long authenticatedUserId, Long paymentAmount) {
        validateAuthenticatedUser(authenticatedUserId);
        List<CardOwnerRecord> owners = findActiveCoupleMembers(authenticatedUserId);
        // 활성 커플 구성원의 카드 조회에 사용할 사용자 ID 목록입니다.
        List<Long> ownerIds = owners.stream()
                .map(CardOwnerRecord::getUserId)
                .collect(Collectors.toList());
        // 추천 후보가 되는 본인·상대방 보유 카드 목록입니다.
        List<UserCardRecord> userCards = cardStrategyMapper.selectUserCardsByUserIds(ownerIds);
        if (userCards == null) {
            userCards = Collections.emptyList();
        }
        // 카드별 현재 월 인정 실적을 담은 조회 결과입니다.
        Map<Long, Long> performanceAmountByCardKey = findPerformanceAmountByCardKey(userCards);
        // 상품 조건 확인일 상태 판단에 사용하는 오늘 날짜입니다.
        LocalDate today = LocalDate.now(clock);

        // 정렬 기준을 적용한 전체 카드 추천 후보 목록입니다.
        List<CardRecommendationCandidate> candidates = userCards.stream()
                .map(card -> buildCandidate(authenticatedUserId, paymentAmount, performanceAmountByCardKey, today, card))
                .sorted(recommendationComparator())
                .collect(Collectors.toList());

        // 화면에서 본인·상대방을 구분해 표시할 소유자별 응답 목록입니다.
        List<CardRecommendationOwnerResponseDTO> ownerResponses = owners.stream()
                .map(owner -> buildOwnerResponse(authenticatedUserId, owner, candidates))
                .collect(Collectors.toList());
        // 추천 가능 카드 중 예상 혜택이 가장 큰 전체 최적 카드입니다.
        BestCardResponseDTO bestCard = candidates.stream()
                .filter(CardRecommendationCandidate::getRecommendable)
                .findFirst()
                .map(this::buildBestCardResponse)
                .orElse(null);

        return CardRecommendationResponseDTO.builder()
                .paymentAmount(paymentAmount)
                .bestCard(bestCard)
                .owners(ownerResponses)
                .benefitNotice(BENEFIT_NOTICE)
                .build();
    }

    // 사용자 카드 식별자로 권한 검증 후 카드 상세 정보를 조회합니다.
    @Override
    @Transactional(readOnly = true)
    public CardDetailResponseDTO getCardDetail(Long authenticatedUserId, Long cardId) {
        validateAuthenticatedUser(authenticatedUserId);
        if (cardId == null || cardId <= 0) {
            throw resourceNotFound("카드 정보를 찾을 수 없습니다.");
        }

        Integer accessCount = cardStrategyMapper.existsCardAccessInActiveCouple(authenticatedUserId, cardId);
        if (accessCount == null || accessCount <= 0) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        CardProductRecord cardDetail = cardStrategyMapper.selectCardDetailByCardId(cardId);
        if (cardDetail == null) {
            throw resourceNotFound("카드 정보를 찾을 수 없습니다.");
        }

        // 카드 상세 화면에 표시할 정규화된 혜택 규칙 목록입니다.
        List<CardBenefitRule> benefitRules = CardBenefitCatalog.parse(
                cardDetail.getBenefit(),
                cardDetail.getConditionText()
        );
        // 상세 응답에서 보여줄 카드 상품 추천 가능 상태입니다.
        // 판매·추천 가능 상태 판단에 사용하는 상품 조건 확인 시점입니다.
        LocalDateTime conditionCheckedAt = parseConditionCheckedAt(cardDetail.getConditionCheckedAt());
        CardProductRecommendStatus recommendStatus = CardProductRecommendStatus.decide(
                LocalDate.now(clock),
                conditionCheckedAt,
                CardBenefitCatalog.hasCalculableBenefit(benefitRules)
        );

        return CardDetailResponseDTO.builder()
                .cardId(cardDetail.getCardId())
                .cardProductId(cardDetail.getCardProductId())
                .company(cardDetail.getCompany())
                .cardName(cardDetail.getCardName())
                .image(cardDetail.getImage())
                .annualFee(cardDetail.getAnnualFee())
                .benefits(toBenefitResponses(benefitRules))
                .condition(cardDetail.getConditionText())
                .conditionCheckedAt(formatDateTime(conditionCheckedAt))
                .officialUrl(null)
                .saleStatus(UNKNOWN_SALE_STATUS)
                .recommendableStatus(recommendStatus.getCode())
                .benefitNotice(BENEFIT_NOTICE)
                .build();
    }

    // 인증 사용자 ID가 유효한지 확인합니다.
    private void validateAuthenticatedUser(Long authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 인증 사용자가 속한 활성 커플 구성원을 조회합니다.
    private List<CardOwnerRecord> findActiveCoupleMembers(Long authenticatedUserId) {
        List<CardOwnerRecord> owners = cardStrategyMapper.selectActiveCoupleMembers(authenticatedUserId);
        if (owners == null || owners.isEmpty()) {
            throw resourceNotFound("활성 커플 정보를 찾을 수 없습니다.");
        }
        return owners;
    }

    private CustomException resourceNotFound(String message) {
        return new CustomException(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    // 카드별 현재 월 인정 실적을 조회해 카드 식별자 기준 Map으로 변환합니다.
    private Map<Long, Long> findPerformanceAmountByCardKey(List<UserCardRecord> userCards) {
        if (userCards == null || userCards.isEmpty()) {
            return Collections.emptyMap();
        }

        // 현재 월 실적 조회에 사용할 사용자 카드 식별자 목록입니다.
        List<Long> cardIds = userCards.stream()
                .map(UserCardRecord::getCardId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (cardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 실적 산정 기간의 마지막 날로 사용하는 오늘 날짜입니다.
        LocalDate today = LocalDate.now(clock);
        List<CardPerformanceRecord> performanceRecords = cardStrategyMapper.selectCurrentMonthPerformances(
                cardIds,
                today.withDayOfMonth(1),
                today
        );
        if (performanceRecords == null) {
            performanceRecords = Collections.emptyList();
        }
        // 카드 식별자별 현재 월 인정 실적입니다.
        Map<Long, Long> performanceAmountByCardKey = new HashMap<>();
        for (CardPerformanceRecord performanceRecord : performanceRecords) {
            performanceAmountByCardKey.put(
                    performanceRecord.getCardId(),
                    Math.max(nullToZero(performanceRecord.getRecognizedPerformanceAmount()), 0L)
            );
        }
        return performanceAmountByCardKey;
    }

    // 보유 카드와 실적 정보를 추천 후보로 변환합니다.
    private CardRecommendationCandidate buildCandidate(
            Long authenticatedUserId,
            Long paymentAmount,
            Map<Long, Long> performanceAmountByCardKey,
            LocalDate today,
            UserCardRecord card
    ) {
        // 카드 상품 혜택 JSON과 조건 문구에서 추출한 계산 규칙입니다.
        List<CardBenefitRule> benefitRules = CardBenefitCatalog.parse(card.getBenefit(), card.getConditionText());
        // 취소·환불 차감 후 카드별 현재 월 인정 실적입니다.
        Long currentPerformanceAmount = performanceAmountByCardKey.getOrDefault(card.getCardId(), 0L);
        // 결제 예정 금액 기준 예상 혜택과 다음 구간 계산 결과입니다.
        CardBenefitCalculation calculation = cardBenefitCalculator.calculate(
                paymentAmount,
                currentPerformanceAmount,
                benefitRules
        );
        // 상품 확인일과 혜택 데이터 기준 추천 가능 상태입니다.
        // 판매·추천 가능 상태 판단에 사용하는 상품 조건 확인 시점입니다.
        LocalDateTime conditionCheckedAt = parseConditionCheckedAt(card.getConditionCheckedAt());
        CardProductRecommendStatus recommendStatus = CardProductRecommendStatus.decide(
                today,
                conditionCheckedAt,
                CardBenefitCatalog.hasCalculableBenefit(benefitRules)
        );
        // 추천 대상에서 제외된 상품은 순위 계산 혜택을 0원으로 처리합니다.
        Long expectedBenefitAmount = Boolean.TRUE.equals(recommendStatus.isRecommendable())
                ? calculation.getExpectedBenefitAmount()
                : 0L;

        return CardRecommendationCandidate.builder()
                .rank(0)
                .cardId(card.getCardId())
                .cardProductId(card.getCardProductId())
                .userId(card.getUserId())
                .ownerType(card.getUserId().equals(authenticatedUserId) ? "SELF" : "PARTNER")
                .ownerName(card.getOwnerName())
                .ownerRole(card.getOwnerRole())
                .company(card.getCompany())
                .cardName(card.getCardName())
                .annualFee(card.getAnnualFee())
                .image(card.getImage())
                .expectedBenefitAmount(expectedBenefitAmount)
                .currentPerformanceAmount(calculation.getCurrentPerformanceAmount())
                .performanceAfterPayment(calculation.getPerformanceAfterPayment())
                .nextBenefitThreshold(calculation.getNextBenefitThreshold())
                .shortageAmount(calculation.getShortageAmount())
                .monthlyBenefitLimit(calculation.getMonthlyBenefitLimit())
                .benefitRate(calculation.getAppliedBenefitRate())
                .conditionSummary(card.getConditionText())
                .recommendable(recommendStatus.isRecommendable())
                .recommendableStatus(recommendStatus.getCode())
                .recommendationNotice(recommendStatus.isRecommendable() ? BENEFIT_NOTICE : recommendStatus.getNotice())
                .conditionCheckedAt(formatDateTime(conditionCheckedAt))
                .build();
    }

    // 소유자별 카드 목록과 순위를 구성합니다.
    private CardRecommendationOwnerResponseDTO buildOwnerResponse(
            Long authenticatedUserId,
            CardOwnerRecord owner,
            List<CardRecommendationCandidate> allCandidates
    ) {
        // 해당 소유자의 카드만 추려 정렬된 후보 목록을 만듭니다.
        List<CardRecommendationCandidate> ownerCandidates = allCandidates.stream()
                .filter(candidate -> candidate.getUserId().equals(owner.getUserId()))
                .collect(Collectors.toCollection(ArrayList::new));
        // 소유자 카드 목록 응답에 사용할 순위 반영 결과입니다.
        List<CardRecommendationCardResponseDTO> cards = new ArrayList<>();
        for (int index = 0; index < ownerCandidates.size(); index++) {
            CardRecommendationCandidate candidate = ownerCandidates.get(index).withRank(index + 1);
            cards.add(buildCardResponse(candidate));
        }

        return CardRecommendationOwnerResponseDTO.builder()
                .ownerType(owner.getUserId().equals(authenticatedUserId) ? "SELF" : "PARTNER")
                .userId(owner.getUserId())
                .ownerName(owner.getOwnerName())
                .ownerRole(owner.getOwnerRole())
                .cardCount(ownerCandidates.size())
                .cards(cards)
                .build();
    }

    // 후보 정보를 카드 목록 응답으로 변환합니다.
    private CardRecommendationCardResponseDTO buildCardResponse(CardRecommendationCandidate candidate) {
        return CardRecommendationCardResponseDTO.builder()
                .rank(candidate.getRank())
                .cardId(candidate.getCardId())
                .cardProductId(candidate.getCardProductId())
                .company(candidate.getCompany())
                .cardName(candidate.getCardName())
                .image(candidate.getImage())
                .annualFee(candidate.getAnnualFee())
                .expectedBenefitAmount(candidate.getExpectedBenefitAmount())
                .currentPerformanceAmount(candidate.getCurrentPerformanceAmount())
                .performanceAfterPayment(candidate.getPerformanceAfterPayment())
                .nextBenefitThreshold(candidate.getNextBenefitThreshold())
                .shortageAmount(candidate.getShortageAmount())
                .monthlyBenefitLimit(candidate.getMonthlyBenefitLimit())
                .benefitRate(candidate.getBenefitRate())
                .conditionSummary(candidate.getConditionSummary())
                .recommendable(candidate.getRecommendable())
                .recommendableStatus(candidate.getRecommendableStatus())
                .recommendationNotice(candidate.getRecommendationNotice())
                .conditionCheckedAt(candidate.getConditionCheckedAt())
                .build();
    }

    // 후보 정보를 전체 최적 카드 응답으로 변환합니다.
    private BestCardResponseDTO buildBestCardResponse(CardRecommendationCandidate candidate) {
        return BestCardResponseDTO.builder()
                .cardId(candidate.getCardId())
                .ownerType(candidate.getOwnerType())
                .ownerName(candidate.getOwnerName())
                .company(candidate.getCompany())
                .cardName(candidate.getCardName())
                .image(candidate.getImage())
                .expectedBenefitAmount(candidate.getExpectedBenefitAmount())
                .benefitRate(candidate.getBenefitRate())
                .recommendableStatus(candidate.getRecommendableStatus())
                .build();
    }

    // 혜택 규칙을 상세 조회용 혜택 응답으로 변환합니다.
    private List<CardBenefitResponseDTO> toBenefitResponses(List<CardBenefitRule> benefitRules) {
        if (benefitRules == null || benefitRules.isEmpty()) {
            return Collections.emptyList();
        }
        return benefitRules.stream()
                .map(rule -> CardBenefitResponseDTO.builder()
                        .title(rule.getTitle())
                        .description(rule.getDescription())
                        .benefitRate(rule.getBenefitRate())
                        .fixedBenefitAmount(rule.getFixedBenefitAmount())
                        .monthlyLimitAmount(rule.getMonthlyLimitAmount())
                        .minimumPerformanceAmount(rule.getMinimumPerformanceAmount())
                        .excludedKeywords(rule.safeExcludedKeywords())
                        .build())
                .collect(Collectors.toList());
    }

    // 추천 목록의 결정적인 정렬 기준을 제공합니다.
    private Comparator<CardRecommendationCandidate> recommendationComparator() {
        return Comparator
                .comparing(CardRecommendationCandidate::getRecommendable, Comparator.reverseOrder())
                .thenComparing(CardRecommendationCandidate::getExpectedBenefitAmount, Comparator.reverseOrder())
                .thenComparing(candidate -> nullToMax(candidate.getShortageAmount()))
                .thenComparing(candidate -> safeText(candidate.getCompany()))
                .thenComparing(candidate -> safeText(candidate.getCardName()))
                .thenComparing(CardRecommendationCandidate::getCardId);
    }

    // LocalDateTime 값을 API 포맷 문자열로 변환합니다.
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(CONDITION_CHECKED_AT_FORMATTER);
    }

    // DB에서 문자열로 받은 조건 확인 시점을 계산용 날짜로 변환합니다.
    private LocalDateTime parseConditionCheckedAt(String conditionCheckedAt) {
        if (conditionCheckedAt == null || conditionCheckedAt.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(conditionCheckedAt.trim(), CONDITION_CHECKED_AT_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    // 비어 있는 금액 값을 0으로 바꿉니다.
    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    // 정렬용 비어 있는 금액 값을 가장 뒤로 보냅니다.
    private Long nullToMax(Long value) {
        return value == null ? Long.MAX_VALUE : value;
    }

    // 정렬용 비어 있는 문자열 값을 빈 문자열로 바꿉니다.
    private String safeText(String value) {
        return value == null ? "" : value;
    }
}

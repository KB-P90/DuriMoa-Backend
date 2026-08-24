package com.project.domain.strategy.card.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public enum CardProductRecommendStatus {
    RECOMMENDABLE("RECOMMENDABLE", "추천 계산에 사용할 수 있는 상품입니다."),
    CATEGORY_REQUIRED("CATEGORY_REQUIRED", "사용처가 입력되지 않아 전 가맹점 또는 기본 혜택이 없는 상품은 추천 대상에서 제외했습니다."),
    CATEGORY_NOT_MATCHED("CATEGORY_NOT_MATCHED", "입력한 카테고리에 적용 가능한 계산 혜택이 없어 추천 대상에서 제외했습니다."),
    NEEDS_REVIEW("NEEDS_REVIEW", "상품 조건 확인일이 오래되어 추천 대상에서 제외했습니다."),
    NO_BENEFIT_DATA("NO_BENEFIT_DATA", "계산 가능한 혜택 데이터가 없어 추천 대상에서 제외했습니다.");

    // 카드 상품 조건 재확인이 필요하다고 판단하는 기준 일수입니다.
    private static final Long REVIEW_REQUIRED_AFTER_DAYS = 180L;

    // API에 전달하는 추천 가능 상태 코드입니다.
    private final String code;

    // 화면에서 안내 문구로 사용할 추천 제외 사유입니다.
    private final String notice;

    // 추천 가능 상태의 코드와 안내 문구를 보관합니다.
    CardProductRecommendStatus(String code, String notice) {
        this.code = code;
        this.notice = notice;
    }

    // 상품 상태와 혜택 데이터 유무로 추천 가능 상태를 판단합니다.
    public static CardProductRecommendStatus decide(LocalDate today, LocalDateTime productUpdatedAt, Boolean hasBenefitData) {
        if (!Boolean.TRUE.equals(hasBenefitData)) {
            return NO_BENEFIT_DATA;
        }
        if (productUpdatedAt == null) {
            return NEEDS_REVIEW;
        }

        LocalDate reviewLimitDate = today.minusDays(REVIEW_REQUIRED_AFTER_DAYS);
        if (productUpdatedAt.toLocalDate().isBefore(reviewLimitDate)) {
            return NEEDS_REVIEW;
        }
        return RECOMMENDABLE;
    }

    // 사용처 입력이 없는 카드 추천에서 확정 계산 가능한 혜택 여부까지 포함해 추천 가능 상태를 판단합니다.
    public static CardProductRecommendStatus decideForAmountOnly(
            LocalDate today,
            LocalDateTime productUpdatedAt,
            Boolean hasBenefitData,
            Boolean hasAmountOnlyBenefit
    ) {
        if (!Boolean.TRUE.equals(hasBenefitData)) {
            return NO_BENEFIT_DATA;
        }
        if (!Boolean.TRUE.equals(hasAmountOnlyBenefit)) {
            return CATEGORY_REQUIRED;
        }
        return decide(today, productUpdatedAt, true);
    }

    // 카테고리 기반 카드 추천에서 입력 카테고리에 맞는 계산 혜택 여부까지 포함해 추천 가능 상태를 판단합니다.
    public static CardProductRecommendStatus decideForCategory(
            LocalDate today,
            LocalDateTime productUpdatedAt,
            Boolean hasBenefitData,
            Boolean hasCategoryBenefit
    ) {
        if (!Boolean.TRUE.equals(hasBenefitData)) {
            return NO_BENEFIT_DATA;
        }
        if (!Boolean.TRUE.equals(hasCategoryBenefit)) {
            return CATEGORY_NOT_MATCHED;
        }
        return decide(today, productUpdatedAt, true);
    }

    // 추천 계산에 포함할 수 있는 상태인지 반환합니다.
    public Boolean isRecommendable() {
        return this == RECOMMENDABLE;
    }

    // API 상태 코드를 반환합니다.
    public String getCode() {
        return code;
    }

    // 추천 상태 안내 문구를 반환합니다.
    public String getNotice() {
        return notice;
    }
}

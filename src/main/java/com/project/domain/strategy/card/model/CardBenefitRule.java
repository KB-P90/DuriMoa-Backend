package com.project.domain.strategy.card.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBenefitRule {

    // 혜택 영역 또는 혜택 이름입니다.
    private final String title;

    // 화면 상세에 표시할 혜택 설명입니다.
    private final String description;

    // 결제 금액에 곱하는 혜택률입니다. 예: 1.2%는 0.012입니다.
    private final BigDecimal benefitRate;

    // 비율 대신 고정으로 적용되는 혜택 금액입니다.
    private final Long fixedBenefitAmount;

    // 한 달 동안 받을 수 있는 해당 혜택의 최대 금액입니다.
    private final Long monthlyLimitAmount;

    // 혜택 적용에 필요한 인정 실적 기준 금액입니다.
    private final Long minimumPerformanceAmount;

    // 혜택이 적용되는 소비 카테고리입니다. ALL은 사용처 미입력 상태에서도 계산 가능한 혜택입니다.
    private final List<String> categories;

    // 다음 혜택 구간 계산에 사용하는 실적 기준 금액 목록입니다.
    private final List<Long> benefitThresholds;

    // 제외 업종이나 제외 조건 안내 키워드입니다.
    private final List<String> excludedKeywords;

    // 추천 계산에서 해당 혜택을 제외할지 여부입니다.
    private final Boolean excluded;

    // 계산에 사용할 수 있는 혜택 값이 있는지 확인합니다.
    public Boolean hasBenefitValue() {
        return benefitRate != null || fixedBenefitAmount != null;
    }

    // 비어 있을 수 있는 혜택 구간 목록을 안전하게 반환합니다.
    public List<Long> safeBenefitThresholds() {
        if (benefitThresholds == null) {
            return Collections.emptyList();
        }
        return benefitThresholds;
    }

    // 비어 있을 수 있는 적용 카테고리 목록을 안전하게 반환합니다.
    public List<String> safeCategories() {
        if (categories == null) {
            return Collections.emptyList();
        }
        return categories;
    }

    // 비어 있을 수 있는 제외 조건 목록을 안전하게 반환합니다.
    public List<String> safeExcludedKeywords() {
        if (excludedKeywords == null) {
            return Collections.emptyList();
        }
        return excludedKeywords;
    }
}

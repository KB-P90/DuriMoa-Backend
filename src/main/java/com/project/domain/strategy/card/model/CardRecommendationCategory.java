package com.project.domain.strategy.card.model;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum CardRecommendationCategory {
    FURNITURE(
            "FURNITURE",
            "가구",
            List.of(
                    "가구", "FURNITURE", "FURNISHING", "HOME", "LIVING", "MART", "ELECTRONICS", "APPLIANCE"
            ),
            List.of(
                    "가구", "혼수", "가전", "전자", "전자제품", "대형마트", "마트", "이마트", "홈플러스",
                    "롯데마트", "코스트코", "트레이더스", "하나로마트", "하이마트", "전자랜드", "LG전자",
                    "삼성스토어", "오늘의집", "이케아", "IKEA", "생활용품", "생활잡화", "인테리어"
            )
    ),
    TRAVEL(
            "TRAVEL",
            "여행",
            List.of(
                    "여행", "TRAVEL", "AIR", "AIRLINE", "HOTEL", "ACCOMMODATION", "OVERSEAS",
                    "MILEAGE", "DUTY_FREE"
            ),
            List.of(
                    "여행", "항공", "호텔", "숙박", "리조트", "펜션", "야놀자", "여기어때", "대한항공",
                    "아시아나", "제주항공", "진에어", "티웨이", "에어부산", "에어서울", "여행사",
                    "관광", "면세점", "공항", "렌터카", "해외", "국외", "마일리지", "마일"
            )
    );

    private final String code;
    private final String displayName;
    private final List<String> aliases;
    private final List<String> benefitKeywords;

    CardRecommendationCategory(String code, String displayName, List<String> aliases, List<String> benefitKeywords) {
        this.code = code;
        this.displayName = displayName;
        this.aliases = aliases;
        this.benefitKeywords = benefitKeywords;
    }

    // 화면 입력값을 추천 카테고리로 변환합니다. 빈 값은 기존 금액 기반 추천을 유지하기 위해 null로 둡니다.
    public static CardRecommendationCategory from(String categoryText) {
        if (categoryText == null || categoryText.trim().isEmpty()) {
            return null;
        }

        String normalizedCategory = normalize(categoryText);
        return Arrays.stream(values())
                .filter(category -> category.matchesInput(normalizedCategory))
                .findFirst()
                .orElseThrow(() -> new CustomException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "카드 추천 카테고리는 가구 또는 여행만 입력할 수 있습니다."
                ));
    }

    // 혜택 규칙의 카테고리 코드, 제목, 설명이 현재 추천 카테고리에 해당하는지 확인합니다.
    public Boolean matchesBenefitScope(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String normalizedText = normalize(text);
        if (normalize(code).equals(normalizedText) || normalize(displayName).equals(normalizedText)) {
            return true;
        }
        boolean aliasMatched = aliases.stream()
                .map(CardRecommendationCategory::normalize)
                .anyMatch(alias -> alias.equals(normalizedText));
        if (aliasMatched) {
            return true;
        }
        return benefitKeywords.stream()
                .map(CardRecommendationCategory::normalize)
                .anyMatch(keyword -> normalizedText.contains(keyword));
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    private Boolean matchesInput(String normalizedCategory) {
        if (normalize(code).equals(normalizedCategory) || normalize(displayName).equals(normalizedCategory)) {
            return true;
        }
        return aliases.stream()
                .map(CardRecommendationCategory::normalize)
                .anyMatch(alias -> alias.equals(normalizedCategory));
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\s_\\-·/]", "")
                .toUpperCase(Locale.ROOT);
    }
}

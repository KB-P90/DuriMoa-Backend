package com.project.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ExpenseCategory {
    FOOD("식비", false),
    TRANSPORT("교통", false),
    SHOPPING("쇼핑", false),
    CAFE("카페/간식", false),
    DELIVERY("배달/외식", false),
    SUBSCRIPTION("구독 서비스", false),
    CULTURE("문화/여가", false),
    BEAUTY("미용/패션", false),

    WEDDINGHALL("예식장", true),
    STUDIO("스튜디오", true),
    MAKEUP("메이크업", true),
    DRESS("드레스", true),
    JEWELRY("예물", true),
    EMERGENCY("예비비", true),

    ETC("기타", false);

    private final String categoryName;
    private final boolean wedding;

    ExpenseCategory(String categoryName, boolean wedding) {
        this.categoryName = categoryName;
        this.wedding = wedding;
    }

    public boolean isWedding() {
        return wedding;
    }

    // 결혼 관련 카테고리 리스트
    public static List<String> getWeddingCodes() {
        return Arrays.stream(values())
                .filter(ExpenseCategory::isWedding)
                .map(Enum::name)
                .toList();
    }

    // 한글 => 영문 변환
    public static ExpenseCategory fromKorean(String categoryName) {
        return Arrays.stream(values())
                .filter(c -> c.getCategoryName().equals(categoryName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("잘못된 지출 카테고리입니다."));
    }
}

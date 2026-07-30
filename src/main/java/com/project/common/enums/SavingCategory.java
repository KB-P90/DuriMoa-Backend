package com.project.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum SavingCategory {

    GENERALSAVING("저축", false),

    WEDDINGSAVING("결혼 저축", true);

    private final String categoryName;
    private final boolean wedding;

    SavingCategory(String categoryName, boolean wedding) {
        this.categoryName = categoryName;
        this.wedding = wedding;
    }

    public boolean isWedding() {
        return wedding;
    }

    // 결혼 관련 카테고리 리스트
    public static List<String> getWeddingCodes() {
        return Arrays.stream(values())
                .filter(SavingCategory::isWedding)
                .map(Enum::name)
                .toList();
    }

    // 한글 => 영문 변환
    public static SavingCategory fromKorean(String categoryName) {
        return Arrays.stream(values())
                .filter(c -> c.getCategoryName().equals(categoryName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("잘못된 저축 카테고리입니다."));
    }
}

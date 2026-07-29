package com.project.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum SavingCategory {

    GENERAL("저축", false),

    WEDDING("결혼 저축", true);

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
}

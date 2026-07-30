package com.project.common.enums;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum IncomeCategory {

    SALARY("급여", false),
    SIDEINCOME("부수입", false),

    MONEYGIFT("축의금", true),

    ETC("기타", false);

    private final String categoryName;
    private final boolean wedding;

    IncomeCategory(String categoryName, boolean wedding) {
        this.categoryName = categoryName;
        this.wedding = wedding;
    }

    public boolean isWedding() {
        return wedding;
    }

    // 결혼 관련 카테고리 리스트
    public static List<String> getWeddingCodes() {
        return Arrays.stream(values())
                .filter(IncomeCategory::isWedding)
                .map(Enum::name)
                .toList();
    }

    // 한글 => 영문 변환
    public static IncomeCategory fromKorean(String categoryName) {
        return Arrays.stream(values())
                .filter(c -> c.getCategoryName().equals(categoryName))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE, "존재하지 않는 카테고리입니다."));
    }
}

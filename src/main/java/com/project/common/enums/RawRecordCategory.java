package com.project.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RawRecordCategory {

    WEDDING("결혼비용"),
    PERSONAL("개인소비");

    private final String categoryName;

    public Boolean matches(String category) {
        return categoryName.equals(category);
    }
}

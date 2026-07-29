package com.project.domain.calendar.domain;

import java.util.Arrays;
import java.util.List;

public enum WeddingCategory {
    HALL("예식장"),
    STUDIO("스튜디오"),
    MAKEUP("메이크업"),
    DRESS("드레스"),
    JEWELRY("예물"),
    EMERGENCY("예비비");

    private final String displayName;

    WeddingCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static List<String> getCodes() {
        return Arrays.stream(values())
                .map(Enum::name)
                .toList();
    }
}

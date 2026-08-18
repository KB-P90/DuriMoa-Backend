package com.project.domain.ai.domain;

import lombok.Getter;

@Getter
public enum AiBudgetType {
    SAVING("saving"),
    BALANCED("balanced"),
    FLEX("flex");

    private final String code;

    AiBudgetType(String code) {
        this.code = code;
    }
}

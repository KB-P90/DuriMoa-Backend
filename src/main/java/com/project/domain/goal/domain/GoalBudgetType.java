package com.project.domain.goal.domain;

import java.util.Arrays;

public enum GoalBudgetType {

    ECONOMY("알뜰형"),
    BALANCED("균형형"),
    FLEX("플렉스형");

    private final String value;

    GoalBudgetType(String value) {
        this.value = value;
    }

    public static boolean supports(String value) {
        return Arrays.stream(values())
                .anyMatch(type -> type.value.equals(value));
    }
}

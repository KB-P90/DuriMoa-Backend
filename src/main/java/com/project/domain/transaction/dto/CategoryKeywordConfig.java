package com.project.domain.transaction.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class CategoryKeywordConfig {
    private List<CategoryRuleConfig> expense = new ArrayList<>();
    private List<CategoryRuleConfig> income = new ArrayList<>();

    @Getter
    @NoArgsConstructor
    public static class CategoryRuleConfig {
        private String category;
        private int priority;
        private List<String> keywords = new ArrayList<>();
    }
}

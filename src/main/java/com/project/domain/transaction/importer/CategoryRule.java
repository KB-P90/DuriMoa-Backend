package com.project.domain.transaction.importer;

import lombok.Getter;

import java.util.List;

@Getter
public class CategoryRule<T> {

    private final T category;
    private final int priority;
    private final List<String> keywords;

    public CategoryRule(T category, int priority, List<String> keywords) {
        this.category = category;
        this.priority = priority;
        this.keywords = keywords;
    }
}
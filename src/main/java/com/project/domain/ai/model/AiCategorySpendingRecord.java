package com.project.domain.ai.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiCategorySpendingRecord {

    private String category;
    private Long amount;
    private Integer transactionCount;
}

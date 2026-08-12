package com.project.domain.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawCategoryExpenseDTO {
    private String categoryCode;
    private Long amount;
}

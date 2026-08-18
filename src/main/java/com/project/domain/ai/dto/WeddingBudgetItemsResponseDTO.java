package com.project.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeddingBudgetItemsResponseDTO {

    private Long venue;
    private Long studio;
    private Long dress;
    private Long makeup;
    private Long reserve;
}

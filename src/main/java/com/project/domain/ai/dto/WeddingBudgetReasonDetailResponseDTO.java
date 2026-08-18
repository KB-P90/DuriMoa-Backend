package com.project.domain.ai.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "AI 결혼 예산 추천 상세 근거")
public class WeddingBudgetReasonDetailResponseDTO {

    private String title;
    private String description;
}

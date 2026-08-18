package com.project.domain.ai.dto;

import com.project.domain.ai.domain.AiAnalysisScope;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "AI 결혼 예산 추천 응답")
public class WeddingBudgetRecommendationResponseDTO {

    private String name;
    private AiAnalysisScope analysisScope;
    private Integer analyzedMemberCount;
    private Integer analysisMonths;
    private Boolean dataSufficient;
    private Long sustainableMonthlySaving;
    private Long affordableTotalBudget;
    private String budgetType;
    private WeddingBudgetItemsResponseDTO items;
    private Long totalBudget;
    private Long groomRatio;
    private Long brideRatio;
    private String reason;
    private List<WeddingBudgetReasonDetailResponseDTO> reasonDetails;

    @ApiModelProperty(value = "데이터 부족 또는 지역 통계 미달 안내", allowEmptyValue = true)
    private String warning;
}

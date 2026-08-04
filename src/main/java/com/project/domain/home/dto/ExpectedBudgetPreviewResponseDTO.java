package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpectedBudgetPreviewResponseDTO {

    // 홈 미리보기에서 보여주는 예산 유형입니다.
    @JsonProperty("budget_type")
    private final String budgetType;

    // 금액 산정에 사용한 통계 기준입니다.
    @JsonProperty("amount_basis")
    private final String amountBasis;

    // 예산 미리보기에 합산한 결혼 준비 카테고리입니다.
    @JsonProperty("categories")
    private final List<String> categories;

    // 막대그래프에 표시할 지역별 예상 금액 목록입니다.
    @JsonProperty("regions")
    private final List<ExpectedBudgetRegionResponseDTO> regions;
}

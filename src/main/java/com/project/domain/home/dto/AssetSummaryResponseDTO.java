package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetSummaryResponseDTO {

    // 총 결혼 예산은 목표·예산 도메인의 확정 값을 연결합니다.
    @JsonProperty("total_budget_amount")
    private final Long totalBudgetAmount;

    // 누적 결혼 자금은 저축·자산 도메인의 확정 값을 연결합니다.
    @JsonProperty("total_saved_amount")
    private final Long totalSavedAmount;

    // 결혼 준비 지출은 지출 도메인의 확정 값을 연결합니다.
    @JsonProperty("wedding_expense_amount")
    private final Long weddingExpenseAmount;

    // 잔여 결혼 자금은 자산 요약 도메인의 확정 값을 연결합니다.
    @JsonProperty("remaining_wedding_fund_amount")
    private final Long remainingWeddingFundAmount;

    // 요약 금액의 기준일은 실제 제공자 연결 후 설정합니다.
    @JsonProperty("base_date")
    private final String baseDate;
}

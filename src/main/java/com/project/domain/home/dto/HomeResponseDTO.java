package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeResponseDTO {

    // 체크리스트 5단계가 모두 완료되어 메인 홈을 보여줄 수 있는지 나타냅니다.
    @JsonProperty("setup_completed")
    private final Boolean setupCompleted;

    // 홈 상단의 커플 연결과 결혼 목표 연결 정보를 담는 영역입니다.
    @JsonProperty("couple")
    private final CoupleResponseDTO couple;

    // 결혼 자금과 지출 요약을 나중에 연결하기 위한 응답 영역입니다.
    @JsonProperty("asset_summary")
    private final AssetSummaryResponseDTO assetSummary;

    // 저축 부족 또는 목표 변경 알림을 나중에 연결하기 위한 응답 영역입니다.
    @JsonProperty("saving_alert")
    private final SavingAlertResponseDTO savingAlert;

    // 홈 상단 알림 버튼의 새 알림 상태입니다.
    @JsonProperty("notification_summary")
    private final NotificationSummaryResponseDTO notificationSummary;

    // 절약 미션 요약과 대표 미션을 나중에 연결하기 위한 응답 영역입니다.
    @JsonProperty("saving_mission_summary")
    private final SavingMissionSummaryResponseDTO savingMissionSummary;

    // 공동 목표와 개인 목표 달성률을 나중에 연결하기 위한 응답 영역입니다.
    @JsonProperty("goal_summary")
    private final GoalSummaryResponseDTO goalSummary;

    // 준비 전 홈 화면의 5단계 진행 상태입니다.
    @JsonProperty("setup_checklist")
    private final SetupChecklistResponseDTO setupChecklist;

    // 체크리스트 미완료 홈에서 보여줄 지역별 예상 예산 미리보기입니다.
    @JsonProperty("expected_budget_preview")
    private final ExpectedBudgetPreviewResponseDTO expectedBudgetPreview;
}

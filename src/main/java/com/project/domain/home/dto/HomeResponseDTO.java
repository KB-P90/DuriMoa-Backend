package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeResponseDTO {

    // 홈 상단의 커플 연결과 결혼 목표 연결 정보를 담는 영역입니다.
    @JsonProperty("couple")
    private final CoupleResponseDTO couple;

    // 결혼 자금과 지출 요약을 나중에 연결하기 위한 응답 영역입니다.
    @JsonProperty("asset_summary")
    private final AssetSummaryResponseDTO assetSummary;

    // 저축 부족 또는 목표 변경 알림을 나중에 연결하기 위한 응답 영역입니다.
    @JsonProperty("saving_alert")
    private final SavingAlertResponseDTO savingAlert;

    // 절약 미션 요약과 대표 미션을 나중에 연결하기 위한 응답 영역입니다.
    @JsonProperty("saving_mission_summary")
    private final SavingMissionSummaryResponseDTO savingMissionSummary;

    // 공동 목표와 개인 목표 달성률을 나중에 연결하기 위한 응답 영역입니다.
    @JsonProperty("goal_summary")
    private final GoalSummaryResponseDTO goalSummary;

    // 준비 전 홈 화면의 5단계 진행 상태입니다.
    @JsonProperty("setup_checklist")
    private final SetupChecklistResponseDTO setupChecklist;

    @Getter
    @Builder
    public static class CoupleResponseDTO {

        // 인증 사용자가 속한 활성 커플 ID입니다.
        @JsonProperty("couple_id")
        private final Long coupleId;

        // 인증 사용자 ID입니다.
        @JsonProperty("user_id")
        private final Long userId;

        // 인증 사용자의 화면 표시 이름입니다.
        @JsonProperty("user_name")
        private final String userName;

        // 인증 사용자의 신랑·신부 역할입니다.
        @JsonProperty("user_role")
        private final String userRole;

        // 활성 커플의 상대방 사용자 ID입니다.
        @JsonProperty("partner_user_id")
        private final Long partnerUserId;

        // 활성 커플의 상대방 화면 표시 이름입니다.
        @JsonProperty("partner_name")
        private final String partnerName;

        // 활성 커플의 상대방 신랑·신부 역할입니다.
        @JsonProperty("partner_role")
        private final String partnerRole;

        // 커플 연결 상태 원본 값입니다.
        @JsonProperty("couple_status")
        private final String coupleStatus;

        // 홈에서 연결할 대표 결혼 목표 ID입니다.
        @JsonProperty("goal_id")
        private final Long goalId;

        // 결혼 예정일을 화면에 표시할 때 사용할 날짜입니다.
        @JsonProperty("wedding_date")
        private final String weddingDate;

        // 결혼 예정일까지 남은 개월 수는 목표 도메인 계산 결과로 연결합니다.
        @JsonProperty("remaining_months")
        private final Integer remainingMonths;

        // 예산 시안 식별자는 목표·예산 도메인 계약 확정 후 연결합니다.
        @JsonProperty("budget_plan_id")
        private final Long budgetPlanId;
    }

    @Getter
    @Builder
    public static class AssetSummaryResponseDTO {

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

    @Getter
    @Builder
    public static class SavingAlertResponseDTO {

        // 표시할 홈 알림 유형은 알림 도메인 연결 후 설정합니다.
        @JsonProperty("alert_type")
        private final String alertType;

        // 홈 알림 제목은 실제 알림 조회 결과를 연결합니다.
        @JsonProperty("title")
        private final String title;

        // 홈 알림 내용은 실제 알림 조회 결과를 연결합니다.
        @JsonProperty("message")
        private final String message;

        // 알림 생성 시점은 알림 도메인 계약 확정 후 연결합니다.
        @JsonProperty("created_at")
        private final String createdAt;
    }

    @Getter
    @Builder
    public static class SavingMissionSummaryResponseDTO {

        // 절약 예상 금액 합계는 미션 도메인의 확정 값을 연결합니다.
        @JsonProperty("total_expected_saving_amount")
        private final Long totalExpectedSavingAmount;

        // 진행 중 미션 수는 미션 도메인의 확정 값을 연결합니다.
        @JsonProperty("in_progress_mission_count")
        private final Integer inProgressMissionCount;

        // 완료 미션 수는 미션 도메인의 확정 값을 연결합니다.
        @JsonProperty("completed_mission_count")
        private final Integer completedMissionCount;

        // 홈에 노출할 대표 절약 미션입니다.
        @JsonProperty("mission")
        private final MissionResponseDTO mission;

        // 홈 절약미션 영역을 눌렀을 때 이동할 화면 안내 문구입니다.
        @JsonProperty("navigation_message")
        private final String navigationMessage;
    }

    @Getter
    @Builder
    public static class MissionResponseDTO {

        // 대표 미션 식별자입니다.
        @JsonProperty("mission_id")
        private final Long missionId;

        // 대표 미션 카테고리 코드입니다.
        @JsonProperty("category_code")
        private final String categoryCode;

        // 대표 미션 제목입니다.
        @JsonProperty("title")
        private final String title;

        // 대표 미션 상태입니다.
        @JsonProperty("status")
        private final String status;

        // 대표 미션의 예상 절약 금액입니다.
        @JsonProperty("expected_saving_amount")
        private final Long expectedSavingAmount;
    }

    @Getter
    @Builder
    public static class GoalSummaryResponseDTO {

        // 커플 공동 목표 진행률 영역입니다.
        @JsonProperty("joint_goal")
        private final GoalProgressResponseDTO jointGoal;

        // 로그인 사용자 개인 월 저축 목표 진행률 영역입니다.
        @JsonProperty("personal_goal")
        private final GoalProgressResponseDTO personalGoal;
    }

    @Getter
    @Builder
    public static class GoalProgressResponseDTO {

        // 연결된 목표 식별자입니다.
        @JsonProperty("goal_id")
        private final Long goalId;

        // 목표 진행 상태 원본 값입니다.
        @JsonProperty("status")
        private final String status;

        // 목표 금액은 목표 도메인의 확정 값을 연결합니다.
        @JsonProperty("target_amount")
        private final Long targetAmount;

        // 현재 금액은 자산·저축 도메인의 확정 값을 연결합니다.
        @JsonProperty("current_amount")
        private final Long currentAmount;

        // 달성률은 확정된 반올림 정책이 생긴 뒤 연결합니다.
        @JsonProperty("achievement_rate")
        private final Integer achievementRate;

        // 목표 설정 안내가 필요한지 판단하는 값입니다.
        @JsonProperty("setup_required")
        private final Boolean setupRequired;
    }

    @Getter
    @Builder
    public static class SetupChecklistResponseDTO {

        // 홈 준비 체크리스트의 전체 단계 수입니다.
        @JsonProperty("total_count")
        private final Integer totalCount;

        // 앞 단계부터 연속해서 완료된 단계 수입니다.
        @JsonProperty("completed_count")
        private final Integer completedCount;

        // 처음으로 완료되지 않은 단계 번호입니다.
        @JsonProperty("current_step")
        private final Integer currentStep;

        // 단계별 완료 여부와 화면 상태 목록입니다.
        @JsonProperty("steps")
        private final List<SetupChecklistStepResponseDTO> steps;
    }

    @Getter
    @Builder
    public static class SetupChecklistStepResponseDTO {

        // 체크리스트에서 보여줄 단계 번호입니다.
        @JsonProperty("step")
        private final Integer step;

        // 프론트가 단계 의미를 구분할 때 사용할 고정 코드입니다.
        @JsonProperty("code")
        private final String code;

        // 화면에 표시할 단계 제목입니다.
        @JsonProperty("title")
        private final String title;

        // 앞 단계 조건까지 만족해 실제 완료로 인정된 여부입니다.
        @JsonProperty("completed")
        private final Boolean completed;

        // COMPLETED, CURRENT, LOCKED 중 하나인 화면 상태입니다.
        @JsonProperty("status")
        private final String status;
    }
}

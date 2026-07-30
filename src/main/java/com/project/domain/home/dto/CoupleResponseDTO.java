package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CoupleResponseDTO {

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

package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalProgressResponseDTO {

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

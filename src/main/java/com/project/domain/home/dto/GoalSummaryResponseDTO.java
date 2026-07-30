package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalSummaryResponseDTO {

    // 커플 공동 목표 진행률 영역입니다.
    @JsonProperty("joint_goal")
    private final GoalProgressResponseDTO jointGoal;

    // 로그인 사용자 개인 월 저축 목표 진행률 영역입니다.
    @JsonProperty("personal_goal")
    private final GoalProgressResponseDTO personalGoal;
}

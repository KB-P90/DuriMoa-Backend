package com.project.domain.home.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeSetupChecklistEvidence {

    // 인증 사용자에게 실제 연결 계좌 또는 카드가 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean accountLinked;

    // 인증 사용자가 활성 커플에 속해 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean coupleConnected;

    // 활성 커플에 결혼 목표 시안이 하나 이상 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean weddingGoalCreated;

    // 활성 커플에 확정된 메인 시안이 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean mainPlanSelected;

    // 첫걸음 시작 단계가 완료되었는지 나타내는 원시 완료 근거입니다.
    private final Boolean firstStepStarted;

    // 모든 단계가 미완료인 기본 근거를 만듭니다.
    public static HomeSetupChecklistEvidence empty() {
        return HomeSetupChecklistEvidence.builder()
                .accountLinked(false)
                .coupleConnected(false)
                .weddingGoalCreated(false)
                .mainPlanSelected(false)
                .firstStepStarted(false)
                .build();
    }
}

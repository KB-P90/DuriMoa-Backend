package com.project.domain.home.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeSetupChecklistEvidence {

    // 인증 사용자가 계좌·카드 연결과 결혼자금 입력을 완료했는지 나타내는 원시 완료 근거입니다.
    private final Boolean accountLinked;

    // 인증 사용자가 활성 커플에 속해 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean coupleConnected;

    // 활성 커플에 결혼 목표 시안이 하나 이상 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean weddingGoalCreated;

    // 모든 단계가 미완료인 기본 근거를 만듭니다.
    public static HomeSetupChecklistEvidence empty() {
        return HomeSetupChecklistEvidence.builder()
                .accountLinked(false)
                .coupleConnected(false)
                .weddingGoalCreated(false)
                .build();
    }
}

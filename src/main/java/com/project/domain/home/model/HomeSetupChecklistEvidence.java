package com.project.domain.home.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeSetupChecklistEvidence {

    // 인증 사용자가 활성 커플에 속해 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean coupleConnected;

    // 인증 사용자에게 실제 연결 계좌 또는 카드가 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean accountLinked;

    // 활성 커플에 대표 결혼 목표가 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean weddingGoalCreated;

    // 대표 결혼 목표에 지역 값이 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean regionSelected;

    // 활성 커플 구성원에게 실제 양수 저축 기록이 있는지 나타내는 원시 완료 근거입니다.
    private final Boolean firstSavingRecorded;

    // 모든 단계가 미완료인 기본 근거를 만듭니다.
    public static HomeSetupChecklistEvidence empty() {
        return HomeSetupChecklistEvidence.builder()
                .coupleConnected(false)
                .accountLinked(false)
                .weddingGoalCreated(false)
                .regionSelected(false)
                .firstSavingRecorded(false)
                .build();
    }
}

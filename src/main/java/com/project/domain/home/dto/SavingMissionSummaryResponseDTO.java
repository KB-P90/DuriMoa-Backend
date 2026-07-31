package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavingMissionSummaryResponseDTO {

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

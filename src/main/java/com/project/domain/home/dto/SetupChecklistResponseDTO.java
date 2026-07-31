package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SetupChecklistResponseDTO {

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
